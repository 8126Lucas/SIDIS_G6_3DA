# Route Service — Domain and Resilience (As Implemented)

## 1. Domain Model

The service has a single aggregate, **Route**, with embedded value objects. Airports are not entities here: each route stores a copy of the airport data (`AirportReference`).

### 1.1. `Route` (table `company_routes`)

* `routeId`: generated identifier (sequence `company_routes_seq`, starting at `1`).
* `version`: optimistic locking field (`@Version`).
* `routeName`: required; uniqueness is checked on creation only (not a database constraint).
* `status`: enum `RouteStatus` (`ACTIVE`, `INACTIVE`, `ARCHIVED`), required.
* `type`: enum `RouteType` (`DIRECT`, `SCALED`), required.
* `originAirport` / `destinationAirport`: embedded `AirportReference` with columns prefixed `origin_*` / `destination_*`.
* `routeHistory`: embedded `RouteHistory`.
* `estimatedFlightTimeHours`: required.
* `distanceKm`: required.
* `routeRequirements`: embedded `RouteRequirements`.

### 1.2. Value Objects

* `AirportReference`: `iataCode` (3 chars) and an optional embedded `AirportLocation`. Equality is by `iataCode`.
* `AirportLocation`: `city`, `country`, `timezone`, `region`, `latitude`, `longitude`.
* `RouteHistory`: `routeBegin` (required), `routeFinish`, `routeUsage`.
* `RouteRequirements`: `requiredRange`, `requiredCapacity`.

Aggregate behavior in `Route`:

* `activateRoute()`, `suspendRoute()`, `retireRoute()`: status transitions (section 2).
* `updateEstimatedFlightTime(...)`: must be positive (`IllegalArgumentException`).
* `changeDestination(...)`: not `null` and different from origin (`IllegalArgumentException`).
* `updateRouteHistory(...)`: replaces the whole `RouteHistory` when it is not `null` (used by `PATCH /api/routes/{routeId}`).
* `updateRoute(...)`, `changeRouteRange(...)`: exist but are not used.

`AirportReference` rejects an IATA code that is `null` or not 3 characters long (`IllegalArgumentException`).

The 8-argument constructor (used by `RouteService.createRoute` and part of `Bootstrap`) sets `distanceKm = 1.0`.

## 2. Lifecycle Rules

New routes start as `ACTIVE` with `routeBegin = today` and `routeUsage = 0`.

Transitions applied by `RouteService.updateRoute(...)`:

| Requested Status | Domain Method | Rule in Current Code |
| :--- | :--- | :--- |
| `ACTIVE` | `activateRoute()` | Rejected when current status is `INACTIVE` (`IllegalStateException`, message `"Retired routes cannot be activated"`) |
| `INACTIVE` | `suspendRoute()` | Rejected when current status is `ARCHIVED` (`IllegalStateException`) |
| `ARCHIVED` | `retireRoute()` + `routeHistory.endRoute(today)` | Always allowed |

Consequences of the current rules:

* A suspended (`INACTIVE`) route cannot be reactivated, while an `ARCHIVED` route can go back to `ACTIVE` (the guard checks `INACTIVE` although the message refers to retired routes).
* Rejected transitions (`IllegalStateException`) return `409`.

## 3. Use Cases

| Use Case / Service | Responsibility | User Story |
| :--- | :--- | :--- |
| `RouteService.createRoute(...)` | Validate and create a route using `airport-service` data | — |
| `RouteService.updateRoute(...)` | Change destination, flight time, status, history | — |
| `RouteService.getRouteDetails(...)` / `getRouteHistory(...)` | Lookup by id | — |
| `RouteService.searchRoutes(...)` | `Specification`-based search by origin/destination | — |
| `RouteService.getRoutesFromAirport(...)` | Routes by origin airport | — |
| `RouteService.listActiveRoutes(...)` | Active routes sorted by usage or distance | — |
| `RouteService.searchAlternativeRoutes(...)` | Active routes for the same origin/destination pair | — |
| `RouteService.calculateTotalRouteDistance()` | Sum of `distanceKm` | — |
| `RouteExportService` (`@UseCase`) | GeoJSON and KML export | US228 |
| `RouteSearchService` | Routes compatible with an aircraft (capacity and range) — not used | — |



## 4. External Integrations

Synchronous `RestClient` integrations (all built from the shared `@LoadBalanced` builder of `GlobalRestClientConfig`, with 2-second timeouts and `JwtTokenInterceptor`):

* `airport-service` (`AirportRestClientService`, base URL `http://airport-service`)
  * `GET /api/airports/{iataCode}` (route creation and destination change)
* `aircraft-service` (`AircraftRestClientService`, base URL `http://aircraft-service`)
  * `GET /api/hangar/aircraft`, `GET /api/hangar/aircraft/{registrationNumber}` — declared, but the client is not injected anywhere.
* `flight-service` (`FlightRestClientService`, base URL `http://flight-service`)
  * Injected into `RouteService`, but the class has no methods.

`JwtTokenInterceptor` adds `X-Gateway-Secret: sidis-g6-secret` to every outgoing call and copies the incoming `Authorization: Bearer` header, so calls pass `airport-service`'s gateway secret filter.

Contract notes:

* `AirportClientDTO` maps `airport-service`'s `location` to `airportLocation` (`@JsonProperty`), so routes get the airport coordinates. `airportRunways` and `version` do not match the response and stay `null`; they are not used.
* Any `4xx` from `airport-service` (for example `409` for a missing airport) is caught and rethrown as `DomainException("Airport does not exist")` → `409`.
* `AircraftClientDTO` now reads `modelName`; `RouteSearchService.findCompatibleRoutes(...)` takes the model specs as a separate argument. Neither is used by an endpoint.

