# Airport Service (`airport-service`) — C4 Level 3 Component View (As Implemented)

This document describes the current internal component architecture at C4 Level 3 based on implemented classes.

---

## 1. Current Component Responsibilities

The `airport-service` currently exposes one controller and the following main application services:

* **`AirportSecurityConfig`**
   * Stateless `SecurityFilterChain` with `gatewaySecretFilter`, which rejects requests without `X-Gateway-Secret` (`403`).
* **`AirportController`** (`/api/airports`)
   * Registration (`POST /`), CSV import (`POST /import`), certifications (`POST /{iataCode}/certifications`).
   * Queries (`GET /{iataCode}`, `GET /`, `GET /grouped`).
   * Updates (`PATCH /{iataCode}/status`, `PATCH /{iataCode}/details`).
   * Route analytics (`GET /{iataCode}/routes`, `GET /busiest`).
* **`AirportService`** (`@UseCase`)
   * Registration rules (IATA format, uniqueness), lookup, `Specification`-based search, status and detail updates, certifications, grouping, and route analytics.
* **`AirportCsvService`**
   * Parses CSV rows (OpenCSV) into `Airport` aggregates; persistence is delegated back to `AirportService`.
* **`RouteRestClientService`**
   * `RestClient` adapter for `route-service` (`@Retryable` annotations, inactive without `@EnableRetry`).
* **`PeerForwarder`**
   * Forwards a GET to other instances of a service; not used by any controller.
* **`AirportRepository`**
   * Spring Data JPA persistence with `JpaSpecificationExecutor`.
* **`GlobalExceptionHandler`**
   * Cross-cutting HTTP error normalization (`ErrorDetails`).
* **`UseCaseLoggingAdvice`**
   * AOP logging aspect intended for `@UseCase` classes (its pointcut does not currently match `AirportService`).

External integration clients:

* `RouteRestClientService` -> `route-service`
* Built from the shared `GlobalRestClientConfig` builder (2 s timeouts, `JwtTokenInterceptor` for `X-Gateway-Secret` + `Authorization` propagation).

---


## 2. Architecture Diagram Source

PlantUML source file:

* `architecture-c4-level3.puml`

---

## 3. Data Flow and Dependencies

1. Inbound requests arrive through the API Gateway for `/api/airports/**`; the gateway validates the JWT and adds `X-Gateway-Secret`, which `gatewaySecretFilter` checks (`403` when missing).
2. All flows go from `AirportController` to `AirportService`, which persists through `AirportRepository`.
3. The CSV import goes through `AirportCsvService` to build the aggregates, then calls `AirportService.execute(...)` once per airport.
4. Airport routes and busiest airports call `route-service` through `RouteRestClientService`.
5. `flight-service` and `route-service` call `GET /api/airports/{iataCode}` to validate airports.
6. Exceptions are transformed by `GlobalExceptionHandler` into `ErrorDetails` payloads.
