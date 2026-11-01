# Flight Operations Service — Domain and Resilience (As Implemented)

## 1. Domain Model

The service has a single aggregate, **Flight**. Aircraft, routes and airports are not stored here: a flight keeps the aircraft registration and a copy of the route data.

### 1.1. `Flight` (table `company_flights`)

* `flightId`: generated identifier (`IDENTITY`).
* `version`: optimistic locking field (`@Version`).
* `routeId`: embedded `RouteReference` (`routeId`, `originIata`, `destinationIata`, `routeName`); equality is by `routeId`.
* `aircraftRegistrationNumber`: embedded `AircraftReference` (column `aircraft_registration`).
* `flightStatus`: enum `FlightStatus` (`DELAYED`, `SCHEDULED`, `DEPARTED`, `CANCELED`, `ARRIVED`, `IN_PROGRESS`).
* `scheduledDeparture`, `scheduledArrival` (required).
* `timeOfDeparture`, `timeOfArrival` (set by `startFlight()` / `completeFlight()`).

Aggregate behavior in `Flight`:

* New flights start as `SCHEDULED`.
* `startFlight()`: only from `SCHEDULED` → `IN_PROGRESS`.
* `completeFlight()`: only from `IN_PROGRESS` → `ARRIVED`.
* `cancelFlight()`: any status except `ARRIVED` → `CANCELED`.
* `updateAircraft(...)`, `changeRoute(...)`: rejected for `ARRIVED` flights.
* `getFlightHours()`: real duration for `ARRIVED` flights (scheduled duration if the real one is under 10 minutes), otherwise `null`.

None of these methods is called by an endpoint, so every flight created through the API stays `SCHEDULED`. `DELAYED` and `DEPARTED` are never set.

## 2. Scheduling Rules

Scheduling is triggered by `POST /api/flights/scheduled`:

1. `FlightController` loads the aircraft and its model through `AircraftSearchService`.
2. `RouteService.scheduleFlight(...)` loads the route and both airports, runs `validateFlightSchedule(...)`, patches the route usage in `route-service`, and saves the new `Flight`.

Rules in `validateFlightSchedule(...)` (all `DomainException` → `409`):

