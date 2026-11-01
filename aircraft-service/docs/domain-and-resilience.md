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
| `FlightSearchService` | Count flights per aircraft (from `flight-service`) for model rankings | US204 |
| `AircraftSearchService.findMostUsedModelsByFlyingAircraftHours` | Sum `totalFlightHours` per model for rankings | US204 |
| `FuelEfficiencyService` (`@UseCase`) | Fuel metrics per aircraft and per route | US227 |
| `CalculationsService` | Monthly utilization per aircraft from `flight-service` flights | — |

Classes annotated with `@UseCase` are `@Service`, `@Validated` and `@Transactional`. `UseCaseLoggingAdvice` logs their calls (method, arguments, execution time) through the pointcut `@within(com.sidis.aircraftservice.UseCase)`.

## 4. External Integrations

Synchronous `RestClient` integrations (Eureka load-balanced, shared `RestClient.Builder` from `GlobalRestClientConfig` with `JwtTokenInterceptor`):

* `route-service`
  * `GET /api/routes` (compatible routes)
  * `GET /api/routes/{routeId}` (fuel efficiency per route)
  * `GET /api/routes/aircraft/{registration}` (declared, not used)
* `flight-service`
  * `GET /api/flights` (fuel efficiency, US204 `assignments` ranking)
  * `GET /api/flights/aircraft/{registration}` (utilization view)

`JwtTokenInterceptor` adds `X-Gateway-Secret: sidis-g6-secret` to every outgoing call and copies the incoming `Authorization: Bearer` header when the call runs inside an HTTP request. All client methods are `@Retryable` (see section 6).

Contract gaps in the current code:

* `route-service` exposes `GET /api/routes` and `GET /api/routes/{routeId}`, but not `GET /api/routes/aircraft/{registration}`.
* `RouteClientDTO` reads capacity from `routeRequirements.requiredCapacity` (matches `route-service`), but still expects `originAirportIata` and `destinationAirportIata` while `route-service` returns `originIataCode` and `destinationIataCode`; both are deserialized as `null`.
* `FlightClientDTO` maps `flightId`, `aircraftRegistrationNumber`, `scheduledDeparture` and `scheduledArrival` through `@JsonAlias`, but `flight-service` does not return `range` or `flightHours`, so those two fields are always `null` (fuel per route and monthly utilization have no hours to work with).

Inbound consumers of this service (all over `http://aircraft-service` with their own `JwtTokenInterceptor`, so they pass the secret filter):

* `flight-service` → `GET /api/hangar/aircraft/{registrationNumber}` and `GET /api/catalog/{modelName}` when scheduling a flight (its client also declares `GET /api/hangar/aircraft` and `GET /api/hangar/analytics/status-summary`, which no endpoint calls).
* `route-service` has a client for `GET /api/hangar/aircraft` and `GET /api/hangar/aircraft/{registrationNumber}`, but no code calls it.
* `maintenance-service` → `GET /api/hangar/aircraft/{registrationNumber}`, `GET /api/hangar/aircraft?size=1000`.

## 5. Analytics

* **Availability:** registrations grouped per `AircraftAvailability` (`EnumMap`), and counts via `countByStatus`.
* **Operational hours:** paged query sorted by `totalOperationalHours` descending, returned as an ordered map.
* **Fuel per aircraft:** `fuelCapacityLiters / maximumRangeKm`, rounded to 2 decimals, sorted ascending.
* **Fuel per route:** average L/km of aircraft that flew the route × route `distanceKm`, sorted by `routeId`.
* **Model rankings (US204):** top 5 models by number of flights (`assignments`) or by summed `totalFlightHours` (`flight-hours`).
* **Monthly utilization:** for each aircraft and each of the last 12 months, `sum(flightHours in month) / hours in month × 100`, rounded to 2 decimals.

## 6. Resilience and Concurrency

Optimistic locking:

