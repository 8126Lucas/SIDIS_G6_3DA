# Airport Service — Domain and Resilience (As Implemented)

## 1. Domain Model

The service has a single aggregate, **Airport**, with embedded value objects and owned child entities.

### 1.1. `Airport` (table `company_registered_airports`)

* `iataCode`: assigned identifier (`@Id`), validated as `[A-Z]{3}` on registration.
* `version`: optimistic locking field (`@Version`).
* `airportType`, `name`: free text.
* `status`: enum `AirportStatus` (`OPERATIONAL`, `CLOSED`, `UNDER_MAINTENANCE`).
* `airportLocation`: embedded `AirportLocation` value object:
  * `city`, `country`, `timezone`, `region`, `latitude`, `longitude` (all required).
* `facilities`: embedded `Facilities` value object:
  * `terminalCount` (required), `gateCount` (required), `services` (element collection in table `facility_services`).
* `routeDistance`: decimal, stored but not used by any business rule.
* `airportRunways`: `@OneToMany` `Runway` (table `company_airports_runways`): `runwayName`, `length`, `orientation`, `status` (`RunwayStatus`: `OPEN`, `IN_USE`, `UNDER_MAINTENANCE`).
* `certifications`: `@OneToMany` `Certification` (table `company_airports_certifications`): `name`, `category`, `startsAt`, `expiresAt`.
* `contacts`: element collection of embedded `Contact` in table `airport_contacts`: `type` (`ContactType`: `PHONE`, `EMAIL`, `FAX`, `SOCIAL_NETWORK`), `value`.
* `airportPhotos`: element collection in table `airport_images`.
* `operationalHours`: free text, default `"07:00-22:00"`.

Runways and certifications are joined by column `airport_iata_code` and saved in cascade (`CascadeType.ALL`).

Aggregate behavior in `Airport`:

* `addCertification(...)`: appends a certification and sets its back-reference.
* `changeStatus(...)`: sets the new status.
* `updateDetails(name, operationalHours, contacts)`: replaces `name` and `contacts`, and calls `changeOperationalHours(...)` when `operationalHours` is not `null`.
* `changeOperationalHours(...)`: rejects a blank value (`IllegalArgumentException`).

Apart from operational hours, there are no invariants enforced inside the entity (no null checks, no runway rules). Certification dates are checked only on the request of `POST /{iataCode}/certifications` (`@AssertTrue`), not in `Certification` or in the CSV import.

## 2. Lifecycle Rules

Airports are created with the status sent by the client (Bootstrap seeds all of them as `OPERATIONAL`).

`AirportService.updateStatus(...)` accepts any `AirportStatus` value:

| Requested Status | Effect |
| :--- | :--- |
| `OPERATIONAL` | `changeStatus(OPERATIONAL)` |
| `CLOSED` | `changeStatus(CLOSED)` |
| `UNDER_MAINTENANCE` | `changeStatus(UNDER_MAINTENANCE)` |
| Unknown value | Rejected by `AirportStatus.valueOf(...)` with `IllegalArgumentException` (`400`) |

There is no transition guard, and the airport status is not checked by other operations (for example, certifications can be added to a `CLOSED` airport).

`RunwayStatus` is only set on creation; there is no endpoint to change a runway.

## 3. Use Cases

| Use Case / Service | Responsibility |
| :--- | :--- |
| `AirportService.execute(...)` | Validate IATA code and uniqueness, persist a new `Airport` |
| `AirportService.addCertification(...)` | Add a certification to an existing airport |
| `AirportService.getAirportByIata(...)` | Lookup by IATA code |
| `AirportService.searchAirports(...)` | `Specification`-based search by city, country, name |
| `AirportService.updateStatus(...)` | Status change |
| `AirportService.updateDetails(...)` | Replace name and contacts, change operational hours |
| `AirportService.groupAirportsBy(...)` | Group by country, city or region |
| `AirportService.findRoutesByAirport(...)` | Routes departing from or arriving at the airport (`route-service`) |
| `AirportService.busiestAirports()` | Rank airports by number of routes (`route-service`) |
| `AirportCsvService.importFile(...)` | Parse a CSV into `Airport` objects (no persistence) |

`AirportService` is annotated with `@UseCase` (Spring `@Service`, `@Validated`, `@Transactional`) and also with `jakarta.transaction.Transactional`; every public method runs in its own transaction.

## 4. External Integrations

Synchronous `RestClient` integration (`RouteRestClientService`, built from the shared `@LoadBalanced` builder of `GlobalRestClientConfig` with base URL `http://route-service`):

* `route-service`
  * `GET /api/routes` (airport routes, busiest airports)
  * `GET /api/routes/from/{airportIataCode}` (declared, no longer used)
  * `GET /api/routes?origin=&destination=` (declared in `findByOriginAirport_IataCodeOrDestinationAirport_IataCode`, not used)

