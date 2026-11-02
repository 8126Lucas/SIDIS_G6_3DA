# Maintenance Service — Domain and Resilience (As Implemented)

## 1. Domain Model

The service has two entities: **MaintenanceTemplate** (what kind of maintenance) and **MaintenanceRecord** (one maintenance activity on one aircraft). Aircraft are not stored locally; records keep only the aircraft registration.

### 1.1. `MaintenanceTemplate` (table `maintenance_template`)

* `templateId`: generated identifier (`IDENTITY`).
* `name`: free text, not unique.
* `expectedDuration`: required.
* `templateChecklist`: element collection in table `maintenance_template_checklist` (`check_item` → `is_required`).
* `operation`: enum `MaintenanceType` (`INSPECTION`, `SCHEDULED`, `OVERHAUL`, `MODIFICATION`).
* `attribute`: enum `MaintenanceAttribute` (`ENGINE`, `AIRFRAME`, `AVIONICS`, `INTERIOR`, `EXTERIOR`).

Behavior: `changeAttribute(...)` replaces the attribute (no null check). There is no `@Version`.

### 1.2. `MaintenanceRecord` (table `company_maintenance_records`)

* `recordId`: generated identifier (`IDENTITY`).
* `version`: optimistic locking field (`@Version`).
* `aircraftRegistration` (column `aircraft_id`): registration of an aircraft in `aircraft-service`.
* `maintenanceTemplate`: `@ManyToOne` to `MaintenanceTemplate` (column `maintenance_template_id`).
* `description` (required), `startDate` (required), `endDate`, `durationHours` (required).
* `techniciansUsername`: list of usernames; `supervisorUsername` (column `supervisor_id`).
* `usedParts`: element collection of embedded `UsedPart` in table `maintenance_used_parts`: `partSerialNumber`, `quantity`, `price` (all required).
* `doneList`: embedded `DoneList`: `completionNotes` and `tasksDone` (table `done_list_tasks`, `task` → `completed`).
* `status`: enum `MaintenanceStatus` (`INLINE`, `ONGOING`, `COMPLETED`).

Aggregate behavior in `MaintenanceRecord`:

* `updateDuration(...)`: must be positive.
* `assignTechnician(...)`: not `null`, no duplicates.
* `addUsedPart(...)`: not `null`.
* `markAsOngoing()`: sets `ONGOING`, no guard.
* `markAsCompleted(notes, tasksDone, endDate)`: rejects an already completed record (`IllegalStateException`), a `null` end date, or an end date before the start date (`IllegalArgumentException`).
* `markAsCompleted(doneList, endDate)`: second overload with no checks; used only by `Bootstrap`.

Usernames are plain strings: there is no link to `auth-service` and no check that the users exist.

## 2. Lifecycle Rules

New records start as `INLINE`.

| From | To | How |
| :--- | :--- | :--- |
| any | `ONGOING` | `PATCH /records/{id}/status` with `ONGOING` |
| `INLINE` / `ONGOING` | `COMPLETED` | `PATCH /records/{id}/complete` (`completeRecord(...)`), with notes, done tasks and end date |
| `COMPLETED` | `COMPLETED` | Rejected (`IllegalStateException`, `409`) |
| any | `INLINE` | Rejected (`DomainException`, reported as `404`) |

`updateProgress(...)` (duration and used parts) exists in `MaintenanceRecordProgressService` but is not called by any controller. As a result, used parts, technicians and the supervisor can only be set by `Bootstrap`.

## 3. Use Cases

| Use Case / Service | Responsibility | User Story |
| :--- | :--- | :--- |
| `AddMaintenanceTemplateUseCase` (`@UseCase`) | Persist a new template | US115 |
| `AddMaintenanceRecordUseCase` (`@UseCase`) | Check aircraft in `aircraft-service`, persist a new record | US116 |
| `MaintenanceTemplateSearchService` | Template lookup and listing | US117 |
| `MaintenanceRecordProgressService` (`@UseCase`) | Record lookup, `ONGOING` and `COMPLETED` transitions, attribute change | US119, US217 |
| `MaintenanceRecordSearchService` | `Specification`-based record search | US218 |
| `MaintenanceReportService` (`@UseCase`) | Ongoing list, costs, turnaround average | US219, US220, US221 |
| `MaintenanceAlertService` (`@UseCase`) | Alerts by days since last maintenance and flight hours | US222 |
| `PartsInventoryService` (`@UseCase`) | Used parts summary and low-stock alerts | US226 |

Classes annotated with `@UseCase` are `@Service`, `@Validated` and `@Transactional`. `UseCaseLoggingAdvice` logs their calls (method, arguments, execution time) through the pointcut `@within(com.sidis.maintenanceservice.UseCase)`; `@SuppressArgLogging` would hide the arguments but is not used on any class.

## 4. External Integrations

Synchronous `RestClient` integration with `aircraft-service` (Eureka load-balanced, shared `RestClient.Builder` from `GlobalRestClientConfig` with `JwtTokenInterceptor`):

| Client Method | Downstream Call | Used By |
| :--- | :--- | :--- |
| `findAircraftByRegistration(...)` | `GET /api/hangar/aircraft/{registrationNumber}` | Record creation (twice), record search |
| `findAircraftModelNameByRegistrationNumber(...)` | same as above, returns `modelName` or `""` | Costs by model, turnaround average |
| `findAll()` | `GET /api/hangar/aircraft?size=1000` (JSON array) | Alerts |

`JwtTokenInterceptor` adds `X-Gateway-Secret: sidis-g6-secret` to every outgoing call and copies the incoming `Authorization: Bearer` header, so calls pass `aircraft-service`'s gateway secret filter.

`AircraftClientDTO` maps `registrationNumber` (as `aircraftRegistration` through `@JsonProperty`), `modelName` and `totalFlightHours`, which matches `aircraft-service`'s response.

