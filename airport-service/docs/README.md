# Airport Service (`airport-service`) — Technical Documentation

This folder documents the behavior that is currently implemented in code for the `airport-service` microservice.

---

## 1. Scope and Responsibilities

The service is a Spring Boot microservice focused on the registry of airports used by the company network.

Current implemented responsibilities:

* Register airports with location, facilities, runways, certifications, contacts, and photos.
* Bulk import airports from a CSV file (OpenCSV).
* Add certifications to an existing airport.
* Query airports by IATA code and by filters (city, country, name).
* Change airport status and update airport details (name, operational hours, contacts).
* Group airports by country, city or region.
* List the routes departing from or arriving at an airport and rank airports by number of routes using `route-service` data.
* Reject requests that do not come through the API Gateway (`X-Gateway-Secret` filter).
* Return standardized API errors via `GlobalExceptionHandler`.

Important implementation notes:

* **CQRS (iteration 2).** Commands write to PostgreSQL (`airport_db`); after the transaction commits, a `AirportEventPublisher` sends a `AirportChangedEvent` (the whole read model, with its JPA `@Version`) to `airport.exchange`, and `AirportRedisProjector` stores it in Redis unless Redis already has the same or a newer version. Reads go through `AirportQueryService` and Redis. On every start, `Bootstrap` republishes every row, so Redis is rebuilt from PostgreSQL. Details: [`../../docs/data-architecture-cqrs.md`](../../docs/data-architecture-cqrs.md).
* There are no role checks in this service: any valid token reaches every endpoint.
* There is optimistic locking on `Airport` via `@Version`; conflicts map to `409`.
* A missing airport is reported as `DomainException` (`409 Conflict`), not `404`; `ResourceNotFoundException` exists but is not thrown by the service's own lookups.
* Only the certification request has a Bean Validation rule (expiry not before start); other invalid input fails in the domain, in enum parsing, or at persistence.
* Every request (except Actuator, H2 console and Swagger UI paths) must carry `X-Gateway-Secret: sidis-g6-secret`, otherwise `403` (see `domain-and-resilience.md`, section 6).
* Calls to `route-service` have 2-second timeouts. The client methods are marked `@Retryable`, but `@EnableRetry` is missing, so they are not retried.
* The `UseCaseLoggingAdvice` pointcut does not match `AirportService` (see `domain-and-resilience.md`, section 6).

---

## 2. Runtime and Configuration

| Resource / Parameter | Current Value | Description |
| :--- | :--- | :--- |
| Service Port | `8091` (instance 1, PC3), `8092` (instance 2, PC4) | `server.port`, overridden by `SERVER_PORT` in `docker-compose.yml` |
| API Prefix | `/api/airports` | Base mapping in `AirportController` |
| Write database | PostgreSQL `jdbc:postgresql://${IP_PC5}:5432/airport_db` | User `sidis`; tables created by Hibernate (`ddl-auto=update`) |
| Read database | Redis cluster `${SPRING_REDIS_CLUSTER_NODES}` | Default `localhost:6385,localhost:6386,localhost:6387`; in the lab `redis-airport-1..3`; hash `Airport` |
| Message broker | RabbitMQ `${SPRING_RABBITMQ_ADDRESSES}` | Default `localhost:5672`; exchange `airport.exchange` |
| Metrics | `/actuator/prometheus`, `/actuator/health` | Scraped by Prometheus; exempt from the gateway secret filter |
| Schema Strategy | `update` | `spring.jpa.hibernate.ddl-auto` |
| OpenAPI Docs | `/api/docs` | springdoc API docs path |
| Swagger UI | `/api/swagger-ui` | springdoc Swagger UI path |
| Service Registry | `${EUREKA_URL_1},${EUREKA_URL_2}` | Both discovery nodes (`prefer-ip-address=true`); registers with `${IP_PC<n>}` as its address |
| Gateway Secret | `sidis-g6-secret` | Hardcoded in `AirportSecurityConfig` and `JwtTokenInterceptor` |
| Outgoing Call Timeouts | `2000` ms connect / `2000` ms read | `GlobalRestClientConfig` |

External client base URLs configured in code (resolved through Eureka via `@LoadBalanced` `RestClient.Builder`):

* `route-service`: `http://route-service`

All outgoing calls go through `JwtTokenInterceptor`, which adds `X-Gateway-Secret` and forwards the caller's `Authorization: Bearer` header.

API Gateway routing (clients call HAProxy at `https://192.168.137.1`, which forwards to one of the two gateways):

* `/api/airports/**` is routed to `lb://airport-service` with the `JwtAuthentication` filter, which validates the JWT issued by `auth-service` and adds `X-User-Name` and `X-Gateway-Secret`.

---

## 3. Documentation Index

This `docs/` directory is organized as follows:

* `api-specification.md`: Real API contract implemented by the controller and services.
* `domain-and-resilience.md`: Domain model, lifecycle rules, integrations, exception mapping, and concurrency notes.
* `architecture/architecture-c4-level3.md`: Component-level architecture aligned with current classes.
* `architecture/architecture-c4-level3.puml`: PlantUML source of the architecture diagram.
* `sequence-diagrams/README.md`: Dynamic behavior narrative for read, write, and inter-service flows.
* `sequence-diagrams/scenario-1-query-airports.puml`: Sequence for read and grouping flows.
* `sequence-diagrams/scenario-2-register-airport.puml`: Sequence for airport registration, status change, and CSV import.
* `sequence-diagrams/scenario-3-route-analytics.puml`: Sequence for flows that depend on `route-service`.

---

## 4. Quick Start

Run locally:

```bash
./mvnw spring-boot:run
```

Call the API through the gateway (requires a token from `POST /api/auth/login` in `auth-service`):

```bash
curl -k -H "Authorization: Bearer <token>" https://192.168.137.1/api/airports/OPO
```

Calling `http://localhost:8091/api/...` directly without `X-Gateway-Secret` returns `403` (`"Unauthorized Access! Please use the API Gateway!"`).


Open Swagger UI:

* URL: `http://localhost:8091/api/swagger-ui`
* The OpenAPI document at `/api/docs` is not in the filter's exemption list (it only exempts paths containing `/api-docs`), so the spec request returns `403`.
