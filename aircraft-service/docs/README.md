# Aircraft Service (`aircraft-service`) — Technical Documentation

This folder documents the behavior that is currently implemented in code for the `aircraft-service` microservice.

---

## 1. Scope and Responsibilities

The service is a Spring Boot microservice focused on the aircraft model catalog and the company fleet (hangar).

Current implemented responsibilities:

* Register, update and look up aircraft models in the catalog (US101, US201, US202).
* Register aircraft instances in the hangar and change their availability (US102, US105).
* Query aircraft by registration and by filters (model, status, manufacturing date) (US103, US104).
* Find routes compatible with an aircraft using `route-service` data (US203).
* Rank the top 5 aircraft models by flight assignments or flight hours (US204).
* Provide fleet analytics: availability, status summary, operational hours (US205, US206).
* Provide fuel efficiency metrics per aircraft and per route (US227).
* Reject requests that do not come through the API Gateway (`X-Gateway-Secret` filter).
* Return standardized API errors via `GlobalExceptionHandler`.

Important implementation notes:

* **CQRS (iteration 2).** Commands write to PostgreSQL (`aircraft_db`) and publish an event to RabbitMQ; a listener in the service projects it into the Redis cluster, and every read and analytics endpoint reads from Redis. Details: [`../../docs/data-architecture-cqrs.md`](../../docs/data-architecture-cqrs.md).
* `Bootstrap` seeds PostgreSQL when the tables are empty and then, on **every** start, republishes one event per model and aircraft, so Redis is rebuilt from PostgreSQL. The read models have `@Indexed` fields (`modelName`, `status`, ...), so lookups such as `GET /api/catalog/{modelName}` work.
* **Known gaps (checked by running commit `5240749`):**
  * `GET /api/hangar/analytics/fuel-efficiency` answers `500` once flights exist (`NullPointerException` on a `Double` from `FlightClientDTO`, because `flight-service` does not return `range`/`flightHours`).
  * Only `ATCC` can list the fleet (`GET /api/hangar/aircraft`), and `maintenance-service` uses that list for its alerts, so alerts are empty for every other role. `ADMIN` is refused (`403`) on every endpoint that has a role.
* **Roles (RBAC).** `JwtAuthenticationFilter` reads the `roles` claim of the token (a list or a single string; a `ROLE_` prefix is not added twice) and `@PreAuthorize` checks it on each endpoint (`@EnableMethodSecurity` is on). See section 2.1. A token that cannot be parsed is logged to `System.err` with its stack trace and the request continues unauthenticated.
* There is optimistic locking on `Aircraft` via `@Version` (not on `AircraftModel`); conflicts now map to `409`.
* Every request (except Actuator and Swagger UI paths) must carry `X-Gateway-Secret: sidis-g6-secret`, otherwise `403`. With the Spring profile `test` active the check is skipped. `/error` is open, so errors keep their real status.
* The utilization view (`/api/hangar/view/utilization`) now has a placeholder `utilization.html` template that does not render the computed data.
* Some calls to other services target endpoints or fields that are not exposed by those services (see `domain-and-resilience.md`, section 4).
* Outgoing calls have 2-second timeouts and up to 3 attempts (`@Retryable`, enabled with `@EnableRetry`).
* `GET /api/hangar/aircraft/{registrationNumber}` asks other instances of the service (`PeerForwarder`) when the aircraft is not in Redis (see `domain-and-resilience.md`, section 6). Since both instances share the same Redis cluster, a peer normally finds nothing more.

---

## 2. Runtime and Configuration