Inbound consumers of this service (over `http://route-service` with their own `JwtTokenInterceptor`):

* `aircraft-service` → `GET /api/routes`, `GET /api/routes/{routeId}`, and `GET /api/routes/aircraft/{registration}` (the last one is not exposed here).
* `airport-service` → `GET /api/routes` (it still declares `GET /api/routes/from/{iataCode}`, but no longer calls it).
* `flight-service` → `GET /api/routes/{routeId}` and `PATCH /api/routes/{routeId}` when scheduling a flight; `GET /api/routes` and `GET /api/routes/from/{iataCode}` are declared but not used.

## 5. Analytics and Export

* **Active routes:** `findByStatus(ACTIVE, sort)` by `distanceKm` ascending or `routeHistory.routeUsage` descending. `routeUsage` changes only when a client sends a new `history` (as `flight-service` does after scheduling).
* **Total distance:** sum of `distanceKm` of all routes, of any status.
* **Same-pair routes:** active routes with identical origin and destination; no multi-leg search.
* **GeoJSON / KML export (US228):** hand-built strings from all routes; requires `AirportLocation` with coordinates on both airports (GeoJSON: `IllegalStateException` → `409` otherwise; KML: `NullPointerException` → `500`); KML text is wrapped in `CDATA`, GeoJSON values are not escaped.

## 6. Resilience and Concurrency

Optimistic locking:

* Implemented with `@Version` on `Route.version`.
* A concurrent update fails with `ObjectOptimisticLockingFailureException` → `409`.

Exception handling:

* Centralized by `GlobalExceptionHandler`.
* `DomainException` maps to `409 Conflict`; it is used for validation failures, duplicates, non-operational or missing airports, and missing routes.
* `ObjectOptimisticLockingFailureException` and `IllegalStateException` (rejected status transitions, GeoJSON export without coordinates) map to `409 Conflict`.
* Malformed JSON (including unknown enum values) and `IllegalArgumentException` map to `400 Bad Request`.
* `ResourceNotFoundException` maps to `404 Not Found`; it is only thrown by the unused `PeerForwarder`.
* Unhandled exceptions map to `500 Internal Server Error`.

Outgoing calls:

* `GlobalRestClientConfig` sets a 2-second connect timeout and a 2-second read timeout.
* `AirportRestClientService` and `AircraftRestClientService` methods are annotated `@Retryable` (3 attempts, backoff 1 s then 2 s), but `RouteServiceApplication` has no `@EnableRetry`, so each call is made once.

Current gaps:

* Missing resources return `409` instead of `404`.
* No Bean Validation (`spring-boot-starter-validation` is not a dependency).
* Retries are not active and there is no circuit breaker; any `airport-service` failure other than `4xx` becomes `500`.
* `UseCaseLoggingAdvice` uses the pointcut `within(com.sidis.routeservice.UseCase)`, which matches the `UseCase` annotation type itself rather than classes annotated with it, so no use case calls are logged.

Security:

* `RouteSecurityConfig` defines a stateless `SecurityFilterChain`: CSRF, form login and HTTP Basic disabled, all requests `permitAll()`, no role checks.
* `gatewaySecretFilter` (registered before `UsernamePasswordAuthenticationFilter`) requires `X-Gateway-Secret: sidis-g6-secret`; otherwise it writes `403` with the plain-text body `Unauthorized Access! Please use the API Gateway!`.
* Paths containing `/h2-console`, `/swagger-ui` or `/api-docs` skip the check.
* JWT validation happens only in the API Gateway; the service does not read `X-User-Name`.
* The gateway secret is a hardcoded constant, so any client that knows it can call the service directly without a JWT.

## 7. Bootstrap

`Bootstrap` (`ApplicationRunner`) seeds data when the route table is empty. The five airports (`OPO`, `LIS`, `MAD`, `CDG`, `LHR`) are created with their city, coordinates, region, timezone and country, so the seeded routes can be exported.

* 4 test routes (`TEST ROUTE 1` to `4`): `OPO`→`LIS`, `LIS`→`MAD`, `MAD`→`CDG`, `CDG`→`LHR`; `estimatedFlightTimeHours = 1.0`, `distanceKm = 1.0`, `routeUsage = 0`.
* 9 named routes with explicit distance and usage:

| Route | Pair | Type | Distance (km) | Usage |
| :--- | :--- | :--- | :--- | :--- |
| `Pati&Patatá` | `OPO`→`LIS` | `SCALED` | 200 | 0 |
| `Iberia Connect` | `OPO`→`MAD` | `DIRECT` | 500 | 2 |
| `TAP Express` | `LIS`→`MAD` | `DIRECT` | 600 | 1 |
| `Air France Iberia Codeshare` | `MAD`→`CDG` | `DIRECT` | 1000 | 5 |
| `Air France - British Airways Alliance` | `CDG`→`LHR` | `DIRECT` | 700 | 8 |
| `British Airways` | `LHR`→`LIS` | `DIRECT` | 900 | 10 |
| `Iberia Regional` | `MAD`→`OPO` | `DIRECT` | 600 | 1 |
| `Air France` | `CDG`→`MAD` | `DIRECT` | 700 | 6 |
| `British Airways` | `LHR`→`CDG` | `DIRECT` | 800 | 7 |

All seeded routes are `ACTIVE`. The name `British Airways` is used twice, since seeding bypasses the uniqueness check.
