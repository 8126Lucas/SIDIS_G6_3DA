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

* **CQRS (iteration 2).** Commands write to PostgreSQL (`maintenance_db`) and publish an event to RabbitMQ; a listener in the service projects it into the Redis cluster. Template reads, record searches, reports and alerts read from Redis; `GET /records/{id}` still reads PostgreSQL. Details: [`../../docs/data-architecture-cqrs.md`](../../docs/data-architecture-cqrs.md).
* **Known gaps of the CQRS version:** template lookups by id and the records listed by `GET /records` (each embeds its template) fail with `404`, and status-based reports return empty lists, because the Redis derived queries have no `@Indexed` field; seeded templates and records are only in PostgreSQL.
* Creating a template or a record now answers `201 Created` with an empty body.
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
| Service Port | `8121` (instance 1, PC2), `8122` (instance 2, PC3) | `server.port`, overridden by `SERVER_PORT` in `docker-compose.yml` |
| API Prefix | `/api/maintenance` | Base mapping in `MaintenanceRecordController` |
| Write database | PostgreSQL `jdbc:postgresql://${IP_PC5}:5432/maintenance_db` | User `sidis` |
| Schema Strategy | `update` | `spring.jpa.hibernate.ddl-auto` |
| Read database | Redis cluster `${SPRING_REDIS_CLUSTER_NODES}` | Default `localhost:6379,6380,6381`; in the lab `redis-maintenance-1..3` (`6382 - 6384`); hashes `MaintenanceRecord`, `MaintenanceTemplate` |
| Message broker | RabbitMQ `${IP_PC5}:5672,${IP_PC5}:5673,${IP_PC3}:5674` | Exchanges `maintenance.exchange`, `maintenancetemplate.exchange` |
| Metrics | `/actuator/prometheus`, `/actuator/health` | Scraped by Prometheus; exempt from the gateway secret filter |
| OpenAPI Docs | `/api/docs` | springdoc API docs path |
| Swagger UI | `/api/swagger-ui` | springdoc Swagger UI path |
| Service Registry | `${EUREKA_URL_1},${EUREKA_URL_2}` | Both discovery nodes; registers with `${IP_PC<n>}` as its address |
| Outgoing Call Timeouts | `2000` ms connect / `2000` ms read | `GlobalRestClientConfig` |
| Gateway Secret | `sidis-g6-secret` | Hardcoded in `MaintenanceSecurityConfig` and `JwtTokenInterceptor` |

External client base URLs configured in code (resolved through Eureka via `@LoadBalanced` `RestClient.Builder`):

* `aircraft-service`: `http://aircraft-service`

All outgoing calls go through `JwtTokenInterceptor`, which adds `X-Gateway-Secret` and forwards the caller's `Authorization: Bearer` header.

API Gateway routing (clients call HAProxy at `https://192.168.137.1`, which forwards to one of the two gateways):

* `/api/maintenance/**` is routed to `lb://maintenance-service` with the `JwtAuthentication` filter, which validates the JWT issued by `auth-service` and adds `X-User-Name` and `X-Gateway-Secret`.
* `/api/welcome` is not under `/api/maintenance`, so it is only reachable directly on the service port.

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
* [`../../docs/data-architecture-cqrs.md`](../../docs/data-architecture-cqrs.md): global description of the CQRS split, the RabbitMQ events and the Redis cluster.

---

## 4. Quick Start

The service needs PostgreSQL, a Redis cluster and RabbitMQ (see section 2). In the lab the instances run on PC2 and PC3:

```bash
docker compose --profile pc2 up -d    # maintenance-service-1 (8121)
docker compose --profile pc3 up -d    # maintenance-service-2 (8122)
```

Call the API through HAProxy (requires a token from `POST /api/auth/login`):

```bash
curl -k -H "Authorization: Bearer <token>" https://192.168.137.1/api/maintenance/templates
```

Calling `http://<IP_PC2>:8121/api/...` directly without `X-Gateway-Secret` returns `403` (`"Unauthorized Access! Please use the API Gateway!"`).

Inspect the read model: `redis-cli -c -p 6382 KEYS 'MaintenanceTemplate*'`. Inspect the queues: RabbitMQ management on `http://<IP_PC5>:15672` (`guest` / `guest`).

Open Swagger UI:

* URL: `http://<IP_PC2>:8121/api/swagger-ui`
* The UI page loads, but the OpenAPI document at `/api/docs` is not in the filter's exemption list (it only exempts paths containing `/api-docs`), so the spec request returns `403`.

The `.http` files under `src/test/java` still point to `http://localhost:8081/auth/login` and `http://localhost:8081/api/maintenance/...` (aircraft-service port, old login path); they do not match the current gateway or service URLs.
