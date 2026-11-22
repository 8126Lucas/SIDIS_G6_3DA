# AISafe Flight Management System — Network Topology, Port Mappings & Security Architecture

This document describes the network topology, port allocation, distribution over the five PCs, service discovery, routing and security of the **AISafe Flight Management System**, as implemented in iteration 2.

---

## 1. Network Architecture Overview

Clients call the system only through **HAProxy** on PC1 (`https://192.168.137.1`, port `443`). HAProxy forwards each TLS connection, without decrypting it, to one of the two **API Gateway** instances (`7979` on PC1, `7980` on PC4). The gateway validates the JWT and routes the request by path prefix to a business service, resolved through the **Eureka Discovery Service** (two peer nodes). Every service runs two instances on different PCs; they register in Eureka with their PC's IP, so any gateway or service can reach any instance.

Business services call each other over HTTP, also through Eureka, and each one rejects requests that do not carry the gateway secret header. The five business services with data of their own use PostgreSQL, RabbitMQ and a Redis cluster (see [`data-architecture-cqrs.md`](data-architecture-cqrs.md)).

---

## 2. Entry Point — HAProxy

Configuration: `haproxy.cfg` (image `haproxy:2.8-alpine`, profile `pc1`).

| Frontend | Port | Mode | What it does |
| :--- | :--- | :--- | :--- |
| `api_gateway_front` | `443` | TCP | Sends every connection to `api_gateway_back`. |
| `stats` | `8404` | HTTP | Live dashboard at `http://192.168.137.1:8404/stats` (refresh every 2 s). |

`api_gateway_back` balances with `roundrobin` between `gateway1` (`${IP_PC1}:7979`) and `gateway2` (`${IP_PC4}:7980`). Both servers have `check`, so HAProxy probes them with TCP connections and stops sending traffic to a gateway that does not answer (failover).

Because HAProxy works in TCP mode, TLS ends at the gateway: clients still receive the gateway's self-signed certificate (`sidis-keystore.p12`) and must accept it (`-k` in Newman, SSL verification off in Postman). The client no longer chooses a gateway port: every request goes to `https://192.168.137.1/...`.

The stats page in `haproxy.cfg` has no `stats auth` line, so it does not ask for credentials.

---

## 3. Distribution over the PCs

`docker-compose.yml` assigns each container to a profile; each PC starts its own with `docker compose --profile pc<n> up -d`.

| PC | Application containers | Infrastructure containers |
| :--- | :--- | :--- |
| **PC1** (`192.168.137.1`) | `discovery-node1`, `api-gateway-1` | `haproxy`, `prometheus`, `redis-aircraft-1`, `redis-maintenance-1` |
| **PC2** | `auth-service-1`, `aircraft-service-1`, `maintenance-service-1` | `redis-aircraft-2`, `redis-maintenance-2` |
| **PC3** | `discovery-node2`, `auth-service-2`, `airport-service-1`, `maintenance-service-2` | `rabbitmq-3`, `redis-aircraft-3`, `redis-maintenance-3` |
| **PC4** | `api-gateway-2`, `airport-service-2`, `flight-service-1`, `route-service-1` | `redis-airport-1`, `redis-flight-1`, `redis-route-1` |
| **PC5** | `aircraft-service-2`, `flight-service-2`, `route-service-2` | `postgresql-1`, `rabbitmq-1`, `rabbitmq-2`, `redis-airport-2/3`, `redis-flight-2/3`, `redis-route-2/3` |

The table in the root `README.md` ("PC Workload Distribution") predates the last changes to `docker-compose.yml` and differs from it (for example, it places `api-gateway-2` on PC3); the compose profiles above are what runs.

### 3.1. Variables each PC must define

`.env` only sets `REPO_OWNER`. Each PC also needs:

| Variable | Meaning |
| :--- | :--- |
| `IP_PC1` … `IP_PC5` | LAN IP of each PC. Used as `EUREKA_INSTANCE_IP_ADDRESS`, in the instance ids, and in the PostgreSQL, RabbitMQ and Redis addresses. |
| `EUREKA_URL_1`, `EUREKA_URL_2` | URLs of the two Eureka nodes (`http://<IP_PC1>:8761/eureka/`, `http://<IP_PC3>:8762/eureka/`). |

