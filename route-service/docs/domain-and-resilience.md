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
* `updateRoute(...)`, `changeRouteRange(...)`: exist but are not used.

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
* `IllegalStateException` has no handler, so rejected transitions return `500`.

## 3. Use Cases

| Use Case / Service | Responsibility | User Story |
| :--- | :--- | :--- |
| `RouteService.createRoute(...)` | Validate and create a route using `airport-service` data | — |
| `RouteService.updateRoute(...)` | Change destination, flight time, status | — |
| `RouteService.getRouteDetails(...)` / `getRouteHistory(...)` | Lookup by id | — |
| `RouteService.searchRoutes(...)` | `Specification`-based search by origin/destination | — |
| `RouteService.getRoutesFromAirport(...)` | Routes by origin airport | — |
| `RouteService.listActiveRoutes(...)` | Active routes sorted by usage or distance | — |
| `RouteService.searchAlternativeRoutes(...)` | Active routes for the same origin/destination pair | — |
| `RouteService.calculateTotalRouteDistance()` | Sum of `distanceKm` | — |
| `RouteExportService` (`@UseCase`) | GeoJSON and KML export | US228 |
| `RouteSearchService` | Routes compatible with an aircraft (capacity and range) — not used | — |



## 4. External Integrations

Synchronous `RestClient` integrations (Eureka load-balanced):

* `airport-service` (`AirportRestClientService`, base URL `http://airport-service`)
  * `GET /api/airports/{iataCode}` (route creation and destination change)
* `aircraft-service` (`AircraftRestClientService`, base URL `https://aircraft-service`)
  * `GET /api/hangar/aircraft`, `GET /api/hangar/aircraft/{registrationNumber}` — declared, but the client is not injected anywhere.
* `flight-service` (`FlightRestClientService`, base URL `http://flight-service`)
  * Injected into `RouteService`, but the class has no methods.

Contract gaps in the current code:

* `AirportClientDTO` expects `airportLocation`, `airportRunways`, and `version`, while `airport-service` returns `location` and `runways` (and no `version`). `airportLocation` is therefore always `null`, and `fromDTOtoAirportReference(...)` throws `NullPointerException` (`500`) for every operational airport.
* `airport-service` returns `409` for a missing airport; `RestClient` raises it as `HttpClientErrorException` → `500`.
* `AircraftClientDTO` expects `modelDTO` and `seatingPackDTO`, which `aircraft-service` does not return; `findAll()` also reads a JSON array as a single object. Not exercised, because the client is unused.

Inbound consumers of this service:

* `aircraft-service` → `GET /api/routes`, `GET /api/routes/{routeId}`, and `GET /api/routes/aircraft/{registration}` (the last one is not exposed here).
* `airport-service` → `GET /api/routes/from/{iataCode}`, `GET /api/routes`.
* `flight-service` → `GET /api/routes` (with and without `origin`/`destination`), `GET /api/routes/from/{iataCode}`, `GET /api/routes/{routeId}`.

## 5. Analytics and Export

* **Active routes:** `findByStatus(ACTIVE, sort)` by `distanceKm` ascending or `routeHistory.routeUsage` descending. `routeUsage` is never updated by the service.
* **Total distance:** sum of `distanceKm` of all routes, of any status.
* **Same-pair routes:** active routes with identical origin and destination; no multi-leg search.
* **GeoJSON / KML export (US228):** hand-built strings from all routes; requires `AirportLocation` on both airports; values are not escaped.

## 6. Resilience and Concurrency

Optimistic locking:

* Implemented with `@Version` on `Route.version`.
* A concurrent update fails with `ObjectOptimisticLockingFailureException` → `500`.

Exception handling:

* Centralized by `GlobalExceptionHandler`.
* `DomainException` maps to `409 Conflict`; it is used for validation failures, duplicates, non-operational airports, and missing routes.
* Malformed JSON (including unknown enum values) and `IllegalArgumentException` map to `400 Bad Request`.
* `ResourceNotFoundException` maps to `404 Not Found`, but is never thrown.
* Unhandled exceptions map to `500 Internal Server Error`.

Current gaps:

* Missing resources return `409` instead of `404`.
* No Bean Validation (`spring-boot-starter-validation` is not a dependency).

* No timeout, retry, or circuit breaker around `RestClient` calls; any `airport-service` failure becomes `500`.
* `UseCaseLoggingAdvice` uses the pointcut `within(UseCase)`, which matches the `UseCase` annotation type itself rather than classes annotated with it, so no use case calls are logged.

Security:

* `route-service` has no security dependency and performs no authentication or authorization.
* Role rules for the export endpoints are declared in `auth-service`'s `SecurityConfig`, which only applies to requests handled by `auth-service`.

## 7. Bootstrap

`Bootstrap` (`ApplicationRunner`) seeds data when the route table is empty. All airports are created with `new AirportReference(iataCode)` (no location).

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
