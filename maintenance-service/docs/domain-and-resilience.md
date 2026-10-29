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
| any | `COMPLETED` | Not reachable through the API (`completeRecord(...)` is not exposed) |
| any | `INLINE` | Rejected (`409`) |

`updateProgress(...)` (duration and used parts) and `completeRecord(...)` exist in `MaintenanceRecordProgressService` but are not called by any controller. As a result, used parts, technicians, the supervisor, the done list and the end date can only be set by `Bootstrap`.

## 3. Use Cases

| Use Case / Service | Responsibility | User Story |
| :--- | :--- | :--- |
| `AddMaintenanceTemplateUseCase` (`@UseCase`) | Persist a new template | US115 |
| `AddMaintenanceRecordUseCase` (`@UseCase`) | Check aircraft in `aircraft-service`, persist a new record | US116 |
| `MaintenanceTemplateSearchService` | Template lookup and listing | US117 |
| `MaintenanceRecordProgressService` (`@UseCase`) | Record lookup, `ONGOING` transition, attribute change | US119, US217 |
| `MaintenanceRecordSearchService` | `Specification`-based record search | US218 |
| `MaintenanceReportService` (`@UseCase`) | Ongoing list, costs, turnaround average | US219, US220, US221 |
| `MaintenanceAlertService` (`@UseCase`) | Alerts by days since last maintenance and flight hours | US222 |
| `PartsInventoryService` (`@UseCase`) | Used parts summary and low-stock alerts | US226 |

Classes annotated with `@UseCase` are `@Service`, `@Validated` and `@Transactional`. `UseCaseLoggingAdvice` is meant to log them (method, arguments, execution time, with `@SuppressArgLogging` to hide arguments), but its pointcut is `within(com.sidis.maintenanceservice.UseCase)`, which matches the annotation type itself instead of classes annotated with it, so no use case is logged. `@SuppressArgLogging` is not used on any class.

## 4. External Integrations

Synchronous `RestClient` integration with `aircraft-service` (Eureka load-balanced, shared `RestClient.Builder` from `GlobalRestClientConfig` with `JwtTokenInterceptor`):

| Client Method | Downstream Call | Used By |
| :--- | :--- | :--- |
| `findAircraftByRegistration(...)` | `GET /api/hangar/aircraft/{registrationNumber}` | Record creation (twice), record search |
| `findAircraftModelNameByRegistrationNumber(...)` | same as above, returns `modelName` or `""` | Costs by model, turnaround average |
| `findAll()` | `GET /api/hangar/aircraft` | Alerts |

`JwtTokenInterceptor` adds `X-Gateway-Secret: sidis-g6-secret` to every outgoing call and copies the incoming `Authorization: Bearer` header, so calls pass `aircraft-service`'s gateway secret filter.

Contract gaps in the current code:

* `AircraftClientDTO` expects `aircraftRegistration`, `modelName`, `totalFlightHours`, while `aircraft-service` returns `registrationNumber`, `modelName`, `totalFlightHours` (plus other fields). `aircraftRegistration` is always `null`, which breaks record creation (section 3.3 of `api-specification.md`) and alert matching.
* `findAll()` deserializes the JSON array from `GET /api/hangar/aircraft` into a single `AircraftClientDTO`; the error is not caught, so alerts always return `500`. That endpoint is also paged (default `10`).
* Only `HttpClientErrorException.NotFound` (and, in `findAll()`, `HttpClientErrorException`) is caught; other errors propagate as `500`.

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
* `DomainException`, `IllegalStateException` and `ObjectOptimisticLockingFailureException` map to `409 Conflict`.
* Bean validation, malformed JSON, and `IllegalArgumentException` map to `400 Bad Request`.
* `ResourceNotFoundException` maps to `404 Not Found` (used only in record creation).
* Unhandled exceptions map to `500 Internal Server Error`.

Current gaps:

* Missing templates and records use `DomainException` (`409`) instead of `ResourceNotFoundException` (`404`).
* No timeout, retry, or circuit breaker around `RestClient` calls; any downstream failure becomes `500`.
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
| `ENGINE-INSPECTION` (1) | `INSPECTION` / `ENGINE` | 1 | `CS-TUA` | `COMPLETED` (end `2003-03-01`) | `ENG-001` ×2 @ 1500 |
| `LANDING-GEAR-CHECK` (2) | `INSPECTION` / `AIRFRAME` | 2 | `DA-BXA` | `COMPLETED` (end `2013-04-12`) | `TIRE-900` ×4 @ 800, `BRAKE-200` ×2 @ 1200 |
| `AVIONICS-DIAGNOSTIC` (3) | `INSPECTION` / `AVIONICS` | 3 | `FH-XKA` | `COMPLETED` (end `2023-09-21`) | none |
| `ENGINE-REPAIR` (4) | `OVERHAUL` / `ENGINE` | 4 | `CS-TUA` | `INLINE` ("In Progress") | `FUEL-PUMP-01` ×1 @ 3200, `OIL-FILTER-22` ×2 @ 250 |

Data issues in the seed:

* Start dates are relative to today (`now - 10`, `now - 5`, `now - 2`, `now`), but the end dates are fixed in the past, so completed records end before they start. The unchecked `markAsCompleted(doneList, endDate)` overload allows it.
* `DA-BXA` and `FH-XKA` do not exist in `aircraft-service`, which seeds `D-ABXA` and `F-HXKA`.
* Record 4 is described as "In Progress" but is `INLINE`, so `GET /ongoing` starts empty.
