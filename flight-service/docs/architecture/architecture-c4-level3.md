# Flight Operations Service (`flight-service`) — C4 Level 3 Component View (As Implemented)

This document describes the current internal component architecture at C4 Level 3 based on implemented classes.

---

## 1. Current Component Responsibilities

The `flight-service` currently exposes one controller and three main application services:

* **`FlightController`**
   * `POST /api/flights/scheduled`
   * `GET /api/flights/aircraft/{registrationNumber}`
   * `GET /api/flights/reports/route-utilization`
* **`RouteService`**
   * Scheduling orchestration and business-rule validation.
   * Calls route and airport clients.
   * Performs local overlap validation through repository.
   * Updates route usage through route-service patch operation.
* **`AircraftSearchService`**
   * Aircraft lookup and filtering helpers.
   * Used by controller in scheduling flow.
* **`FlightUtilizationReportService`**
   * Aggregates route utilization by counting flights grouped by route.
* **`FlightRepository`**
   * Spring Data JPA persistence for `Flight` entities.
* **`GlobalExceptionHandler`**
   * Cross-cutting HTTP error normalization (`ErrorDetails`).

External integration clients:

* `RouteRestClientService` -> `route-service`
* `AircraftRestClientService` -> `aircraft-service`
* `AirportRestClientService` -> `airport-service`

Not present in current implementation:

* `maintenance-service` integration.
* A dedicated `FlightUtilizationController` (utilization is served by `FlightController`).

---

## 2. Architecture Diagram Source

PlantUML source file:

* `architecture-c4-level3.puml`

---

## 3. Data Flow and Dependencies

1. Inbound requests arrive to `FlightController` under `/api/flights/**`.
2. Scheduling flow (`POST /scheduled`) first resolves aircraft data through `AircraftSearchService`, then delegates to `RouteService`.
3. `RouteService` performs synchronous calls to `route-service` and `airport-service`, validates business rules, updates route usage, and persists the `Flight`.
4. Route utilization flow delegates to `FlightUtilizationReportService`, which reads all flights and groups by route (`flightCount`).
5. Exceptions are transformed by `GlobalExceptionHandler` into `ErrorDetails` payloads.