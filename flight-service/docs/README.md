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

* **CQRS (iteration 2).** Commands write to PostgreSQL (`flight_db`); after the transaction commits, a `FlightEventPublisher` sends a `FlightChangedEvent` (the whole read model, with its JPA `@Version`) to `flight.exchange`, and `FlightRedisProjector` stores it in Redis unless Redis already has the same or a newer version. Reads go through `FlightQueryService` and Redis. On every start, `Bootstrap` republishes every row, so Redis is rebuilt from PostgreSQL. Details: [`../../docs/data-architecture-cqrs.md`](../../docs/data-architecture-cqrs.md).
* **Roles are declared but not enforced.** The endpoints have `@PreAuthorize` roles (section 2.1), but the service has no `JwtAuthenticationFilter` and no `@EnableMethodSecurity`, and its chain is `permitAll()`, so any request that passes the gateway reaches every endpoint. `security.jwt.secret` is set in `application.properties` but not read.
* Downstream `4xx` answers from `route-service` (lookup and usage `PATCH`) are now turned into `DomainException` → `409`.
* There is optimistic locking on `Flight` via `@Version`; conflicts map to `409`.
* Every request (except Actuator, H2 console and Swagger paths) must carry `X-Gateway-Secret: sidis-g6-secret`, otherwise `403` (see `domain-and-resilience.md`, section 6).
* Flights are only created as `SCHEDULED`; there are no endpoints to start, complete or cancel a flight.
* The utilization report counts flights per route; it does not sum flight hours.
* Outgoing calls have 2-second timeouts. Several client methods are marked `@Retryable`, but `@EnableRetry` is missing, so they are not retried.
* There is no `maintenance-service` integration.

---

## 2. Runtime and Configuration

| Resource / Parameter | Current Value | Description |
| :--- | :--- | :--- |
| Service Port | `8111` (instance 1, PC4), `8112` (instance 2, PC5) | `server.port`, overridden by `SERVER_PORT` in `docker-compose.yml` |
| API Prefix | `/api/flights` | Base mapping in `FlightController` |
| Write database | PostgreSQL `jdbc:postgresql://${IP_PC5}:5432/flight_db` | User `sidis`; tables created by Hibernate (`ddl-auto=update`) |
| Read database | Redis cluster `${SPRING_REDIS_CLUSTER_NODES}` (profile `docker`) | Default `localhost:6388,localhost:6389,localhost:6390`; in the lab `redis-flight-1..3`; hash `Flight`. Without the `docker` profile: standalone Redis on `localhost:6379` (`6388` with the `test` profile) |
| Message broker | RabbitMQ `${IP_PC5}:5672,${IP_PC5}:5673,${IP_PC3}:5674` | Overridden by `SPRING_RABBITMQ_ADDRESSES` in `docker-compose.yml`; exchange `flight.exchange` |
| Profiles | `docker` (set in `docker-compose.yml`), `test` (CI) | `application-docker.properties`: Redis cluster. `application-test.properties`: PostgreSQL, Eureka, RabbitMQ and Redis on `localhost` |
| Metrics | `/actuator/prometheus`, `/actuator/health` | Scraped by Prometheus; exempt from the gateway secret filter |
| Schema Strategy | `update` | `spring.jpa.hibernate.ddl-auto` |
| Service Registry | `${EUREKA_URL_1},${EUREKA_URL_2}` | Both discovery nodes (`prefer-ip-address=true`); registers with `${IP_PC<n>}` as its address |
| Gateway Secret | `sidis-g6-secret` | Hardcoded in `FlightSecurityConfig` and `JwtTokenInterceptor` |
| Outgoing Call Timeouts | `2000` ms connect / `2000` ms read | `GlobalRestClientConfig` |

### 2.1. Roles per Endpoint (declared, not enforced)

| Endpoint | `@PreAuthorize` role | User story (comment in code) |
| :--- | :--- | :--- |
| `POST /api/flights/scheduled` | `ATCC` | US212 |
| `GET /api/flights/aircraft/{registrationNumber}` | `ATCC` | US213 |
| `GET /api/flights/reports/route-utilization` | `BACKOFFICE` | US229 |
| `GET /api/flights` | none | — |

`aircraft-service` calls `GET /api/flights` and `GET /api/flights/aircraft/{reg}` with the caller's token; if these roles were enforced, its `BACKOFFICE` analytics would be refused on the second call.

External client base URLs configured in code (resolved through Eureka via the shared `@LoadBalanced` `RestClient.Builder`):

* `aircraft-service`: `http://aircraft-service`
* `route-service`: `http://route-service`
* `airport-service`: `http://airport-service`

All outgoing calls go through `JwtTokenInterceptor`, which adds `X-Gateway-Secret` and forwards the caller's `Authorization: Bearer` header.

API Gateway routing (clients call HAProxy at `https://192.168.137.1`, which forwards to one of the two gateways):

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
curl -k -H "Authorization: Bearer <token>" https://192.168.137.1/api/flights
```

Calling `http://localhost:8111/api/...` directly without `X-Gateway-Secret` returns `403` (`"Unauthorized Access! Please use the API Gateway!"`).


There is no Swagger UI: `springdoc-openapi` is not a dependency of this service. `application.properties` now sets `springdoc.api-docs.path=/api/docs` and `springdoc.swagger-ui.path=/api/swagger-ui`, but without the dependency these properties have no effect.
