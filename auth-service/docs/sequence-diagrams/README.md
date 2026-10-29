# Auth Service — Sequence Diagrams and Dynamic Views (As Implemented)

This document describes runtime behavior aligned with the current code implementation.

---

## 1. Scenario 1: Login

### Description

Scenario 1 describes `POST /api/auth/login`, the only business endpoint of the service.

### Sequence Diagram Source

* `scenario-1-login.puml`

### Key Steps

1. Client sends `username` and `password` to the gateway, which routes `/api/auth/**` without the JWT filter.
2. `SecurityFilterChain` permits `POST /api/auth/login`.
3. Blank fields → `400` (`ErrorDetails` with `validationErrors`).
4. `AuthenticateCollaboratorUseCase` loads the collaborator by username; not found → `InvalidCredentialsException` → `409`.
5. `PasswordEncoder` (BCrypt) checks the password; mismatch → `409`.
6. `JwtService` builds the token (`sub` = `collaboratorId`, `roles`, 1-hour expiration, `HS384`).
7. Controller returns `200 OK` with `{ "token": "..." }`.

---

## 2. Scenario 2: Authenticated Request

### Description

Scenario 2 shows how the token issued by `auth-service` is used on another route, taking `GET /api/maintenance/templates` as the example. `auth-service` itself is not called in this flow.

### Sequence Diagram Source

* `scenario-2-authenticated-request.puml`

### Key Steps

1. Client sends the request with `Authorization: Bearer <token>`.
2. `JwtAuthenticationGatewayFilterFactory` rejects a missing header, a non-Bearer header, a bad signature or an expired token with `401` (empty body).
3. On success, the gateway adds `X-User-Name` (the UUID subject) and `X-Gateway-Secret` and routes the request through Eureka.
4. In `aircraft-service` and `maintenance-service`, `gatewaySecretFilter` checks the secret (`403` if missing); `airport-service`, `route-service` and `flight-service` do not check it.
5. The controller handles the request; roles are not checked anywhere.

---

## 3. Exception Summary

| Case | Where | HTTP Status |
| :--- | :--- | :--- |
| Blank login fields / malformed JSON | `auth-service` | `400 Bad Request` |
| Unknown username / wrong password | `auth-service` | `409 Conflict` |
| Any non-public path on `auth-service` | Spring Security | `403 Forbidden` |
| Missing / invalid / expired token | API Gateway | `401 Unauthorized` |
| Missing `X-Gateway-Secret` | `aircraft-service`, `maintenance-service` | `403 Forbidden` |