* Implemented with `@Version` on `Aircraft.version`.
* `AircraftModel` has no version field; concurrent patches are last-write-wins, and `POST` with an existing `modelName` overwrites it (merge).

Exception handling:

* Centralized by `GlobalExceptionHandler`.
* `DomainException`, `IllegalStateException` and `ObjectOptimisticLockingFailureException` map to `409 Conflict`.
* Bean validation, malformed JSON, and `IllegalArgumentException` map to `400 Bad Request`.
* `ResourceNotFoundException` maps to `404 Not Found`.
* Unhandled exceptions map to `500 Internal Server Error`.

Outgoing calls:

* `GlobalRestClientConfig` sets a 2-second connect timeout and a 2-second read timeout on every `RestClient`.
* `RouteRestClientService` and `FlightRestClientService` methods are `@Retryable` (any `Exception`, 3 attempts, backoff 1 s then 2 s); `@EnableRetry` is on `AircraftServiceApplication`. There is no `@Recover` method or circuit breaker, so after the third failure the error becomes `500`.

Peer forwarding (`PeerForwarder`):

* Used only by `GET /api/hangar/aircraft/{registrationNumber}` when the aircraft is not found locally.
* Asks Eureka for all `aircraft-service` instances, skips the one with the same port as the current instance, and calls `GET /api/hangar/aircraft/{registrationNumber}` on each (with `X-Gateway-Secret` and the caller's `Authorization`); any error is ignored. If no peer answers, it throws `ResourceNotFoundException` (`404`, `"Resource not found in any instance"`).
* Peers are told apart only by port, so instances on other hosts that use the same port are skipped.
* The `RestClient` comes from the `@LoadBalanced` builder, which treats the peer's host name or IP as a service id; such names are not registered in Eureka, so the peer call fails and the endpoint returns `404`.

Current gaps:

* No circuit breaker or fallback; repeated failures of a downstream service are retried on every request.

## 7. Security

`AircraftSecurityConfig` defines a stateless `SecurityFilterChain`:

* CSRF, form login and HTTP Basic are disabled; sessions are `STATELESS`.
* All requests are `permitAll()`; there is no authentication or role check inside the service.
* `gatewaySecretFilter` (`OncePerRequestFilter`, registered before `UsernamePasswordAuthenticationFilter`) requires `X-Gateway-Secret: sidis-g6-secret`. Otherwise it writes `403` with the plain-text body `Unauthorized Access! Please use the API Gateway!`.
* Paths containing `/h2-console`, `/swagger-ui` or `/api-docs` skip the check. The OpenAPI document is served at `/api/docs`, which does not match, so Swagger UI cannot load the spec.

Responsibilities split with the API Gateway:

* The gateway's `JwtAuthentication` filter validates the JWT signature (same secret as `auth-service`), returns `401` when it is missing or invalid, and adds `X-User-Name` and `X-Gateway-Secret` to the forwarded request.
* `aircraft-service` trusts the secret header only; it does not validate the JWT and does not use `X-User-Name`.
* The `jjwt` dependency and `security.jwt.*` properties were removed from this service.

Known weaknesses:

* The gateway secret is a hardcoded constant in `AircraftSecurityConfig`, `JwtTokenInterceptor` and the gateway filter, not a configuration property.
* Any client that knows the secret can call the service directly without a JWT.

## 8. Bootstrap

`Bootstrap` (`ApplicationRunner`) seeds data when tables are empty:

* 13 aircraft models (Boeing, Airbus, Embraer, Bombardier, ATR).
* 9 aircraft: `CS-TUA` (`INACTIVE`), `D-ABXA` (`AVAILABLE`), `F-HXKA` (`MAINTENANCE`), `G-X321`, `N789DL` (`INACTIVE`), `PR-EMJ` (`IN_FLIGHT`), and `TEST-001`, `TEST-002`, `TEST-003` (`INACTIVE`), with seating packs, features, certifications, and installed components.
