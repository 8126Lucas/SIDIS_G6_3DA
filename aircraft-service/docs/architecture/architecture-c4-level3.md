# Aircraft Service (`aircraft-service`) — C4 Level 3 Component View (As Implemented)

This document describes the current internal component architecture at C4 Level 3 based on implemented classes.

---

## 1. Current Component Responsibilities

The `aircraft-service` currently exposes three controllers and the following main application services:

* **`AircraftSecurityConfig`**
   * Stateless `SecurityFilterChain` with `gatewaySecretFilter`, which rejects requests without `X-Gateway-Secret` (`403`).
* **`ModelCatalogController`** (`/api/catalog`)
   * `POST /model`
   * `PATCH /model/{modelName}`
   * `GET /rankings?mode=` (US204).
   * `GET /{modelName}`.
* **`HangarController`** (`/api/hangar`)
   * Aircraft CRUD-like operations under `/aircraft/**`.
   * Analytics under `/analytics/**`.
   * Feature search under `/aircraft-features`.
* **`HangarViewController`** (`/api/hangar/view`)
   * `GET /utilization` (Thymeleaf view, placeholder template).
* **`AddModelUseCase`** / **`AircraftModelUpdater`** / **`AircraftModelSearchService`**
   * Catalog persistence, updates, and lookup by `modelName`.
* **`AddAircraftUseCase`**
   * Aircraft persistence.
* **`AircraftSearchService`**
   * Lookup, `Specification`-based filtering, counts, feature filtering.
* **`AircraftLifeCycleUpdaterService`**
   * Availability transitions on the `Aircraft` aggregate.
* **`RouteSearchService`**
   * Compatible-route filtering by capacity and range.
* **`FlightSearchService`**
   * Flight counts per aircraft from `flight-service`, used by the model ranking.
* **`FuelEfficiencyService`**
   * Fuel metrics per aircraft and per route.
* **`CalculationsService`**
   * Monthly utilization data for the view.
* **`AircraftRepository`** / **`AircraftModelRepository`**
   * Spring Data JPA persistence.
* **`GlobalExceptionHandler`**
   * Cross-cutting HTTP error normalization (`ErrorDetails`).
* **`UseCaseLoggingAdvice`**
   * AOP logging of `@UseCase` classes (`@within` pointcut).

External integration clients:

* `RouteRestClientService` -> `route-service`
* `FlightRestClientService` -> `flight-service`
* `PeerForwarder` -> other `aircraft-service` instances (aircraft lookup on a local miss)
* All are built from the shared `GlobalRestClientConfig` builder (2 s timeouts, `JwtTokenInterceptor` for `X-Gateway-Secret` + `Authorization` propagation); client methods are `@Retryable` (3 attempts).

---

## 2. Architecture Diagram Source

PlantUML source file:

* `architecture-c4-level3.puml`

---

## 3. Data Flow and Dependencies

1. Inbound requests arrive through the API Gateway for `/api/hangar/**` and `/api/catalog/**`; the gateway validates the JWT and adds `X-Gateway-Secret`.
2. `gatewaySecretFilter` checks the secret header before any controller runs (`403` when missing).
3. Catalog flows go from `ModelCatalogController` to `AddModelUseCase` / `AircraftModelUpdater`, which persist through `AircraftModelRepository`.
4. Aircraft registration resolves the model through `AircraftModelSearchService`, applies the availability through `AircraftLifeCycleUpdaterService`, and persists through `AddAircraftUseCase`.
5. Read and analytics flows use `AircraftSearchService` over `AircraftRepository`; a single-aircraft lookup that misses locally is passed to `PeerForwarder`.
6. Compatible routes, route fuel efficiency, model rankings (`assignments`) and the utilization view call `route-service` and/or `flight-service` through `RestClient` adapters.
7. Exceptions are transformed by `GlobalExceptionHandler` into `ErrorDetails` payloads.
