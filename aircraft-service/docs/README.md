# Aircraft Service (`aircraft-service`) — Technical Documentation

This folder documents the behavior that is currently implemented in code for the `aircraft-service` microservice.

---

## 1. Scope and Responsibilities

The service is a Spring Boot microservice focused on the aircraft model catalog and the company fleet (hangar).

Current implemented responsibilities:

* Register and update aircraft models in the catalog (US101, US201, US202).
* Register aircraft instances in the hangar and change their availability (US102, US105).
* Query aircraft by registration and by filters (model, status, manufacturing date) (US103, US104).
* Find routes compatible with an aircraft using `route-service` data (US203).
* Rank the top 5 aircraft models by flight assignments or flight hours (US204).
* Provide fleet analytics: availability, status summary, operational hours (US205, US206).
* Provide fuel efficiency metrics per aircraft and per route (US227).
* Reject requests that do not come through the API Gateway (`X-Gateway-Secret` filter).
* Return standardized API errors via `GlobalExceptionHandler`.

Important implementation notes:

* There is optimistic locking on `Aircraft` via `@Version` (not on `AircraftModel`); conflicts now map to `409`.
* Every request (except H2 console and Swagger UI paths) must carry `X-Gateway-Secret: sidis-g6-secret`, otherwise `403`. JWT validation happens only in the API Gateway (see `domain-and-resilience.md`, section 7).
* The utilization view (`/api/hangar/view/utilization`) now has a placeholder `utilization.html` template that does not render the computed data.
* Some calls to other services target endpoints or fields that are not exposed by those services (see `domain-and-resilience.md`, section 4).

---

## 2. Runtime and Configuration

| Resource / Parameter | Current Value | Description |
| :--- | :--- | :--- |
| Service Port | `8081` | Configured in `application.properties` |
| API Prefixes | `/api/hangar`, `/api/catalog` | Base mappings in `HangarController` and `ModelCatalogController` |
| Database | `H2 In-Memory` | `jdbc:h2:mem:aircrafttestdb` |
| H2 Console Path | `/api/hangar/h2-console` | Spring H2 console path |
| OpenAPI Docs | `/api/docs` | springdoc API docs path |
| Swagger UI | `/api/swagger-ui` | springdoc Swagger UI path |
| Service Registry | `http://localhost:8761/eureka/` | Eureka client registration |
| Gateway Secret | `sidis-g6-secret` | Hardcoded in `AircraftSecurityConfig` and `JwtTokenInterceptor` |

External client base URLs configured in code (resolved through Eureka via `@LoadBalanced` `RestClient.Builder`):

* `flight-service`: `http://flight-service`
* `route-service`: `http://route-service`

All outgoing calls go through `JwtTokenInterceptor`, which adds `X-Gateway-Secret` and forwards the caller's `Authorization: Bearer` header.

API Gateway routing (`https://localhost:7979`):

* `/api/hangar/**` and `/api/catalog/**` are routed to `aircraft-service` with the `JwtAuthentication` filter, which validates the JWT issued by `auth-service` and adds `X-User-Name` and `X-Gateway-Secret`.

---

## 3. Documentation Index

This `docs/` directory is organized as follows:

* `api-specification.md`: Real API contract implemented by controllers and services.
* `domain-and-resilience.md`: Domain model, lifecycle rules, integrations, exception mapping, concurrency, and security notes.
* `architecture/architecture-c4-level3.md`: Component-level architecture aligned with current classes.
* `architecture/architecture-c4-level3.puml`: PlantUML source of the architecture diagram.
* `sequence-diagrams/README.md`: Dynamic behavior narrative for read, write, and inter-service flows.
* `sequence-diagrams/scenario-1-query-aircraft.puml`: Sequence for read/analytics flows.
* `sequence-diagrams/scenario-2-register-aircraft.puml`: Sequence for aircraft registration and availability change.
* `sequence-diagrams/scenario-3-route-analytics.puml`: Sequence for flows that depend on `route-service` and `flight-service`.

---

## 4. Quick Start

Run locally:

```bash
./mvnw spring-boot:run
```

Call the API through the gateway (requires a token from `POST /api/auth/login` in `auth-service`):

```bash
curl -k -H "Authorization: Bearer <token>" https://localhost:7979/api/hangar/aircraft/D-ABXA
```

Calling `http://localhost:8081/api/...` directly without `X-Gateway-Secret` returns `403` (`"Unauthorized Access! Please use the API Gateway!"`).

Open H2 console (exempt from the gateway secret filter):

* URL: `http://localhost:8081/api/hangar/h2-console`
* JDBC URL: `jdbc:h2:mem:aircrafttestdb`
* User: `sa`
* Password: empty

Open Swagger UI:

* URL: `http://localhost:8081/api/swagger-ui`
* The UI page loads, but the OpenAPI document at `/api/docs` is not in the filter's exemption list (it only exempts paths containing `/api-docs`), so the spec request returns `403`.
