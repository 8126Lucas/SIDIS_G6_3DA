# Maintenance Service (`maintenance-service`) — Technical Documentation

This folder documents the behavior that is currently implemented in code for the `maintenance-service` microservice.

---

## 1. Scope and Responsibilities

The service is a Spring Boot microservice focused on aircraft maintenance templates, maintenance records, and maintenance reporting.

Current implemented responsibilities:

* Register maintenance templates (type, component attribute, expected duration, checklist) (US115).
* Register maintenance records for an aircraft from a template, after checking the aircraft in `aircraft-service` (US116).
* Query templates (US117) and records, move a record to `ONGOING` and complete it (US119).
* Change the component attribute of a record (US217) and search records by aircraft, attribute and date range (US218).
* Report ongoing maintenance (US219), maintenance costs per aircraft or model (US220), and average turnaround time per aircraft model (US221).
* Raise maintenance alerts by calendar days or flight hours (US222).
* Summarize used parts and raise low-stock alerts (US226).
* Reject requests that do not come through the API Gateway (`X-Gateway-Secret` filter).
* Return standardized API errors via `GlobalExceptionHandler`.

Important implementation notes:

* There is optimistic locking on `MaintenanceRecord` via `@Version` (not on `MaintenanceTemplate`); conflicts map to `409`.
* Records are completed through `PATCH /api/maintenance/records/{id}/complete`; `updateProgress(...)` (duration and used parts) is still not exposed.
* `DomainException` now maps to `404 Not Found`, so every business-rule rejection (for example an invalid status transition) is also reported as `404`.
* `GET /api/maintenance/records` (US218) only works when the `aircraft` filter is given; without it the search service dereferences `null` and returns `500` (see `api-specification.md`, section 3.10).
* Outgoing calls have 2-second timeouts and up to 3 attempts (`@Retryable`, enabled with `@EnableRetry`).
* A template that is not found locally is looked up in other instances through `PeerForwarder`, but the service id passed is `aircraft-service` (see `domain-and-resilience.md`, section 6).

---

## 2. Runtime and Configuration

| Resource / Parameter | Current Value | Description |
| :--- | :--- | :--- |
| Service Port | `8121` | Configured in `application.properties` |
| API Prefix | `/api/maintenance` | Base mapping in `MaintenanceRecordController` |
| Database | `H2 In-Memory` | `jdbc:h2:mem:testdb` |
| Schema Strategy | `update` | `spring.jpa.hibernate.ddl-auto` |
| H2 Console Path | `/h2-console` | Spring H2 console path |
| OpenAPI Docs | `/api/docs` | springdoc API docs path |
| Swagger UI | `/api/swagger-ui` | springdoc Swagger UI path |
| Service Registry | `http://localhost:8761/eureka/,http://localhost:8762/eureka/` | Eureka client registration, both discovery nodes (`prefer-ip-address=true`) |
| Outgoing Call Timeouts | `2000` ms connect / `2000` ms read | `GlobalRestClientConfig` |
| Gateway Secret | `sidis-g6-secret` | Hardcoded in `MaintenanceSecurityConfig` and `JwtTokenInterceptor` |

External client base URLs configured in code (resolved through Eureka via `@LoadBalanced` `RestClient.Builder`):

* `aircraft-service`: `http://aircraft-service`

All outgoing calls go through `JwtTokenInterceptor`, which adds `X-Gateway-Secret` and forwards the caller's `Authorization: Bearer` header.

API Gateway routing (`https://localhost:7979`):

* `/api/maintenance/**` is routed to `lb://maintenance-service` with the `JwtAuthentication` filter, which validates the JWT issued by `auth-service` and adds `X-User-Name` and `X-Gateway-Secret`.
* `/api/welcome` and `/h2-console` are not under `/api/maintenance`, so they are only reachable directly on the service port.

---

## 3. Documentation Index

This `docs/` directory is organized as follows:

* `api-specification.md`: Real API contract implemented by the controller and services.
* `domain-and-resilience.md`: Domain model, lifecycle rules, integrations, exception mapping, concurrency, and security notes.
* `architecture/architecture-c4-level3.md`: Component-level architecture aligned with current classes.
* `architecture/architecture-c4-level3.puml`: PlantUML source of the architecture diagram.
* `sequence-diagrams/README.md`: Dynamic behavior narrative for read, write, and inter-service flows.
* `sequence-diagrams/scenario-1-query-maintenance.puml`: Sequence for template and record queries.
* `sequence-diagrams/scenario-2-register-maintenance.puml`: Sequence for template and record registration and status change.
* `sequence-diagrams/scenario-3-maintenance-reports.puml`: Sequence for reports and alerts that depend on `aircraft-service`.

---

## 4. Quick Start

Run locally:

```bash
./mvnw spring-boot:run
```

Call the API through the gateway (requires a token from `POST /api/auth/login` in `auth-service`):

```bash
curl -k -H "Authorization: Bearer <token>" https://localhost:7979/api/maintenance/templates
```

Calling `http://localhost:8121/api/...` directly without `X-Gateway-Secret` returns `403` (`"Unauthorized Access! Please use the API Gateway!"`).

Open H2 console (exempt from the gateway secret filter):

* URL: `http://localhost:8121/h2-console`
* JDBC URL: `jdbc:h2:mem:testdb`
* User: `sa`
* Password: empty

Open Swagger UI:

* URL: `http://localhost:8121/api/swagger-ui`
* The UI page loads, but the OpenAPI document at `/api/docs` is not in the filter's exemption list (it only exempts paths containing `/api-docs`), so the spec request returns `403`.

The `.http` files under `src/test/java` still point to `http://localhost:8081/auth/login` and `http://localhost:8081/api/maintenance/...` (aircraft-service port, old login path); they do not match the current gateway or service URLs.