| Resource / Parameter | Current Value | Description |
| :--- | :--- | :--- |
| Service Port | `8081` (instance 1, PC2), `8082` (instance 2, PC5) | `server.port`, overridden by `SERVER_PORT` in `docker-compose.yml` |
| API Prefixes | `/api/hangar`, `/api/catalog` | Base mappings in `HangarController` and `ModelCatalogController` |
| Write database | PostgreSQL `jdbc:postgresql://${IP_PC5}:5432/aircraft_db` | User `sidis`; tables created by Hibernate (`ddl-auto=update`) |
| Read database | Redis cluster `${SPRING_REDIS_CLUSTER_NODES}` (profile `docker`) | Default `localhost:6379,6380,6381`; hashes `Aircraft` and `AircraftModel`. Without the `docker` profile: standalone Redis on `localhost:6379` |
| Profiles | `docker` (set in `docker-compose.yml`), `test` (CI) | `application-docker.properties`: Redis cluster. `application-test.properties`: PostgreSQL, Eureka, RabbitMQ and Redis on `localhost` |
| Message broker | RabbitMQ `${IP_PC5}:5672,${IP_PC5}:5673,${IP_PC3}:5674` | Exchanges `aircraft.exchange`, `aircraftmodel.exchange` |
| Metrics | `/actuator/prometheus`, `/actuator/health` | Scraped by Prometheus; exempt from the gateway secret filter |
| OpenAPI Docs | `/api/docs` | springdoc API docs path |
| Swagger UI | `/api/swagger-ui` | springdoc Swagger UI path |
| Service Registry | `${EUREKA_URL_1},${EUREKA_URL_2}` | Both discovery nodes; registers with `${IP_PC<n>}` as its address |
| Outgoing Call Timeouts | `2000` ms connect / `2000` ms read | `GlobalRestClientConfig` |
| Gateway Secret | `sidis-g6-secret` | Hardcoded in `AircraftSecurityConfig` and `JwtTokenInterceptor` |

### 2.1. Roles per Endpoint

| Endpoint | Role |
| :--- | :--- |
| `POST /api/catalog/model`, `PATCH /api/catalog/model/{modelName}` | `BACKOFFICE` |
| `GET /api/catalog/{modelName}`, `GET /api/catalog/rankings` | any authenticated user |
| `POST /api/hangar/aircraft`, `GET /api/hangar/aircraft`, `GET /api/hangar/aircraft/{reg}/compatible-routes` | `ATCC` |
| `GET /api/hangar/aircraft/{reg}` | `ATCC`, `BACKOFFICE`, `MAINTENANCE_SUPERVISOR` or `MAINTENANCE_TECHNICIAN` |
| `GET /api/hangar/aircraft/{reg}/status` | any authenticated user |
| `PATCH /api/hangar/aircraft/{reg}` | `BACKOFFICE` |
| `GET /api/hangar/analytics/aircraft-availability`, `/analytics/status-summary` | `ATCC` |
| `GET /api/hangar/analytics/fuel-efficiency`, `/fuel-efficiency/aircraft`, `/fuel-efficiency/routes`, `/operational-hours`, `GET /api/hangar/aircraft-features`, `GET /api/hangar/view/utilization` | `BACKOFFICE` |

A wrong role gets `403 Access Denied`. Other services that call these endpoints forward the original user's token, so the same rules apply to them: `maintenance-service` can look up one aircraft with a technician's or supervisor's token, but its aircraft list call (`GET /api/hangar/aircraft`, alerts) still gets `403` for them (see `../../docs/global-network-and-ports.md`, section 7.3).

External client base URLs configured in code (resolved through Eureka via `@LoadBalanced` `RestClient.Builder`):

* `flight-service`: `http://flight-service`
* `route-service`: `http://route-service`

All outgoing calls go through `JwtTokenInterceptor`, which adds `X-Gateway-Secret` and forwards the caller's `Authorization: Bearer` header.

API Gateway routing (clients call HAProxy at `https://192.168.137.1`, which forwards to one of the two gateways):

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
* [`../../docs/data-architecture-cqrs.md`](../../docs/data-architecture-cqrs.md): global description of the CQRS split, the RabbitMQ events and the Redis cluster.

---

## 4. Quick Start

The service needs PostgreSQL, a Redis cluster and RabbitMQ (see section 2). In the lab they are started by the `pc1`, `pc2`, `pc3` and `pc5` profiles of `docker-compose.yml`; the instances run on PC2 and PC5:

```bash
docker compose --profile pc2 up -d    # aircraft-service-1 (8081)
docker compose --profile pc5 up -d    # aircraft-service-2 (8082)
```

Call the API through HAProxy (requires a token from `POST /api/auth/login`):

```bash
curl -k -H "Authorization: Bearer <token>" https://192.168.137.1/api/hangar/aircraft/D-ABXA
```

Calling `http://<IP_PC2>:8081/api/...` directly without `X-Gateway-Secret` returns `403` (`"Unauthorized Access! Please use the API Gateway!"`).

Inspect the read model: `redis-cli -c -p 6379 HGETALL Aircraft:D-ABXA`. Inspect the queues: RabbitMQ management on `http://<IP_PC5>:15672` (`guest` / `guest`).

Open Swagger UI:

* URL: `http://localhost:8081/api/swagger-ui`
* The UI page loads, but the OpenAPI document at `/api/docs` is not in the filter's exemption list (it only exempts paths containing `/api-docs`), so the spec request returns `403`.