All three endpoints are exposed by `route-service`, and `RouteClientDTO` matches its `RouteResponse` (`routeId`, `routeName`, `status`, `type`, `originIataCode`, `destinationIataCode`, `estimatedFlightTimeHours`, `distanceKm`, `routeRequirements`, `routeHistory`).

`JwtTokenInterceptor` adds `X-Gateway-Secret: sidis-g6-secret` to every outgoing call and copies the incoming `Authorization: Bearer` header, so calls pass `route-service`'s gateway secret filter.

Contract notes:

* `RouteResponse` re-exposes the data with different field names (`routeStatus`, `routeType`, `originAirportIata`, `destinationAirportIata`, `routeRequirementsDTO`, `routeHistoryDTO`).
* Airport routes are filtered in this service from the full route list (origin or destination equal to the IATA code).

Inbound consumers of this service (over `http://airport-service` with their own `JwtTokenInterceptor`):

* `flight-service` → `GET /api/airports/{iataCode}`.
* `route-service` → `GET /api/airports/{iataCode}`.

Both consumers receive `409 Conflict` (not `404`) when the airport does not exist and turn any `4xx` into their own `DomainException("Airport does not exist")`.

## 5. Analytics

* **Busiest airports:** for each route, the origin and destination airports found locally are incremented by one; results are sorted by count descending. Airports without routes are omitted.
* **Grouping:** in-memory `Collectors.groupingBy` over all airports, keyed by `country`, `city` or `region`.

## 6. Resilience and Concurrency

Optimistic locking:

* Implemented with `@Version` on `Airport.version`.
* `updateStatus`, `updateDetails`, and `addCertification` load and save the airport in the same transaction; a concurrent update fails with `ObjectOptimisticLockingFailureException`.

Exception handling:

* Centralized by `GlobalExceptionHandler`.
* `DomainException` maps to `409 Conflict`; it is used for invalid IATA codes, duplicates, missing airports, and unknown grouping criteria.
* `ObjectOptimisticLockingFailureException` and `IllegalStateException` map to `409 Conflict`.
* Malformed JSON, the certification date check, and `IllegalArgumentException` (unknown enum names, blank operational hours) map to `400 Bad Request`.
* `ResourceNotFoundException` maps to `404 Not Found`; it is only thrown by the unused `PeerForwarder`.
* Unhandled exceptions map to `500 Internal Server Error`.

Outgoing calls:

* `GlobalRestClientConfig` sets a 2-second connect timeout and a 2-second read timeout.
* `RouteRestClientService` methods are annotated `@Retryable` (3 attempts, backoff 1 s then 2 s), but `AirportServiceApplication` has no `@EnableRetry`, so the annotation has no effect and each call is made once.
* `PeerForwarder` (forward a GET to other instances of a service) exists but is not used by any controller.

Current gaps:

* Missing resources return `409` instead of `404`.
* Most requests have no Bean Validation constraints; missing nested objects (`location`, `facilities`, `runways`) cause `NullPointerException` (`500`).
* Retries are not active (missing `@EnableRetry`), and there is no circuit breaker; any `route-service` failure becomes `500`.
* The CSV import is not atomic: each airport is saved in a separate transaction.
* `UseCaseLoggingAdvice` uses the pointcut `within(com.sidis.airportservice.UseCase)`, which matches the `UseCase` annotation type itself rather than classes annotated with it (`@within(com.sidis.airportservice.UseCase)`), so no use case calls are logged.

Security:

* `AirportSecurityConfig` defines a stateless `SecurityFilterChain`: CSRF, form login and HTTP Basic disabled, all requests `permitAll()`, no role checks.
* `gatewaySecretFilter` (registered before `UsernamePasswordAuthenticationFilter`) requires `X-Gateway-Secret: sidis-g6-secret`; otherwise it writes `403` with the plain-text body `Unauthorized Access! Please use the API Gateway!`.
* Paths containing `/h2-console`, `/swagger-ui` or `/api-docs` skip the check. The OpenAPI document is served at `/api/docs`, which does not match, so Swagger UI cannot load the spec.
* JWT validation happens only in the API Gateway; the service does not read `X-User-Name`.
* The gateway secret is a hardcoded constant, so any client that knows it can call the service directly without a JWT.

## 7. Bootstrap

`Bootstrap` (`ApplicationRunner`) seeds data when the airport table is empty:

* 5 airports, all `OPERATIONAL` and of type `International`:
  * `OPO` — Francisco Sá Carneiro Airport (Porto, Portugal), 1 runway.
  * `LIS` — Humberto Delgado Airport (Lisbon, Portugal), 2 runways (one `UNDER_MAINTENANCE`).
  * `MAD` — Adolfo Suárez Madrid–Barajas Airport (Madrid, Spain), 4 runways.
  * `CDG` — Charles de Gaulle Airport (Paris, France), 3 runways.
  * `LHR` — London Heathrow Airport (London, United Kingdom), 2 runways.
* Each airport has facilities, certifications, contacts (phone and email), and photos.