Error handling in the client: `HttpClientErrorException.NotFound` (and, in `findAll()`, any `HttpClientErrorException`) is caught and turned into an empty result; other errors are retried (section 6) and then propagate as `500`.

Inbound consumers of this service:

* None in the current code; only the API Gateway routes to it.

## 5. Analytics

* **Costs:** `sum(quantity × price)` of used parts per record, grouped by registration or model name.
* **Turnaround:** `days(startDate → endDate)` of completed records, averaged per model name.
* **Alerts:** last completed record per aircraft compared with `calendarDaysThreshold` (default `180`), and `totalFlightHours` compared with `flightHoursThreshold` (default `500`).
* **Parts inventory:** used parts grouped by serial number (total quantity, total cost, number of entries).
* **Low stock:** parts with `totalQuantityUsed >= threshold` (default `10`); there is no stock level, only usage.

## 6. Resilience and Concurrency

Optimistic locking:

* Implemented with `@Version` on `MaintenanceRecord.version`.
* `MaintenanceTemplate` has no version field; concurrent attribute changes are last-write-wins.

Exception handling:

* Centralized by `GlobalExceptionHandler`.
* `DomainException` and `ResourceNotFoundException` map to `404 Not Found`.
* `IllegalStateException` and `ObjectOptimisticLockingFailureException` map to `409 Conflict`.
* Bean validation, malformed JSON, and `IllegalArgumentException` map to `400 Bad Request`.
* Unhandled exceptions map to `500 Internal Server Error`.

Outgoing calls:

* `GlobalRestClientConfig` sets a 2-second connect timeout and a 2-second read timeout on every `RestClient`.
* `AircraftRestClientService` methods are `@Retryable` (any `Exception`, 3 attempts, backoff 1 s then 2 s); `@EnableRetry` is on `MaintenanceServiceApplication`. Exceptions caught inside a method (`404`, or any `4xx` in `findAll()`) are not retried. There is no `@Recover` method, so after the third failure the error becomes `500`.

Peer forwarding (`PeerForwarder`):

* Used only by `GET /templates/{id}` when the template is not found locally.
* Asks Eureka for the instances of the given service id, skips the one with the same port as the current instance, and calls the same path on each (with `X-Gateway-Secret` and the caller's `Authorization`); any error is ignored. If none answers, it throws `ResourceNotFoundException` (`404`).
* The controller passes `aircraft-service` as the service id, so it asks `aircraft-service` for `/api/maintenance/templates/{id}`, which that service does not have.
* The `RestClient` comes from the `@LoadBalanced` builder, which treats the peer's host name or IP as a service id, so the forwarded call fails even with the right service id.

Current gaps:

* `DomainException` is used for both missing resources and rule violations, so rule violations are reported as `404` instead of `409`.
* No circuit breaker or fallback around `RestClient` calls.
* Costs by model and turnaround call `aircraft-service` once per record (no caching).

## 7. Security

`MaintenanceSecurityConfig` defines a stateless `SecurityFilterChain`:

* CSRF, form login and HTTP Basic are disabled; sessions are `STATELESS`.
* All requests are `permitAll()`; there is no authentication or role check inside the service.
* `gatewaySecretFilter` (`OncePerRequestFilter`, registered before `UsernamePasswordAuthenticationFilter`) requires `X-Gateway-Secret: sidis-g6-secret`. Otherwise it writes `403` with the plain-text body `Unauthorized Access! Please use the API Gateway!`.
* Paths containing `/h2-console`, `/swagger-ui` or `/api-docs` skip the check. The OpenAPI document is served at `/api/docs`, which does not match, so Swagger UI cannot load the spec.

Responsibilities split with the API Gateway:

* The gateway's `JwtAuthentication` filter validates the JWT signature (same secret as `auth-service`), returns `401` when it is missing or invalid, and adds `X-User-Name` and `X-Gateway-Secret` to the forwarded request.
* `maintenance-service` trusts the secret header only; it does not validate the JWT, does not use `X-User-Name`, and has no `jjwt` dependency.

Known weaknesses:

* The gateway secret is a hardcoded constant in `MaintenanceSecurityConfig`, `JwtTokenInterceptor` and the gateway filter, not a configuration property.
* Any client that knows the secret can call the service directly without a JWT.

## 8. Bootstrap

`Bootstrap` (`ApplicationRunner`) seeds data when there are no records:

| Template (id) | Operation / Attribute | Record | Aircraft | Status | Used Parts |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `ENGINE-INSPECTION` (1) | `INSPECTION` / `ENGINE` | 1 | `CS-TUA` | `COMPLETED` (start `today - 10`, end `today - 1`) | `ENG-001` ×2 @ 1500 |
| `LANDING-GEAR-CHECK` (2) | `INSPECTION` / `AIRFRAME` | 2 | `D-ABXA` | `COMPLETED` (start `today - 5`, end `today - 1`) | `TIRE-900` ×4 @ 800, `BRAKE-200` ×2 @ 1200 |
| `AVIONICS-DIAGNOSTIC` (3) | `INSPECTION` / `AVIONICS` | 3 | `F-HXKA` | `COMPLETED` (start `today - 2`, end `today - 1`) | none |
| `ENGINE-REPAIR` (4) | `OVERHAUL` / `ENGINE` | 4 | `CS-TUA` | `INLINE` ("In Progress", start `today`) | `FUEL-PUMP-01` ×1 @ 3200, `OIL-FILTER-22` ×2 @ 250 |

Notes on the seed:

* Dates are relative to the start-up day, and the registrations match aircraft seeded by `aircraft-service`.
* Record 4 is described as "In Progress" but is `INLINE`, so `GET /ongoing` starts empty.
