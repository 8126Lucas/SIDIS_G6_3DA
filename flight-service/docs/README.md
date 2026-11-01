# Flight Operations Service (`flight-service`) — Technical Documentation

This folder documents the behavior that is currently implemented in code for the `flight-service` microservice.

---

## 1. Scope and Responsibilities

The service is a Spring Boot microservice focused on flight scheduling and route-based reporting.

Current implemented responsibilities:

* Schedule a flight after synchronous checks against `aircraft-service`, `route-service` and `airport-service` (US212).
* List all flights and the flights of one aircraft (US213).
* Produce a route utilization report with the number of flights per route (US229).
* Increment the route usage counter in `route-service` each time a flight is scheduled.
* Reject requests that do not come through the API Gateway (`X-Gateway-Secret` filter).
* Return standardized API errors via `GlobalExceptionHandler`.

Important implementation notes:

* There is optimistic locking on `Flight` via `@Version`; conflicts map to `409`.
* Every request (except H2 console and Swagger paths) must carry `X-Gateway-Secret: sidis-g6-secret`, otherwise `403` (see `domain-and-resilience.md`, section 6).
* Flights are only created as `SCHEDULED`; there are no endpoints to start, complete or cancel a flight.
* The utilization report counts flights per route; it does not sum flight hours.
* Outgoing calls have 2-second timeouts. Several client methods are marked `@Retryable`, but `@EnableRetry` is missing, so they are not retried.
* There is no `maintenance-service` integration.

---

## 2. Runtime and Configuration

| Resource / Parameter | Current Value | Description |
| :--- | :--- | :--- |
| Service Port | `8111` | Configured in `application.properties` |
| API Prefix | `/api/flights` | Base mapping in `FlightController` |
| Database | `H2 In-Memory` | `jdbc:h2:mem:flighttestdb` |
| Schema Strategy | `update` | `spring.jpa.hibernate.ddl-auto` |
| H2 Console Path | `/api/flight/h2-console` | Spring H2 console path (singular `flight`) |
| Service Registry | `http://localhost:8761/eureka/` | Eureka client registration (`prefer-ip-address=true`); only the first discovery node |
| Gateway Secret | `sidis-g6-secret` | Hardcoded in `FlightSecurityConfig` and `JwtTokenInterceptor` |
| Outgoing Call Timeouts | `2000` ms connect / `2000` ms read | `GlobalRestClientConfig` |

External client base URLs configured in code (resolved through Eureka via the shared `@LoadBalanced` `RestClient.Builder`):

* `aircraft-service`: `http://aircraft-service`
* `route-service`: `http://route-service`
* `airport-service`: `http://airport-service`

All outgoing calls go through `JwtTokenInterceptor`, which adds `X-Gateway-Secret` and forwards the caller's `Authorization: Bearer` header.

API Gateway routing (`https://localhost:7979`):

* `/api/flights/**` is routed to `lb://flight-service` with the `JwtAuthentication` filter, which validates the JWT issued by `auth-service` and adds `X-User-Name` and `X-Gateway-Secret`.

---

## 3. Documentation Index

This `docs/` directory is organized as follows:

* `api-specification.md`: Real API contract implemented by the controller and services.
* `domain-and-resilience.md`: Domain model, scheduling rules, integrations, exception mapping, concurrency, and security notes.
* `architecture/architecture-c4-level3.md`: Component-level architecture aligned with current classes.
* `architecture/architecture-c4-level3.puml`: PlantUML source of the architecture diagram.
* `sequence-diagrams/README.md`: Dynamic behavior narrative for read and write flows.
* `sequence-diagrams/scenario-1-query-flights.puml`: Sequence for read/report flows.
* `sequence-diagrams/scenario-2-schedule-flight.puml`: Sequence for the scheduling flow.

---

## 4. Quick Start

Run locally:

```bash
./mvnw spring-boot:run
```

Call the API through the gateway (requires a token from `POST /api/auth/login` in `auth-service`):

```bash
curl -k -H "Authorization: Bearer <token>" https://localhost:7979/api/flights
```

Calling `http://localhost:8111/api/...` directly without `X-Gateway-Secret` returns `403` (`"Unauthorized Access! Please use the API Gateway!"`).

Open H2 console (exempt from the gateway secret filter):

* URL: `http://localhost:8111/api/flight/h2-console`
* JDBC URL: `jdbc:h2:mem:flighttestdb`
* User: `sa`
* Password: empty

There is no Swagger UI: `springdoc-openapi` is not a dependency of this service.