Every business and auth container also sets `SPRING_PROFILES_ACTIVE=docker`. In the five business services that profile holds the Redis cluster nodes (`application-docker.properties`; `auth-service` has no such file, so the profile changes nothing there); without it the services use a single Redis on `localhost:6379` (see `data-architecture-cqrs.md`, section 5).

---

## 4. Port Allocation Matrix

### 4.1. Application

| Component | `spring.application.name` | Instance 1 | Instance 2 | Reserved range | Protocol |
| :--- | :--- | :--- | :--- | :--- | :--- |
| HAProxy | — | `443` (PC1) | — | — | TCP (TLS pass-through) |
| Discovery Service | `discovery-service` | `8761` (PC1) | `8762` (PC3) | `8761 - 8762` | HTTP |
| API Gateway | `APIgateway` | `7979` (PC1) | `7980` (PC4) | `7979 - 7980` | HTTPS |
| Aircraft Service | `aircraft-service` | `8081` (PC2) | `8082` (PC5) | `8081 - 8090` | HTTP |
| Airport Service | `airport-service` | `8091` (PC3) | `8092` (PC4) | `8091 - 8100` | HTTP |
| Auth Service | `auth-service` | `8101` (PC2) | `8102` (PC3) | `8101 - 8110` | HTTP |
| Flight Service | `flight-service` | `8111` (PC4) | `8112` (PC5) | `8111 - 8120` | HTTP |
| Maintenance Service | `maintenance-service` | `8121` (PC2) | `8122` (PC3) | `8121 - 8130` | HTTP |
| Route Service | `route-service` | `8131` (PC4) | `8132` (PC5) | `8131 - 8140` | HTTP |

The services' ports are published on their PC, so they can also be reached directly (they then answer `403` without the gateway secret).

### 4.2. Infrastructure

| Component | Host ports | PC |
| :--- | :--- | :--- |
| PostgreSQL (`postgresql-1`) | `5432` | PC5 |
| RabbitMQ AMQP / management | `5672` / `15672`, `5673` / `15673` | PC5 |
| RabbitMQ AMQP / management | `5674` / `15674` | PC3 |
| Redis aircraft | `6379`, `6380`, `6381` | PC1, PC2, PC3 |
| Redis maintenance | `6382`, `6383`, `6384` | PC1, PC2, PC3 |
| Redis airport | `6385`, `6386`, `6387` | PC4, PC5, PC5 |
| Redis flight | `6388`, `6389`, `6390` | PC4, PC5, PC5 |
| Redis route | `6391`, `6392`, `6393` | PC4, PC5, PC5 |
| HAProxy stats | `8404` | PC1 |
| Prometheus | `9090` | PC1 |

---

## 5. API Gateway Routing

| Path Prefix | Target | JWT Required |
| :--- | :--- | :--- |
| `/api/auth/**` | `lb://auth-service` | No |
| `/api/hangar/**`, `/api/catalog/**` | `lb://aircraft-service` | Yes |
| `/api/airports/**` | `lb://airport-service` | Yes |
| `/api/flights/**` | `lb://flight-service` | Yes |
| `/api/maintenance/**` | `lb://maintenance-service` | Yes |
| `/api/routes/**` | `lb://route-service` | Yes |

All six routes use the `spring.cloud.gateway.server.webflux.routes[n]` keys. The `test` profile (`application-test.properties`) repeats the same routes with Eureka on `http://localhost:8761/eureka/`, for CI.

Details: [`APIgateway/docs/routing-and-security.md`](../APIgateway/docs/routing-and-security.md).

---

## 6. Service Discovery and Load Balancing

