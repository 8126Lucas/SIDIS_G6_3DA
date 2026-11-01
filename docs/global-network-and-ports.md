# AISafe Flight Management System — Network Topology, Port Mappings & Security Architecture

This document describes the network topology, port allocation, service discovery, routing and security of the **AISafe Flight Management System**, as implemented.

---

## 1. Network Architecture Overview

Clients call the system only through the **API Gateway** (HTTPS, port `7979`). The gateway validates the JWT and routes each request by path prefix to a business service, resolved through the **Eureka Discovery Service** (two peer nodes). Business services call each other over HTTP, also through Eureka, and each one rejects requests that do not carry the gateway secret header.

---

## 2. Port Allocation Matrix

Each business service has a base port and a range reserved for extra instances.

| Component | `spring.application.name` | Base Port | Reserved Range | Protocol |
| :--- | :--- | :--- | :--- | :--- |
| Discovery Service (2 nodes) | `discovery-service` | `8761` / `8762` | `8761 - 8762` | HTTP |
| API Gateway | `APIgateway` | `7979` | `7979 - 7980` | HTTPS |
| Aircraft Service | `aircraft-service` | `8081` | `8081 - 8090` | HTTP |
| Airport Service | `airport-service` | `8091` | `8091 - 8100` | HTTP |
| Auth Service | `auth-service` | `8101` | `8101 - 8110` | HTTP |
| Flight Service | `flight-service` | `8111` | `8111 - 8120` | HTTP |
| Maintenance Service | `maintenance-service` | `8121` | `8121 - 8130` | HTTP |
| Route Service | `route-service` | `8131` | `8131 - 8140` | HTTP |

`docker-compose.yml` publishes every base port on the host, so the services can also be reached directly (they then answer `403` without the gateway secret).

---

## 3. API Gateway Routing

| Path Prefix | Target | JWT Required |
| :--- | :--- | :--- |
| `/api/auth/**` | `lb://auth-service` | No |
| `/api/hangar/**`, `/api/catalog/**` | `lb://aircraft-service` | Yes |
| `/api/airports/**` | `lb://airport-service` | Yes |
| `/api/flights/**` | `lb://flight-service` | Yes |
| `/api/maintenance/**` | `lb://maintenance-service` | Yes |
| `/api/routes/**` | `lb://route-service` | Yes |

Details: [`APIgateway/docs/routing-and-security.md`](../APIgateway/docs/routing-and-security.md).

---

## 4. Service Discovery and Load Balancing

1. **Registration:** every service registers with Eureka on startup and sends a heartbeat every 30 seconds (lease expires after 90 seconds).
2. **Nodes:** `discovery-node1` (`8761`) and `discovery-node2` (`8762`) are configured as peers. `auth-service`, `aircraft-service`, `maintenance-service` and the gateway list both nodes; `airport-service`, `flight-service` and `route-service` list only `8761` locally (Docker gives every service both nodes).
3. **Load balancing:** the gateway (`lb://`) and the `@LoadBalanced` `RestClient` of each service resolve a service name to one of its registered instances (round robin).

Details and known replication limits: [`discovery-service/docs/registry-topology.md`](../discovery-service/docs/registry-topology.md).

---

## 5. Security Model

### 5.1. Authentication Flow (JWT)

1. The client sends `POST /api/auth/login` with username and password to the gateway, which routes it to `auth-service` without a JWT check.
2. `auth-service` checks the BCrypt password and returns a JWT signed with `HS384` (`sub` = collaborator UUID, `roles`, 1-hour expiration).
3. The client sends `Authorization: Bearer <token>` on every other request.
4. The gateway verifies signature and expiration with the shared `security.jwt.secret` (`401` otherwise) and forwards the request with `X-User-Name` and `X-Gateway-Secret: sidis-g6-secret`.
5. Each business service's `gatewaySecretFilter` rejects requests without that secret (`403`).

### 5.2. Roles

`auth-service` seeds one collaborator per role (`ADMIN`, `ATCC`, `BACKOFFICE`, `MAINTENANCE_TECHNICIAN`, `MAINTENANCE_SUPERVISOR`) and puts the roles in the token, but neither the gateway nor any service checks them: every authenticated collaborator can call every endpoint.

Details: [`auth-service/docs/security-and-authorization.md`](../auth-service/docs/security-and-authorization.md).

---

## 6. Inter-Service Communication

All calls are synchronous HTTP through a shared `@LoadBalanced` `RestClient` (2-second connect and read timeouts). `JwtTokenInterceptor` adds `X-Gateway-Secret` and copies the caller's `Authorization` header.

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
