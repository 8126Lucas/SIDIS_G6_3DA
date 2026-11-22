# Flight Operations Service (`flight-service`) — C4 Level 3 Component View (As Implemented)

This document describes the current internal component architecture at C4 Level 3 based on implemented classes.

---

## 1. Current Component Responsibilities

The `flight-service` currently exposes one controller and the following main components:

* **`FlightSecurityConfig`**
   * Stateless `SecurityFilterChain` with `gatewaySecretFilter`, which rejects requests without `X-Gateway-Secret` (`403`).
* **`FlightController`** (`/api/flights`)
   * `GET /`
   * `POST /scheduled` (US212)
   * `GET /aircraft/{registrationNumber}` (US213)
   * `GET /reports/route-utilization` (US229)
* **`FlightService`** (`@UseCase`, renamed from `RouteService` in iteration 2) — command side
   * Scheduling orchestration and business-rule validation; route and airport lookups; route usage update; local overlap check; saves the `Flight` in PostgreSQL and raises a `FlightChangedEvent`.
* **`AircraftSearchService`**
   * Aircraft and model lookup in `aircraft-service`.
* **`FlightQueryService`** — query side (replaces `FlightSearchService` and `FlightUtilizationReportService`)
   * All flights, flights of an aircraft, flights of an aircraft in a time window, route utilization report (US229), all read from Redis.
* **`FlightRepository`** / **`FlightReadRepository`**
   * Spring Data JPA over PostgreSQL (`flight_db`) / Spring Data Redis over the flight Redis cluster (`Flight:<flightId>`, `aircraftRegistrationNumber` indexed).
* **`FlightReadModelMapper`** / **`FlightReadModel`**
   * Read model built from the entity, carrying the JPA `@Version`.
* **`FlightEventPublisher`** / **`FlightRedisProjector`** / **`RabbitMQConfig`**
   * After the transaction commits, the publisher sends the event to `flight.exchange` (`flight.event.updated`); the projector (`flight.sync.queue`, concurrency 1) stores it in Redis unless Redis already holds the same or a newer version.
* **`GlobalExceptionHandler`**
   * Cross-cutting HTTP error normalization (`ErrorDetails`).
* **`UseCaseLoggingAdvice`**
   * AOP logging aspect intended for `@UseCase` classes (its pointcut does not currently match them).
* **`Bootstrap`**
   * Seeds 6 flights.

External integration clients:

* `AircraftRestClientService` -> `aircraft-service`
* `RouteRestClientService` -> `route-service`
* `AirportRestClientService` -> `airport-service`
* `PeerForwarder` -> other instances of a service (not used)
* All are built from the shared `GlobalRestClientConfig` builder (2 s timeouts, `JwtTokenInterceptor` for `X-Gateway-Secret` + `Authorization` propagation). Their `@Retryable` annotations are inactive (no `@EnableRetry`).

---

## 2. Architecture Diagram Source

PlantUML source file:

* `architecture-c4-level3.puml`

---

## 3. Data Flow and Dependencies

1. Inbound requests arrive through the API Gateway for `/api/flights/**`; the gateway validates the JWT and adds `X-Gateway-Secret`, which `gatewaySecretFilter` checks (`403` when missing).
2. Scheduling first resolves the aircraft and its model through `AircraftSearchService`, then delegates to `FlightService`. The model specs (range, capacity) come from `aircraft-service`'s catalog (`GET /api/catalog/{modelName}`).
3. `FlightService` calls `route-service` and `airport-service`, validates the business rules, patches the route usage in `route-service`, saves the `Flight` in PostgreSQL, and publishes the change after commit; `FlightRedisProjector` writes it to Redis.
4. Read flows and the utilization report use `FlightQueryService` over `FlightReadRepository` (Redis). On every start, `Bootstrap` republishes every flight so Redis matches PostgreSQL.
5. `aircraft-service` calls `GET /api/flights` and `GET /api/flights/aircraft/{registration}`.
6. Exceptions are transformed by `GlobalExceptionHandler` into `ErrorDetails` payloads.
