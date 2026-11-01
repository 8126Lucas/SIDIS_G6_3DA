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

* There is optimistic locking on `Route` via `@Version`; conflicts map to `409`.
* Routes created through the API always get `distanceKm = 1.0` (hardcoded in the `Route` constructor).
* A missing route is reported as `DomainException` (`409 Conflict`), not `404`.
* `RouteSearchService`, `AircraftRestClientService`, `FlightRestClientService` and `PeerForwarder` exist but are not used by any endpoint.
* Every request (except H2 console and Swagger paths) must carry `X-Gateway-Secret: sidis-g6-secret`, otherwise `403` (see `domain-and-resilience.md`, section 6).
* Calls to `airport-service` have 2-second timeouts. The client methods are marked `@Retryable`, but `@EnableRetry` is missing, so they are not retried.

---

## 2. Runtime and Configuration

| Resource / Parameter | Current Value | Description |
| :--- | :--- | :--- |
| Service Port | `8131` | Configured in `application.properties` |
| API Prefix | `/api/routes` | Base mapping in `RouteController` |
| Database | `H2 In-Memory` | `jdbc:h2:mem:routetestdb` |
| Schema Strategy | `update` | `spring.jpa.hibernate.ddl-auto` (SQL logging enabled) |
| H2 Console Path | `/api/routes/h2-console` | Spring H2 console path |
| Service Registry | `http://localhost:8761/eureka/` | Eureka client registration (`prefer-ip-address=true`); only the first discovery node |
| Gateway Secret | `sidis-g6-secret` | Hardcoded in `RouteSecurityConfig` and `JwtTokenInterceptor` |
| Outgoing Call Timeouts | `2000` ms connect / `2000` ms read | `GlobalRestClientConfig` |

External client base URLs configured in code (resolved through Eureka via the shared `@LoadBalanced` `RestClient.Builder`):

* `airport-service`: `http://airport-service`
* `aircraft-service`: `http://aircraft-service` (client not used)
* `flight-service`: `http://flight-service` (client has no methods)

All outgoing calls go through `JwtTokenInterceptor`, which adds `X-Gateway-Secret` and forwards the caller's `Authorization: Bearer` header.

API Gateway routing (`https://localhost:7979`):

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
curl -k -H "Authorization: Bearer <token>" https://localhost:7979/api/routes/active
```

Calling `http://localhost:8131/api/...` directly without `X-Gateway-Secret` returns `403` (`"Unauthorized Access! Please use the API Gateway!"`).

Open H2 console (exempt from the gateway secret filter):

* URL: `http://localhost:8131/api/routes/h2-console`
* JDBC URL: `jdbc:h2:mem:routetestdb`
* User: `sa`
* Password: empty

There is no Swagger UI: `springdoc-openapi` is not a dependency of this service.
