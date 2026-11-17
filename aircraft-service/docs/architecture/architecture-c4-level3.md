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
* **Command side** (writes to PostgreSQL, then publishes an event):
   * **`AddModelUseCase`** / **`AircraftModelUpdater`**: create and update models (`aircraftmodel.event.created` / `.updated`).
   * **`AddAircraftUseCase`**: creates an aircraft after loading its model from PostgreSQL (`aircraft.event.created`).
   * **`AircraftLifeCycleUpdaterService`**: availability transitions on the `Aircraft` aggregate (`aircraft.event.updated`).
* **Query side** (reads only from Redis):
   * **`AircraftModelSearchService`**: model lookup by `modelName`.
   * **`AircraftSearchService`**: lookup, filtering, counts, feature filtering over `AircraftRedis`.
* **`RouteSearchService`**
   * Compatible-route filtering by capacity and range.
* **`FlightSearchService`**
   * Flight counts per aircraft from `flight-service`, used by the model ranking.
* **`FuelEfficiencyService`**
   * Fuel metrics per aircraft and per route.
* **`CalculationsService`**
   * Monthly utilization data for the view.
* **`AircraftCommandRepository`** / **`AircraftModelCommandRepository`**
   * Spring Data JPA over PostgreSQL (`aircraft_db`): the write model and source of truth.
* **`AircraftQueryRepository`** / **`AircraftModelQueryRepository`**
   * Spring Data Redis over the Redis cluster: hashes `Aircraft:<registration>` and `AircraftModel:<modelName>` (read models `AircraftRedis`, `AircraftModelRedis`).
* **`RabbitMQConfig`**
   * Declares the topic exchanges `aircraft.exchange` and `aircraftmodel.exchange`, the durable queues `aircraft.sync.queue` and `aircraftmodel.sync.queue`, and the JSON message converter.
* **`AircraftRedisProjector`** / **`AircraftModelRedisProjector`**
   * `@RabbitListener`s that turn each sync event into the Redis hash. Both instances listen to the same queue, so each event is projected once.
* **`RedisConfig`**
   * `RedisTemplate` with String keys and JSON values.
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
3. Catalog commands go from `ModelCatalogController` to `AddModelUseCase` / `AircraftModelUpdater`, which save through `AircraftModelCommandRepository` and publish to `aircraftmodel.exchange`.
4. Aircraft registration calls `AircraftLifeCycleUpdaterService` (availability) and then `AddAircraftUseCase` (creation), which publishes to `aircraft.exchange`. Because the availability change runs first, on an aircraft that does not exist yet, the request currently ends in `404`.
5. The projectors receive the events from the sync queues and save the read models in the Redis cluster.
6. Read and analytics flows use `AircraftSearchService` / `AircraftModelSearchService` over the query repositories; a single-aircraft lookup that misses in Redis is passed to `PeerForwarder`.
7. `Bootstrap` seeds models and aircraft only through the command repositories, so the seed data is not in Redis.
8. Compatible routes, route fuel efficiency, model rankings (`assignments`) and the utilization view call `route-service` and/or `flight-service` through `RestClient` adapters.
9. Exceptions are transformed by `GlobalExceptionHandler` into `ErrorDetails` payloads.
