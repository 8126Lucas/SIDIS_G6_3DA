# Flight Operations Service — Sequence Diagrams and Dynamic Views (As Implemented)

This document describes runtime behavior aligned with the current code implementation.

---

## 1. Scenario 1: Read Flows

### Description

Scenario 1 contains the read operations exposed by `FlightController`:

* **Flow A:** `GET /api/flights/aircraft/{registrationNumber}` (US213)
* **Flow B:** `GET /api/flights/reports/route-utilization` (US229)
* **Flow C:** `GET /api/flights`
* **Rejected requests:** gateway `401`, `gatewaySecretFilter` `403`

In every flow the API Gateway first validates the JWT and forwards the request with `X-Gateway-Secret` and `X-User-Name`; `gatewaySecretFilter` lets it through before the controller runs.

### Sequence Diagram Source

* `scenario-1-query-flights.puml`

### Key Steps

#### Flow A — Flights by Aircraft

1. Controller delegates to `RouteService.getScheduledFlightsForAircraft(...)`.
2. `FlightRepository` reads the flights with that registration (any status).
3. Controller maps them to `FlightResponse` and returns `200 OK` (`[]` for an unknown registration).

#### Flow B — Route Utilization

1. Controller delegates to `FlightUtilizationReportService.getRouteUtilizationReport()`.
2. The service loads all flights, groups them by `RouteReference`, and counts them.
3. Results are sorted by `flightCount` descending, then `routeName` ascending; controller returns `200 OK`.

#### Flow C — All Flights

1. Controller calls `FlightSearchService.findAll()` and returns every flight as `FlightResponse[]`.

---

## 2. Scenario 2: Schedule Flight

### Description

Scenario 2 describes `POST /api/flights/scheduled` (US212), which checks `aircraft-service`, `route-service` and `airport-service` synchronously and updates the route usage in `route-service`.

### Sequence Diagram Source

* `scenario-2-schedule-flight.puml`

### Key Steps

1. Controller loads the aircraft (`GET /api/hangar/aircraft/{registration}`) and its model (`GET /api/catalog/{modelName}`) from `aircraft-service`; either missing → `409`.
2. `RouteService` loads the route from `route-service` (`409` if missing) and both airports from `airport-service` (`409` if missing).
3. Rules: arrival after departure, route `ACTIVE`, airports `OPERATIONAL`, aircraft `AVAILABLE`, range, capacity, no overlapping flight of the same aircraft. Any failure → `DomainException` → `409`.
4. `RouteService` sends `PATCH /api/routes/{routeId}` with the route history and `routeUsage + 1`.
5. The new `Flight` is saved as `SCHEDULED` and `201 Created` is returned.

Notes:

* Every outgoing call goes through `JwtTokenInterceptor` and has a 2-second timeout; retries are not active.
* Downstream errors that the clients do not turn into `DomainException` (unavailable service, timeout, `5xx`) → `500`.

---

## 3. Exception and Resilience Summary

| Exception Type | Current Mapping |
| :--- | :--- |
| `ResourceNotFoundException` | `404 Not Found` (only thrown by the unused `PeerForwarder`) |
| `MethodArgumentNotValidException` | `400 Bad Request` |
| `HttpMessageNotReadableException` | `400 Bad Request` |
| `IllegalArgumentException` | `400 Bad Request` |
| `DomainException` | `409 Conflict` |
| `ObjectOptimisticLockingFailureException` | `409 Conflict` |
| `IllegalStateException` | `409 Conflict` |
| `Exception` | `500 Internal Server Error` |

Optimistic locking exists (`@Version` on `Flight`) and conflicts map to `409`. The `403` from `gatewaySecretFilter` and the `401` from the gateway are not `ErrorDetails` payloads.