1. **Registration:** every instance registers with Eureka on startup with `prefer-ip-address=true`, `EUREKA_INSTANCE_IP_ADDRESS=${IP_PC<n>}` and an instance id `<ip>:<service>:<port>`, so two instances of the same service on different PCs are kept apart. Heartbeat every 30 seconds, lease expires after 90 seconds.
2. **Nodes:** `discovery-node1` (PC1, `8761`) uses `EUREKA_URL_2` as its peer and `discovery-node2` (PC3, `8762`) uses `EUREKA_URL_1`, so they replicate their registries. Every other component lists both nodes (`${EUREKA_URL_1},${EUREKA_URL_2}`).
3. **Load balancing:** HAProxy spreads clients over the two gateways; the gateway (`lb://`) and the `@LoadBalanced` `RestClient` of each service spread calls over the registered instances of the target service (round robin).

Details and known replication limits: [`discovery-service/docs/registry-topology.md`](../discovery-service/docs/registry-topology.md).

---

## 7. Security Model

### 7.1. Authentication Flow (JWT)

1. The client sends `POST /api/auth/login` with username and password to HAProxy, which hands the connection to a gateway; the gateway routes it to `auth-service` without a JWT check.
2. `auth-service` checks the BCrypt password and returns a JWT signed with `HS384` (`sub` = collaborator UUID, `roles`, 1-hour expiration).
3. The client sends `Authorization: Bearer <token>` on every other request.
4. The gateway verifies signature and expiration with the shared `security.jwt.secret` (`401` otherwise) and forwards the request with `X-User-Name` and `X-Gateway-Secret: sidis-g6-secret`.
5. Each business service's `gatewaySecretFilter` rejects requests without that secret (`403`).

Both gateways and both `auth-service` instances share the same secret, so a token issued by one instance is accepted by every gateway.

### 7.2. Roles

`auth-service` seeds one collaborator per role and puts the roles in the token:

| Username / password | Role |
| :--- | :--- |
| `admin` / `admin123` | `ADMIN` |
| `atcc` / `atcc123` | `ATCC` |
| `backoffice` / `backoffice123` | `BACKOFFICE` |
| `technician` / `technician123` | `MAINTENANCE_TECHNICIAN` |
| `supervisor` / `supervisor123` | `MAINTENANCE_SUPERVISOR` |

The gateway still only validates the token. Every business controller now declares its roles with `@PreAuthorize`, but a role is only checked where the service also has a `JwtAuthenticationFilter` (reads the `roles` claim, accepts a list or a single string, and maps it to `ROLE_<role>`) **and** `@EnableMethodSecurity`:

| Service | Role checks |
| :--- | :--- |
| `aircraft-service` | **Enforced** (`@EnableMethodSecurity`). For example `POST /api/hangar/aircraft`, `GET /api/hangar/aircraft` and the availability analytics need `ATCC`; model changes, availability changes, fuel and operational-hours analytics need `BACKOFFICE`; `GET /api/hangar/aircraft/{reg}` accepts `ATCC`, `BACKOFFICE`, `MAINTENANCE_SUPERVISOR` and `MAINTENANCE_TECHNICIAN`. A wrong role gets `403 Access Denied`. |
| `maintenance-service` | **Declared but not enforced.** The controller has `@PreAuthorize` on every endpoint (templates, records and status changes: `MAINTENANCE_TECHNICIAN`; ongoing, turnaround and parts: `MAINTENANCE_SUPERVISOR`; costs and hours: `ATCC`; alerts: `ATCC` or `MAINTENANCE_SUPERVISOR`), and it has a `JwtAuthenticationFilter`, but `MaintenanceSecurityConfig` has no `@EnableMethodSecurity`, so any authenticated collaborator passes. |
| `airport-service`, `flight-service`, `route-service` | **Declared but not enforced.** `@PreAuthorize` was added to every endpoint (with the US number in a comment), but these services have neither a `JwtAuthenticationFilter` nor `@EnableMethodSecurity`, and their chain is `permitAll()`. The roles are listed in each service's `README.md`. |

The controller tests (`MockMvc`) enable method security in their own test configuration, so they pass even where the running service does not check roles.

Details: [`auth-service/docs/security-and-authorization.md`](../auth-service/docs/security-and-authorization.md).

### 7.3. Roles in calls between services

