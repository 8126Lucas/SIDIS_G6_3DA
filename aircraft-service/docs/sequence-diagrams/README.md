# Aircraft Service — Sequence Diagrams and Dynamic Views (As Implemented)

This document describes runtime behavior aligned with the current code implementation.

---

## 1. Scenario 1: Read and Analytics Flows

### Description

Scenario 1 contains local read operations exposed by `HangarController`:

* **Flow A:** `GET /api/hangar/aircraft/{registrationNumber}` (US103)
* **Flow B:** `GET /api/hangar/aircraft?model=&status=&manufacturingDate=` (US104)
* **Flow C:** `GET /api/hangar/analytics/status-summary` (US205)
* **Flow D:** rejected requests (gateway `401`, `gatewaySecretFilter` `403`)

In every flow the API Gateway first validates the JWT and forwards the request with `X-Gateway-Secret` and `X-User-Name`; `gatewaySecretFilter` lets it through before the controller runs.

### Sequence Diagram Source

* `scenario-1-query-aircraft.puml`

### Key Steps

#### Flow A — Aircraft by Registration

1. Client requests one aircraft.
2. Controller delegates to `AircraftSearchService.spotAircraftInHangar(...)`.
3. Service calls `AircraftRepository.findById(...)`.
4. Missing aircraft throws `ResourceNotFoundException` → `404`.
5. Controller maps to `AircraftResponse` and returns `200 OK`.

#### Flow B — Filtered Search

1. Client sends optional filters and paging.
2. Controller builds a `PageRequest` and delegates to `findAircraftsMatchingFilter(...)`.
3. Service composes a JPA `Specification` (model name, status, manufacturing date).
4. Repository executes the query; controller returns page content as `200 OK`.

#### Flow C — Status Summary

1. Client requests the summary.
2. For each `AircraftAvailability`, the controller calls `countByAvailability(...)`.
3. Repository executes `countByStatus(...)`; controller returns the map as `200 OK`.

#### Flow D — Rejected Requests

1. A request without a valid `Authorization: Bearer` header is stopped by the gateway with `401` (empty body).
2. A direct call to the service port without `X-Gateway-Secret` is stopped by `gatewaySecretFilter` with `403` and the plain-text body `Unauthorized Access! Please use the API Gateway!`.
3. Paths containing `/h2-console`, `/swagger-ui` or `/api-docs` are not checked.

---

## 2. Scenario 2: Register Aircraft and Change Availability

### Description

Scenario 2 describes the write flows in `POST /api/hangar/aircraft` (US102) and `PATCH /api/hangar/aircraft/{registrationNumber}` (US105).

### Sequence Diagram Source

* `scenario-2-register-aircraft.puml`

### Key Steps

1. Client sends `POST /api/hangar/aircraft` with model name, registration, dates, hours, and availability.
2. Controller resolves the model through `AircraftModelSearchService` (`404` if missing).
3. Controller creates the `Aircraft` (initially `INACTIVE`).
4. `AircraftLifeCycleUpdaterService` applies the requested availability (`IN_FLIGHT` → `400`).
5. `AddAircraftUseCase` persists the aircraft; `201 Created` is returned.
6. Later, `PATCH` validates path/body match (`409`), loads the aircraft (`404`), and applies the new availability; `200 OK` is returned.

---

## 3. Scenario 3: Inter-Service Route Analytics

### Description

Scenario 3 describes flows that depend on other microservices:

* **Flow A:** `GET /api/hangar/aircraft/{registrationNumber}/compatible-routes` (US203) → `route-service`
* **Flow B:** `GET /api/hangar/analytics/fuel-efficiency/routes` (US227) → `flight-service` + `route-service`

### Sequence Diagram Source

* `scenario-3-route-analytics.puml`

### Key Steps

#### Flow A — Compatible Routes

1. Controller loads the aircraft locally (`404` if missing).
2. `RouteRestClientService` calls `GET /api/routes` on `route-service`; `JwtTokenInterceptor` adds `X-Gateway-Secret` and copies the `Authorization` header.
3. `RouteSearchService` keeps routes with `routeRequirements.requiredCapacity <= aircraftCapacity` and `distanceKm <= aircraftRange`.
4. Controller returns `200 OK`.

#### Flow B — Fuel Efficiency per Route

1. `FuelEfficiencyService` calls `GET /api/flights` on `flight-service` (through `JwtTokenInterceptor`).
2. Flights are grouped by `routeId`.
3. For each route, `GET /api/routes/{routeId}` is called on `route-service`.
4. The average L/km of the aircraft that flew the route is multiplied by `distanceKm`.
5. Controller returns `200 OK`.

Note:

* `FlightClientDTO` maps `aircraftRegistrationNumber` through `@JsonAlias`, so aircraft lookups by registration work.
* Every client call has a 2-second timeout and up to 3 attempts; after that the error is returned as `500`.
* Flow A reads capacity from `routeRequirements.requiredCapacity`; `originAirportIata` and `destinationAirportIata` are still `null` in the response.

---

## 4. Exception and Resilience Summary

| Exception Type | Current Mapping |
| :--- | :--- |
| `ResourceNotFoundException` | `404 Not Found` |
| `MethodArgumentNotValidException` | `400 Bad Request` |
| `HttpMessageNotReadableException` | `400 Bad Request` |
| `IllegalArgumentException` | `400 Bad Request` |
| `DomainException` | `409 Conflict` |
| `ObjectOptimisticLockingFailureException` | `409 Conflict` |
| `IllegalStateException` | `409 Conflict` |
| `Exception` | `500 Internal Server Error` |

Optimistic locking exists (`@Version` on `Aircraft`) and conflicts map to `409`. Downstream `RestClient` calls have 2-second timeouts and are retried up to 3 times; after that they are not handled separately (`500`). The `403` from `gatewaySecretFilter` and the `401` from the gateway are not `ErrorDetails` payloads.
