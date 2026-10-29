# Auth Service (`auth-service`) — Technical Documentation

This folder documents the behavior that is currently implemented in code for the `auth-service` microservice.

---

## 1. Scope and Responsibilities

The service is a Spring Boot microservice that authenticates company collaborators and issues JWTs that the API Gateway validates for every other service.

Current implemented responsibilities:

* Authenticate a collaborator by username and password (BCrypt) and return a signed JWT.
* Store collaborators and their roles (`ADMIN`, `ATCC`, `BACKOFFICE`, `MAINTENANCE_TECHNICIAN`, `MAINTENANCE_SUPERVISOR`).
* Seed five collaborators, one per role.
* Return standardized API errors via `GlobalExceptionHandler`.

Important implementation notes:

* Invalid credentials return `409 Conflict` (`InvalidCredentialsException` extends `DomainException`), not `401`.
* The JWT `sub` claim is the collaborator's `collaboratorId` (UUID), not the username; the gateway forwards it as `X-User-Name`.
* Roles are put in the token, but no service checks them (see `security-and-authorization.md`, section 4).
* There are no endpoints to register, list or change collaborators.
* `JwtAuthenticationFilter` is not added to the `SecurityFilterChain`, and the H2 console and Swagger UI are blocked by the chain (see `security-and-authorization.md`, section 2).

---

## 2. Runtime and Configuration

| Resource / Parameter | Current Value | Description |
| :--- | :--- | :--- |
| Service Port | `8101` | Configured in `application.properties` |
| API Prefix | `/api/auth` | Base mapping in `AuthController` |
| Database | `H2 In-Memory` | `jdbc:h2:mem:testdb` |
| Schema Strategy | `update` | `spring.jpa.hibernate.ddl-auto` |
| H2 Console Path | `/h2-console` | Enabled, but requires authentication (blocked) |
| OpenAPI Docs / Swagger UI | `/api/docs`, `/api/swagger-ui` | Configured, but require authentication (blocked) |
| JWT Secret | `security.jwt.secret` | 49-byte HMAC key (signs with `HS384`) |
| JWT Expiration | `3600000` ms (1 hour) | `security.jwt.expiration-ms` |
| Service Registry | `http://localhost:8761/eureka/` | Eureka client registration |

API Gateway routing (`https://localhost:7979`):

* `/api/auth/**` is routed to `lb://auth-service` without the `JwtAuthentication` filter, so login is public.
* `/api/welcome` is not under `/api/auth` and is only reachable directly on the service port.

---

## 3. Documentation Index

This `docs/` directory is organized as follows:

* `api-specification.md`: Real API contract implemented by the controllers.
* `security-and-authorization.md`: Token format, security filter chain, gateway validation, and role usage.
* `architecture/architecture-c4-level3.md`: Component-level architecture aligned with current classes.
* `architecture/architecture-c4-level3.puml`: PlantUML source of the architecture diagram.
* `sequence-diagrams/README.md`: Dynamic behavior narrative for login and authenticated requests.
* `sequence-diagrams/scenario-1-login.puml`: Sequence for `POST /api/auth/login`.
* `sequence-diagrams/scenario-2-authenticated-request.puml`: Sequence for a request to another service using the issued token.

---

## 4. Quick Start

Run locally:

```bash
./mvnw spring-boot:run
```

Log in through the gateway:

```bash
curl -k -X POST https://localhost:7979/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "technician", "password": "technician123"}'
```

Use the returned token on any other route:

```bash
curl -k -H "Authorization: Bearer <token>" https://localhost:7979/api/maintenance/templates
```

Seeded collaborators:

| Username | Password | Role |
| :--- | :--- | :--- |
| `admin` | `admin123` | `ADMIN` |
| `atcc` | `atcc123` | `ATCC` |
| `backoffice` | `backoffice123` | `BACKOFFICE` |
| `technician` | `technician123` | `MAINTENANCE_TECHNICIAN` |
| `supervisor` | `supervisor123` | `MAINTENANCE_SUPERVISOR` |
