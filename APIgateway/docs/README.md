# API Gateway (`APIgateway`) — Technical Documentation

This folder documents the behavior that is currently implemented in code for the `APIgateway` module.

---

## 1. Scope and Responsibilities

The gateway is a Spring Cloud Gateway (WebFlux) application and the single public entry point of the system. It has no business logic, database or REST controllers of its own.

Current implemented responsibilities:

* Terminate HTTPS on port `7979` with a local PKCS12 keystore.
* Route requests by path prefix to the six business services, resolved through Eureka (`lb://<service-name>`).
* Validate the JWT on every route except `/api/auth/**` (`JwtAuthenticationGatewayFilterFactory`).
* Add `X-User-Name` and `X-Gateway-Secret` headers to authenticated requests before forwarding them.
* Load-balance across registered instances of each service (Spring Cloud LoadBalancer, round robin).

Important implementation notes:

* The gateway only checks that the token is signed with the shared secret and not expired. It does not read the `roles` claim, so every logged-in collaborator can call every route.
* Every rejection by the JWT filter is `401 Unauthorized` with an empty body.
* There is no rate limiting, CORS configuration, retry or circuit breaker configured in the gateway.
* `/api/welcome` (`auth-service`) and the H2 / Swagger paths of the services are not routed.

---

## 2. Runtime and Configuration

| Resource / Parameter | Current Value | Description |
| :--- | :--- | :--- |
| Application Name | `APIgateway` | Registered in Eureka as `APIGATEWAY` |
| Port | `7979` (HTTPS) | `server.port` |
| TLS Keystore | `classpath:sidis-keystore.p12` | PKCS12, alias `sidis-key` (self-signed, local use) |
| JWT Secret | `security.jwt.secret` | Same value as in `auth-service` |
| Forwarded Gateway Secret | `sidis-g6-secret` | Hardcoded in `JwtAuthenticationGatewayFilterFactory` |
| Service Registry | `http://localhost:8761/eureka/,http://localhost:8762/eureka/` | Both discovery nodes |
| Actuator | `/actuator/health` | Default exposure of `spring-boot-starter-actuator` |

Docker (`docker-compose.yml`): container `api-gateway`, port `7979:7979`, registry overridden to `http://discovery-node1:8761/eureka/,http://discovery-node2:8762/eureka/`.

---

## 3. Documentation Index

This `docs/` directory is organized as follows:

* `routing-and-security.md`: Route table, JWT filter rules, forwarded headers and known limitations.
* `architecture/architecture-c4-level3.md`: Component-level architecture aligned with current classes.
* `architecture/architecture-c4-level3.puml`: PlantUML source of the architecture diagram.
* `sequence-diagrams/README.md`: Dynamic behavior of a request through the gateway.
* `sequence-diagrams/scenario-1-routed-request.puml`: Sequence for public and protected routes.

---

## 4. Quick Start

Start `discovery-service` first, then the gateway:

```bash
./mvnw spring-boot:run
```

Log in (public route) and call a protected route:

```bash
curl -k -X POST https://localhost:7979/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "backoffice", "password": "backoffice123"}'

curl -k -H "Authorization: Bearer <token>" https://localhost:7979/api/hangar/analytics/status-summary
```

`-k` is needed because the certificate is self-signed.

---

## 5. Tests

`APIgatewayApplicationTests` (via `support/GatewayHttpTest`) is an end-to-end test, not a Spring context test:

* It logs in as `backoffice` through the real gateway and calls `GET /api/hangar/analytics/status-summary`, expecting `200`.
* It needs the gateway, `discovery-service`, `auth-service` and `aircraft-service` already running. Without them, `./mvnw verify` fails on this module.
* Base URL and credentials can be overridden with `SIDIS_BASE_URL`, `SIDIS_BACKOFFICE_USERNAME` and `SIDIS_BACKOFFICE_PASSWORD`.
* The CI workflow starts Eureka, the gateway and all services, waits 200 seconds, and only then runs `./mvnw verify`.
