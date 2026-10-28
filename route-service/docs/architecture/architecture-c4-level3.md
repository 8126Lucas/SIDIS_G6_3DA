# Route Service (`route-service`) — C4 Level 3 Component View (As Implemented)

This document describes the current internal component architecture at C4 Level 3 based on implemented classes.

---

## 1. Current Component Responsibilities

The `route-service` currently exposes one controller and the following main application services:

* **`RouteController`** (`/api/routes`)
   * Creation and update (`POST /`, `PATCH /{routeId}`).
   * Queries (`GET /{routeId}`, `GET /{routeId}/history`, `GET /`, `GET /from/{airportIataCode}`, `GET /active`, `GET /alternatives`).
   * Network analytics and export (`GET /total-distance`, `GET /export/geojson`, `GET /export/kml`).
* **`RouteService`** (`@UseCase`)
   * Creation rules (IATA format, distinct airports, unique name, operational airports), updates and status transitions, lookups, `Specification`-based search, sorting, and total distance.
* **`RouteExportService`** (`@UseCase`)
   * GeoJSON and KML generation (US228).
* **`RouteSearchService`**
   * Compatible-route filtering by aircraft capacity and range (not used by any endpoint).
* **`AirportRestClientService`** / **`AircraftRestClientService`** / **`FlightRestClientService`**
   * `RestClient` adapters for `airport-service` (used), `aircraft-service` (not injected), and `flight-service` (no methods).
* **`RouteRepository`**
   * Spring Data JPA persistence with `JpaSpecificationExecutor`.
* **`GlobalExceptionHandler`**
   * Cross-cutting HTTP error normalization (`ErrorDetails`).
* **`UseCaseLoggingAdvice`**
   * AOP logging aspect intended for `@UseCase` classes (its pointcut does not currently match them).

External integration clients:

* `AirportRestClientService` -> `airport-service`
* `AircraftRestClientService` -> `aircraft-service` (unused)
* `FlightRestClientService` -> `flight-service` (empty)

---

## 2. Architecture Diagram Source

PlantUML source file:

* `architecture-c4-level3.puml`

---

## 3. Data Flow and Dependencies

1. Inbound requests arrive through the API Gateway for `/api/routes/**`.
2. Creation and update go from `RouteController` to `RouteService`, which validates the airports through `AirportRestClientService` and persists through `RouteRepository`.
3. Read and analytics flows use `RouteService` over `RouteRepository`.
4. Export flows use `RouteExportService`, which reads all routes and builds the GeoJSON / KML text.
5. `aircraft-service`, `airport-service`, and `flight-service` call the read endpoints of this service.
6. Exceptions are transformed by `GlobalExceptionHandler` into `ErrorDetails` payloads.