* Aircraft must exist.
* `scheduledArrival` must be after `scheduledDeparture`.
* Route status must be `ACTIVE`.
* Origin and destination airports must be `OPERATIONAL`.
* Aircraft status must be `AVAILABLE`.
* Aircraft range (`meanRange` if greater than `0`, otherwise the model's `maximumRangeKm`) must be at least the required range (`routeRequirements.requiredRange`, otherwise `distanceKm`). No range at all → `"Aircraft range information is unavailable"`.
* Model `standardSeatingCapacity` must be at least `routeRequirements.requiredCapacity`, when the route defines it.
* No stored flight of the same aircraft may overlap the new interval (any status).

## 3. Use Cases

| Use Case / Service | Responsibility | User Story |
| :--- | :--- | :--- |
| `RouteService` (`@UseCase`) | Scheduling orchestration, validation, route usage update; flights by aircraft | US212, US213 |
| `AircraftSearchService` | Aircraft and model lookup in `aircraft-service` (also has unused filtering and ranking helpers) | US212 |
| `FlightSearchService` | All flights (also has unused per-aircraft and date-range helpers) | — |
| `FlightUtilizationReportService` | Flights per route | US229 |

`UseCaseLoggingAdvice` uses the pointcut `within(com.sidis.flightservice.UseCase)`, which matches the `UseCase` annotation type itself rather than classes annotated with it (`@within(...)`), so no use case calls are logged.

## 4. External Integrations

Synchronous `RestClient` integrations (all built from the shared `@LoadBalanced` builder of `GlobalRestClientConfig`, with 2-second timeouts and `JwtTokenInterceptor`):

| Client | Downstream Call | Used By | Error Handling |
| :--- | :--- | :--- | :--- |
| `AircraftRestClientService.findAircraftByRegistration` | `aircraft-service` `GET /api/hangar/aircraft/{registrationNumber}` | Scheduling | `404` → empty → `409` |
| `AircraftRestClientService.findAircraftModel` | `aircraft-service` `GET /api/catalog/{modelName}` | Scheduling | any `4xx` → empty → `409` |
| `RouteRestClientService.findByRouteId` | `route-service` `GET /api/routes/{routeId}` | Scheduling | `409` → `DomainException("Route does not exist")` |
| `RouteRestClientService.update` | `route-service` `PATCH /api/routes/{routeId}` | Scheduling | not caught (`500`) |
| `AirportRestClientService.findAirportByIataCode` | `airport-service` `GET /api/airports/{iataCode}` | Scheduling | any `4xx` → `DomainException("Airport does not exist")` |

Declared but not called by any endpoint: `AircraftRestClientService.findAll()` (`GET /api/hangar/aircraft`) and `countByStatus(...)` (`GET /api/hangar/analytics/status-summary`); `RouteRestClientService.getRoutes()`, `getRoutesFromAirport(...)` and the origin/destination search.

`JwtTokenInterceptor` adds `X-Gateway-Secret: sidis-g6-secret` to every outgoing call and copies the incoming `Authorization: Bearer` header, so calls pass the other services' gateway secret filters.

Contract notes:

* `AircraftClientDTO` reads `registrationNumber`, `modelName`, `status`, `manufacturingDate`, `totalFlightHours`, `meanRange`, `features`; `seatingPackDTO` is not returned by `aircraft-service` and stays `null`.
* `AircraftModelDTO` reads `modelName`, `manufacturer` and `specs` from `GET /api/catalog/{modelName}`.
* The route `PATCH` resends the current destination, flight time and status together with the new history, so `route-service` validates the destination airport again.

Inbound consumers of this service (over `http://flight-service` with their own `JwtTokenInterceptor`):

* `aircraft-service` → `GET /api/flights` (rankings, fuel per route) and `GET /api/flights/aircraft/{registration}` (utilization view). It expects `range` and `flightHours`, which this service does not return.

## 5. Analytics

* **Route utilization:** all flights grouped by `RouteReference` (`routeId`), counted, and sorted by count descending then route name ascending.

## 6. Resilience and Concurrency

Optimistic locking:

* Implemented with `@Version` on `Flight.version`.

Exception handling:

* Centralized by `GlobalExceptionHandler`.
* `DomainException`, `ObjectOptimisticLockingFailureException` and `IllegalStateException` map to `409 Conflict`.
* Bean validation, malformed JSON, and `IllegalArgumentException` map to `400 Bad Request`.
* `ResourceNotFoundException` maps to `404 Not Found`; it is only thrown by the unused `PeerForwarder`.
* Unhandled exceptions map to `500 Internal Server Error`.

Outgoing calls:

* `GlobalRestClientConfig` sets a 2-second connect timeout and a 2-second read timeout.
* Most client methods are annotated `@Retryable` (3 attempts, backoff 1 s then 2 s), but `FlightServiceApplication` has no `@EnableRetry`, so each call is made once. `findByRouteId` and `update` are not annotated at all.
* `PeerForwarder` (forward a GET to other instances of a service) exists but is not used.

Current gaps:

* Retries are not active and there is no circuit breaker; a downstream failure becomes `500`.
* Scheduling is not atomic across services: the route usage is incremented before the flight is saved, and it is not rolled back if the save fails.

Security:

* `FlightSecurityConfig` defines a stateless `SecurityFilterChain`: CSRF, form login and HTTP Basic disabled, all requests `permitAll()`, no role checks.
* `gatewaySecretFilter` (registered before `UsernamePasswordAuthenticationFilter`) requires `X-Gateway-Secret: sidis-g6-secret`; otherwise it writes `403` with the plain-text body `Unauthorized Access! Please use the API Gateway!`.
* Paths containing `/h2-console`, `/swagger-ui` or `/api-docs` skip the check.
* JWT validation happens only in the API Gateway; the service does not read `X-User-Name`.
* The gateway secret is a hardcoded constant, so any client that knows it can call the service directly without a JWT.

## 7. Bootstrap

`Bootstrap` (`ApplicationRunner`) seeds 6 `SCHEDULED` flights when the table is empty:

| Flight | Route (id, name) | Aircraft | Departure → Arrival |
| :--- | :--- | :--- | :--- |
| 1 | 1, `Porto-Lisbon` (`OPO`→`LIS`) | `CS-TUA` | 2026-06-15 08:00 → 09:00 |
| 2 | 1, `Porto-Lisbon` | `D-ABXA` | 2026-06-15 10:30 → 12:00 |
| 3 | 2, `Lisbon-Madrid` (`LIS`→`MAD`) | `F-HXKA` | 2026-06-15 14:00 → 15:20 |
| 4 | 3, `Madrid-Paris` (`MAD`→`CDG`) | `F-HXKA` | 2026-06-16 09:15 → 11:20 |
| 5 | 4, `Paris-London` (`CDG`→`LHR`) | `CS-TUA` | 2026-06-16 13:00 → 14:15 |
| 6 | 3, `Madrid-Paris` | `N789DL` | 2026-06-17 11:00 → 13:40 |

Route ids 1–4 match the first four routes seeded by `route-service` (same airport pairs, different names), and the registrations exist in `aircraft-service`. The seed does not call other services, so their route usage counters are not updated.
