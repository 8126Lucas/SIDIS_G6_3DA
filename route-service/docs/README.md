# Route Service (`route-service`) — Technical Documentation

This folder documents the behavior that is currently implemented in code for the `route-service` microservice.

---

## 1. Scope and Responsibilities

The service is a Spring Boot microservice focused on the company route network (origin/destination airport pairs).

Current implemented responsibilities:

* Create routes between two operational airports validated through `airport-service`.
* Update a route's destination, estimated flight time, status, and history (used by `flight-service` to count route usage).
* Query routes by id, by origin airport, and by origin/destination filters.
* List active routes sorted by popularity or distance, and find routes for the same origin/destination pair.
* Calculate the total distance of the route network.
* Export the route network as GeoJSON and KML (US228).
* Reject requests that do not come through the API Gateway (`X-Gateway-Secret` filter).
* Return standardized API errors via `GlobalExceptionHandler`.

Important implementation notes:

* **CQRS (iteration 2).** Commands write to PostgreSQL (`route_db`); after the transaction commits, a `RouteEventPublisher` sends a `RouteChangedEvent` (the whole read model, with its JPA `@Version`) to `route.exchange`, and `RouteRedisProjector` stores it in Redis unless Redis already has the same or a newer version. Reads go through `RouteQueryService` and Redis. On every start, `Bootstrap` republishes every row, so Redis is rebuilt from PostgreSQL. Details: [`../../docs/data-architecture-cqrs.md`](../../docs/data-architecture-cqrs.md).
* There are no role checks in this service: any valid token reaches every endpoint.
* There is optimistic locking on `Route` via `@Version`; conflicts map to `409`.
* Routes created through the API always get `distanceKm = 1.0` (hardcoded in the `Route` constructor).
* A missing route is reported as `DomainException` (`409 Conflict`), not `404`.
* `AircraftRestClientService`, `FlightRestClientService` and `PeerForwarder` exist but are not used by any endpoint.
* Every request (except Actuator, H2 console and Swagger paths) must carry `X-Gateway-Secret: sidis-g6-secret`, otherwise `403` (see `domain-and-resilience.md`, section 6).
* Calls to `airport-service` have 2-second timeouts. The client methods are marked `@Retryable`, but `@EnableRetry` is missing, so they are not retried.

---

## 2. Runtime and Configuration

| Resource / Parameter | Current Value | Description |
| :--- | :--- | :--- |
| Service Port | `8131` (instance 1, PC4), `8132` (instance 2, PC5) | `server.port`, overridden by `SERVER_PORT` in `docker-compose.yml` |
| API Prefix | `/api/routes` | Base mapping in `RouteController` |
| Write database | PostgreSQL `jdbc:postgresql://${IP_PC5}:5432/route_db` | User `sidis`; tables created by Hibernate (`ddl-auto=update`) |
| Read database | Redis cluster `${SPRING_REDIS_CLUSTER_NODES}` | Default `localhost:6391,localhost:6392,localhost:6393`; in the lab `redis-route-1..3`; hash `Route` |
| Message broker | RabbitMQ `${SPRING_RABBITMQ_ADDRESSES}` | Default `localhost:5672`; exchange `route.exchange` |
| Metrics | `/actuator/prometheus`, `/actuator/health` | Scraped by Prometheus; exempt from the gateway secret filter |
| Schema Strategy | `update` | `spring.jpa.hibernate.ddl-auto` (SQL logging enabled) |
| Service Registry | `${EUREKA_URL_1},${EUREKA_URL_2}` | Both discovery nodes (`prefer-ip-address=true`); registers with `${IP_PC<n>}` as its address |
| Gateway Secret | `sidis-g6-secret` | Hardcoded in `RouteSecurityConfig` and `JwtTokenInterceptor` |
| Outgoing Call Timeouts | `2000` ms connect / `2000` ms read | `GlobalRestClientConfig` |

External client base URLs configured in code (resolved through Eureka via the shared `@LoadBalanced` `RestClient.Builder`):

* `airport-service`: `http://airport-service`
* `aircraft-service`: `http://aircraft-service` (client not used)
* `flight-service`: `http://flight-service` (client has no methods)

All outgoing calls go through `JwtTokenInterceptor`, which adds `X-Gateway-Secret` and forwards the caller's `Authorization: Bearer` header.

API Gateway routing (clients call HAProxy at `https://192.168.137.1`, which forwards to one of the two gateways):

* `/api/routes/**` is routed to `lb://route-service` with the `JwtAuthentication` filter, which validates the JWT issued by `auth-service` and adds `X-User-Name` and `X-Gateway-Secret`.

---

## 3. Documentation Index

This `docs/` directory is organized as follows:

* `api-specification.md`: Real API contract implemented by the controller and services.
* `domain-and-resilience.md`: Domain model, lifecycle rules, integrations, exception mapping, and concurrency notes.
* `architecture/architecture-c4-level3.md`: Component-level architecture aligned with current classes.
* `architecture/architecture-c4-level3.puml`: PlantUML source of the architecture diagram.
* `sequence-diagrams/README.md`: Dynamic behavior narrative for read, write, and export flows.
* `sequence-diagrams/scenario-1-query-routes.puml`: Sequence for read flows.
* `sequence-diagrams/scenario-2-create-update-route.puml`: Sequence for route creation and update (depends on `airport-service`).
* `sequence-diagrams/scenario-3-export-network.puml`: Sequence for GeoJSON / KML export and total distance.

---

## 4. Quick Start

Run locally:

```bash
./mvnw spring-boot:run
```

Call the API through the gateway (requires a token from `POST /api/auth/login` in `auth-service`):

```bash
curl -k -H "Authorization: Bearer <token>" https://192.168.137.1/api/routes/active
```

Calling `http://localhost:8131/api/...` directly without `X-Gateway-Secret` returns `403` (`"Unauthorized Access! Please use the API Gateway!"`).


There is no Swagger UI: `springdoc-openapi` is not a dependency of this service.
