# Flight Operations Service — Sequence Diagrams and Dynamic Views (As Implemented)

This document describes runtime behavior aligned with the current code implementation.

---

## 1. Scenario 1: Read Flows

### Description

Scenario 1 contains two read operations currently exposed by `FlightController`:

* **Flow A:** `GET /api/flights/aircraft/{registrationNumber}`
* **Flow B:** `GET /api/flights/reports/route-utilization`

### Sequence Diagram Source

* `scenario-1-query-flights.puml`

### Key Steps

#### Flow A — Flights by Aircraft

1. Client requests flights for one aircraft registration.
2. Controller delegates to `RouteService.getScheduledFlightsForAircraft(...)`.
3. Service queries `FlightRepository.findByAircraftRegistrationNumber_RegistrationNumber(...)`.
4. Repository reads from `company_flights`.
5. Controller maps entities to `FlightResponse` and returns `200 OK`.

#### Flow B — Route Utilization

1. Client requests route utilization report.
2. Controller delegates to `FlightUtilizationReportService.getRouteUtilizationReport()`.
3. Service loads all flights and groups by `RouteReference`.
4. Service returns route metadata plus `flightCount` per route.
5. Controller returns `200 OK`.

Note:

* The report currently aggregates flight count only, not accumulated flight hours.

---

## 2. Scenario 2: Schedule Flight

### Description

Scenario 2 describes the implemented write flow in `POST /api/flights/scheduled`.

Synchronous external checks are performed against:

* `aircraft-service`
* `route-service`
* `airport-service`

Not currently used:

* `maintenance-service`

### Sequence Diagram Source

* `scenario-2-schedule-flight.puml`

### Key Steps

1. Client sends `POST /api/flights/scheduled` with route and schedule data.
2. Controller looks up aircraft through `AircraftSearchService`.
3. Controller delegates scheduling to `RouteService`.
4. `RouteService` fetches route and airports via RestClient adapters.
5. Business rules are evaluated (active route, operational airports, available aircraft, range, no local overlap).
6. Route usage is updated via route-service patch.
7. New `Flight` is persisted and `201 Created` is returned.
8. Rule violations throw `DomainException`, returned as `409 Conflict`.

---

## 3. Exception and Resilience Summary

| Exception Type | Current Mapping |
| :--- | :--- |
| `ResourceNotFoundException` | `404 Not Found` |
| `MethodArgumentNotValidException` | `400 Bad Request` |
| `HttpMessageNotReadableException` | `400 Bad Request` |
| `IllegalArgumentException` | `400 Bad Request` |
| `DomainException` | `409 Conflict` |
| `Exception` | `500 Internal Server Error` |

Optimistic locking exists (`@Version` on `Flight`), but there is no dedicated handler for `OptimisticLockingFailureException` in the current `GlobalExceptionHandler`.