A service that calls another one forwards the caller's `Authorization` header (`JwtTokenInterceptor`), so the second service checks the **original user's** role. `maintenance-service` calls `aircraft-service` in two ways:

* `GET /api/hangar/aircraft/{reg}` (record creation, model names in the turnaround and cost reports) now also accepts `MAINTENANCE_TECHNICIAN` and `MAINTENANCE_SUPERVISOR`, so a technician can create a record.
* `GET /api/hangar/aircraft?size=1000` (maintenance alerts) still needs `ATCC`. With any other token (the supervisor included) `aircraft-service` answers `403`; `AircraftRestClientService.findAll` catches it and returns an empty list, so the alert list is empty instead of an error (checked by running the system).
* `ADMIN` is not accepted by any `@PreAuthorize` in `aircraft-service`, so an `ADMIN` creating a maintenance record gets `500` (from the forwarded `403`).

### 7.4. Test profile

`aircraft-service` and `maintenance-service` skip the gateway secret check when the Spring profile `test` is active (their `gatewaySecretFilter` reads the active profiles; CI starts every service with this profile). Started with `test`, they accept direct calls without `X-Gateway-Secret`. `airport-service`, `flight-service` and `route-service` still require the header in every profile.

In `aircraft-service`, `/error` is also open (`permitAll`), so an exception inside a protected request returns its real status instead of `403`.

---

## 8. Inter-Service Communication

### 8.1. Synchronous (HTTP)

All calls go through a shared `@LoadBalanced` `RestClient` (2-second connect and read timeouts). `JwtTokenInterceptor` adds `X-Gateway-Secret` and copies the caller's `Authorization` header. In `flight-service` the builder also sets `X-Gateway-Secret` as a default header (property `gateway.secret`, default `sidis-g6-secret`), so the header is sent twice with the same value.

| Caller | Callee | Calls | Purpose |
| :--- | :--- | :--- | :--- |
| `flight-service` | `aircraft-service` | `GET /api/hangar/aircraft/{reg}`, `GET /api/catalog/{model}` | Aircraft status, range and capacity when scheduling |
| `flight-service` | `route-service` | `GET /api/routes/{id}`, `PATCH /api/routes/{id}` | Route checks and usage counter |
| `flight-service` | `airport-service` | `GET /api/airports/{iata}` | Airport status |
| `route-service` | `airport-service` | `GET /api/airports/{iata}` | Airport status and location when creating or changing a route |
| `airport-service` | `route-service` | `GET /api/routes` | Routes of an airport, busiest airports |
| `aircraft-service` | `route-service` | `GET /api/routes`, `GET /api/routes/{id}` | Compatible routes, fuel per route |
| `aircraft-service` | `flight-service` | `GET /api/flights`, `GET /api/flights/aircraft/{reg}` | Model rankings, fuel per route, utilization view |
| `maintenance-service` | `aircraft-service` | `GET /api/hangar/aircraft/{reg}`, `GET /api/hangar/aircraft?size=1000` | Aircraft checks, model names, alerts |

Retries: `aircraft-service` and `maintenance-service` retry failed calls up to 3 times (`@EnableRetry`). `airport-service`, `flight-service` and `route-service` have `@Retryable` annotations but no `@EnableRetry`, so they do not retry. There is no circuit breaker.

### 8.2. Asynchronous (RabbitMQ)

Each of the five services with a database uses RabbitMQ inside itself, to project its writes into Redis (CQRS). No event crosses service boundaries. Exchanges, queues and routing keys: [`data-architecture-cqrs.md`](data-architecture-cqrs.md), section 4.2.

---

## 9. Monitoring

`prometheus` (PC1, `9090`, `prometheus.yml`) discovers its targets in Eureka (`http://discovery-node1:8761/eureka`) and scrapes `/actuator/prometheus` every 15 seconds, using HTTPS for the gateways. All eight modules include `micrometer-registry-prometheus` and expose `/actuator/prometheus`, `/actuator/health` and `/actuator/info`; the gateway secret filter of the five business services lets `/actuator` through. `auth-service` does not exempt it: its security chain requires authentication on every path except login, so Prometheus gets `403` from it.
