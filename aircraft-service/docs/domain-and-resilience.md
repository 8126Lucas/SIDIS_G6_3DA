# Aircraft Service — Domain and Resilience (As Implemented)

## 1. Domain Model

The service has two bounded areas: **AircraftCatalog** (models) and **Aircraft** (hangar instances).

### 1.1. `AircraftModel` (table `aircraft_models_catalog`)

* `modelName`: assigned identifier (`@Id`).
* `manufacturer`: required.
* `modelImage`: element collection in table `aircraft_images`.
* `aircraftModelSpecs`: embedded `AircraftSpecs` value object:
  * `fuelCapacityLiters` (required), `maximumRangeKm` (required), `cruisingSpeedKph` (required), `standardSeatingCapacity`.

### 1.2. `Aircraft` (table `company_aircrafts`)

* `registrationNumber`: assigned identifier (`@Id`, not updatable).
* `version`: optimistic locking field (`@Version`).
* `model`: `@ManyToOne` to `AircraftModel`.
* `status`: enum `AircraftAvailability` (`AVAILABLE`, `IN_FLIGHT`, `MAINTENANCE`, `INACTIVE`).
* `manufacturingDate`, `totalOperationalHours`, `totalFlightHours`, `meanRange`.
* `features`: element collection in table `aircraft_features`.
* `seatingPack`: embedded `SeatingPack` (`seatingCapacity`, `seatingOrientation`).
* `components`: `@OneToMany` `InstalledComponent` (`serialNumber`, `partNumber`, `status`).
* `certifications`: `@OneToMany` `AircraftCertification` (`name`, `category`, `startAt`, `expiresAt`).

Aggregate invariants enforced in `Aircraft`:

* A component cannot be `null` or installed twice (same `serialNumber`).
* Certifications, seating configuration, and features cannot be `null`/blank.
* Hour updates must be positive.
* `isReadyForFlight()` is `true` only when status is `AVAILABLE` and all certifications are valid (not expired).

## 2. Lifecycle Rules

New aircraft start as `INACTIVE` with `meanRange = 0.0`.

Transitions applied by `AircraftLifeCycleUpdaterService.changeAvailability(...)`:

| Requested Status | Domain Method |
| :--- | :--- |
| `AVAILABLE` | `activateAircraft()` |
| `MAINTENANCE` | `sendToMaintenance()` |
| `INACTIVE` | `retireAircraft()` |
| `IN_FLIGHT` | Rejected with `IllegalArgumentException` (`400`) |

`sendAircraftInFlight()` exists in the domain but is only used by `Bootstrap`.

There is no transition guard (for example, any status can move to any other allowed status).

## 3. Use Cases

| Use Case / Service | Responsibility | User Story |
| :--- | :--- | :--- |
| `AddModelUseCase` (`@UseCase`) | Persist a new `AircraftModel` | US101, US202 |
| `AircraftModelUpdater` | Patch images, manufacturer, and specs | US201 |
| `AddAircraftUseCase` (`@UseCase`) | Persist a new `Aircraft` | US102 |
| `AircraftSearchService` | Lookup, filtered search, counts, features | US103, US104, US205, US206 |
| `AircraftLifeCycleUpdaterService` | Availability transitions | US105 |
| `RouteSearchService` | Filter compatible routes | US203 |
| `FuelEfficiencyService` (`@UseCase`) | Fuel metrics per aircraft and per route | US227 |
| `CalculationsService` | Monthly utilization (flight hours hardcoded `0.0`) | — |

Classes annotated with `@UseCase` are transactional, validated, and logged by `UseCaseLoggingAdvice` (method, arguments, execution time).

## 4. External Integrations

Synchronous `RestClient` integrations (Eureka load-balanced):

* `route-service`
  * `GET /api/routes` (compatible routes)
  * `GET /api/routes/{routeId}` (fuel efficiency per route)
  * `GET /api/routes/aircraft/{registration}` (declared, not used)
* `flight-service`
  * `GET /api/flights` (fuel efficiency)
  * `GET /api/flights/aircraft/{registration}` (declared, not used)

Contract gaps in the current code:

* `route-service` exposes `GET /api/routes` and `GET /api/routes/{routeId}`, but not `GET /api/routes/aircraft/{registration}`.
* `RouteClientDTO` expects `originAirportIata`, `destinationAirportIata`, `capacity`, while `route-service` returns `originIataCode`, `destinationIataCode` and `routeRequirements.requiredCapacity`; these fields are deserialized as `null` (a `null` `capacity` makes the compatible-routes filter throw `NullPointerException`).
* `flight-service` does not expose `GET /api/flights`.
* `FlightClientDTO` expects `id`, `aircraftRegistration`, `routeId`, `range`, while `flight-service` returns `flightId`, `aircraftRegistrationNumber`, `routeId` and no range.

Inbound consumers of this service:

* `flight-service` → `GET /api/hangar/aircraft/{registrationNumber}`.
* `route-service` and `maintenance-service` also have aircraft clients (see their own documentation).

## 5. Analytics

* **Availability:** registrations grouped per `AircraftAvailability` (`EnumMap`), and counts via `countByStatus`.
* **Operational hours:** paged query sorted by `totalOperationalHours` descending, returned as an ordered map.
* **Fuel per aircraft:** `fuelCapacityLiters / maximumRangeKm`, rounded to 2 decimals, sorted ascending.
* **Fuel per route:** average L/km of aircraft that flew the route × route `distanceKm`, sorted by `routeId`.

## 6. Resilience and Concurrency

Optimistic locking:

* Implemented with `@Version` on `Aircraft.version`.
* `AircraftModel` has no version field; concurrent patches are last-write-wins, and `POST` with an existing `modelName` overwrites it (merge).

Exception handling:

* Centralized by `GlobalExceptionHandler`.
* `DomainException` maps to `409 Conflict`.
* Bean validation, malformed JSON, and `IllegalArgumentException` map to `400 Bad Request`.
* `ResourceNotFoundException` maps to `404 Not Found`.
* Unhandled exceptions map to `500 Internal Server Error`.

Current gaps:

* No dedicated handler for `OptimisticLockingFailureException`.
* No timeout, retry, or circuit breaker around `RestClient` calls; any downstream failure becomes `500`.

Security:

* `spring-boot-starter-security` and `jjwt` are on the classpath and `security.jwt.*` properties are set, but no `SecurityFilterChain` or JWT filter is defined in this service, so Spring Boot's default security auto-configuration applies.

## 7. Bootstrap

`Bootstrap` (`ApplicationRunner`) seeds data when tables are empty:

* 13 aircraft models (Boeing, Airbus, Embraer, Bombardier, ATR).
* 9 aircraft: `CS-TUA` (`INACTIVE`), `D-ABXA` (`AVAILABLE`), `F-HXKA` (`MAINTENANCE`), `G-X321`, `N789DL` (`INACTIVE`), `PR-EMJ` (`IN_FLIGHT`), and `TEST-001`, `TEST-002`, `TEST-003` (`INACTIVE`), with seating packs, features, certifications, and installed components.
