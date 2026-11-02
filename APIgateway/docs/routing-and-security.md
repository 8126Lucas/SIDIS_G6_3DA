# API Gateway — Routing and Security (As Implemented)

---

## 1. Route Table

Routes are defined in `application.properties` (`spring.cloud.gateway.server.webflux.routes[*]`).

| Route Id | Path Predicate | Target | JWT Filter |
| :--- | :--- | :--- | :--- |
| `auth-service` | `/api/auth/**` | `lb://auth-service` | No (public) |
| `aircraft-service` | `/api/hangar/**`, `/api/catalog/**` | `lb://aircraft-service` | `JwtAuthentication` |
| `airport-service` | `/api/airports/**` | `lb://airport-service` | `JwtAuthentication` |
| `maintenance-service` | `/api/maintenance/**` | `lb://maintenance-service` | `JwtAuthentication` |
| `route-service` | `/api/routes/**` | `lb://route-service` | `JwtAuthentication` |
| `flight-service` | `/api/flights/**` | `lb://flight-service` | `JwtAuthentication` |

* Paths are forwarded unchanged (no `StripPrefix` / `RewritePath`).
* A path that matches no route returns `404 Not Found` from the gateway.
* `lb://` names are resolved through the Eureka registry; if no instance is registered, the gateway returns `503 Service Unavailable`.
* The gateway talks to the services over plain HTTP; HTTPS only applies between the client and the gateway.

---

## 2. JWT Filter (`JwtAuthenticationGatewayFilterFactory`)

Applied to every route except `auth-service`. Rules, in order:

| Check | Result on Failure |
| :--- | :--- |
| `Authorization` header present | `401 Unauthorized` (empty body) |
| Header starts with `Bearer ` | `401 Unauthorized` (empty body) |
| Token parses with `security.jwt.secret` (signature valid, not expired, well-formed) | `401 Unauthorized` (empty body) |

On success the request is forwarded with two headers added (they replace any value sent by the client):

| Header | Value | Used By |
| :--- | :--- | :--- |
| `X-User-Name` | The token `sub` claim (the collaborator's UUID, not the username) | Not read by any service |
| `X-Gateway-Secret` | `sidis-g6-secret` | `gatewaySecretFilter` in aircraft, airport, flight, maintenance and route services (`403` if missing) |

The token itself is issued by `auth-service` (`HS384`, 1-hour expiration); see `auth-service/docs/security-and-authorization.md`.

---

## 3. Known Limitations

* **No authorization by role.** The `roles` claim is ignored by the gateway and by the services.
* **Static gateway secret.** `X-Gateway-Secret` is a fixed string, hardcoded in the gateway and in each service. The service ports are also published by `docker-compose.yml`, so anyone who knows the string can call a service directly and skip JWT validation.
* **Duplicated JWT secret.** `security.jwt.secret` must have the same value in `APIgateway` and `auth-service`; changing only one makes every protected request return `401`.
* **No error body.** `401` responses from the filter have no JSON payload, unlike the `ErrorDetails` returned by the services.
* **Secrets in source.** The JWT secret and the keystore password are stored in `application.properties`.
