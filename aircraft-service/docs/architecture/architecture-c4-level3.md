# Aircraft Service (`aircraft-service`) — C4 Level 3 Component View (As Implemented)

This document describes the current internal component architecture at C4 Level 3 based on implemented classes.

---

## 1. Current Component Responsibilities

The `aircraft-service` currently exposes three controllers and the following main application services:

* **`ModelCatalogController`** (`/api/catalog`)
   * `POST /model`
   * `PATCH /model/{modelName}`
* **`HangarController`** (`/api/hangar`)
   * Aircraft CRUD-like operations under `/aircraft/**`.
   * Analytics under `/analytics/**`.
   * Feature search under `/aicraft-features`.
* **`HangarViewController`** (`/api/hangar/view`)
   * `GET /utilization` (Thymeleaf view).
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
* **`FuelEfficiencyService`**
   * Fuel metrics per aircraft and per route.
* **`CalculationsService`**
   * Monthly utilization data for the view.
* **`AircraftRepository`** / **`AircraftModelRepository`**
   * Spring Data JPA persistence.
* **`GlobalExceptionHandler`**
   * Cross-cutting HTTP error normalization (`ErrorDetails`).
* **`UseCaseLoggingAdvice`**
   * AOP logging for `@UseCase` classes.

External integration clients:

* `RouteRestClientService` -> `route-service`
* `FlightRestClientService` -> `flight-service`

Not present in current implementation:

* Model ranking endpoint (US204).
* Security configuration (`SecurityFilterChain`) despite the security dependency.

---

## 2. Architecture Diagram Source

PlantUML source file:

* `architecture-c4-level3.puml`

---

## 3. Data Flow and Dependencies

1. Inbound requests arrive through the API Gateway for `/api/hangar/**`; `/api/catalog/**` is reached directly on the service port.
2. Catalog flows go from `ModelCatalogController` to `AddModelUseCase` / `AircraftModelUpdater`, which persist through `AircraftModelRepository`.
3. Aircraft registration resolves the model through `AircraftModelSearchService`, applies the availability through `AircraftLifeCycleUpdaterService`, and persists through `AddAircraftUseCase`.
4. Read and analytics flows use `AircraftSearchService` over `AircraftRepository`.
5. Compatible routes and route fuel efficiency call `route-service` (and `flight-service` for fuel) through `RestClient` adapters.
6. Exceptions are transformed by `GlobalExceptionHandler` into `ErrorDetails` payloads.
