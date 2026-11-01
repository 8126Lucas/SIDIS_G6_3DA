# Route Service (`route-service`) — C4 Level 3 Component View (As Implemented)

This document describes the current internal component architecture at C4 Level 3 based on implemented classes.

---

## 1. Current Component Responsibilities

The `route-service` currently exposes one controller and the following main application services:

* **`RouteSecurityConfig`**
   * Stateless `SecurityFilterChain` with `gatewaySecretFilter`, which rejects requests without `X-Gateway-Secret` (`403`).
* **`RouteController`** (`/api/routes`)
   * Creation and update (`POST /`, `PATCH /{routeId}`).
   * Queries (`GET /{routeId}`, `GET /{routeId}/history`, `GET /`, `GET /from/{airportIataCode}`, `GET /active`, `GET /alternatives`).
   * Network analytics and export (`GET /total-distance`, `GET /export/geojson`, `GET /export/kml`).
* **`RouteService`** (`@UseCase`)
   * Creation rules (IATA format, distinct airports, unique name, operational airports), updates, status transitions and history replacement, lookups, `Specification`-based search, sorting, and total distance.
* **`RouteExportService`** (`@UseCase`)
   * GeoJSON and KML generation (US228).
* **`RouteSearchService`**
   * Compatible-route filtering by aircraft capacity and range (not used by any endpoint).
* **`AirportRestClientService`** / **`AircraftRestClientService`** / **`FlightRestClientService`**
   * `RestClient` adapters for `airport-service` (used), `aircraft-service` (not injected), and `flight-service` (no methods), built from the shared `GlobalRestClientConfig` builder (2 s timeouts, `JwtTokenInterceptor`). Their `@Retryable` annotations are inactive (no `@EnableRetry`).
* **`PeerForwarder`**
   * Forwards a GET to other instances of a service; not used by any controller.
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

1. Inbound requests arrive through the API Gateway for `/api/routes/**`; the gateway validates the JWT and adds `X-Gateway-Secret`, which `gatewaySecretFilter` checks (`403` when missing).
2. Creation and update go from `RouteController` to `RouteService`, which validates the airports through `AirportRestClientService` and persists through `RouteRepository`.
3. Read and analytics flows use `RouteService` over `RouteRepository`.
4. Export flows use `RouteExportService`, which reads all routes and builds the GeoJSON / KML text.
5. `aircraft-service` and `airport-service` call the read endpoints of this service; `flight-service` reads a route and then patches its history when it schedules a flight.
6. Exceptions are transformed by `GlobalExceptionHandler` into `ErrorDetails` payloads.
