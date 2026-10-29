# Auth Service — REST API Specification (As Implemented)

This document reflects the current implementation in `AuthController`, `LandingController`, and related classes.

---

## 1. General Service Configuration

* **Base URL (via API Gateway):** `https://localhost:7979`
* **Base URL (service, local):** `http://localhost:8101`
* **API Prefix:** `/api/auth`
* **Content-Type:** `application/json`

No header is required to log in. The gateway does not apply the `JwtAuthentication` filter to `/api/auth/**`, and `auth-service` does not check `X-Gateway-Secret`.

---

## 2. Implemented Endpoints Summary

| Method | Endpoint Path | Description | Access | Status Codes |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Authenticate and return a JWT | Public | `200`, `400`, `409` |
| `GET` | `/api/welcome` | Landing message (not routed by the gateway) | Public | `200` |

Any other path (including `/h2-console`, `/api/docs` and `/api/swagger-ui`) requires an authenticated request and returns `403` (see `security-and-authorization.md`, section 2).

---

## 3. Endpoint Details

### 3.1. Login

* **Endpoint:** `POST /api/auth/login`
* **Validation annotations on request:**
  * `username`: `@NotBlank`
  * `password`: `@NotBlank`

#### Request Example

```json
{
  "username": "technician",
  "password": "technician123"
}
```

#### Successful Response (`200 OK`)

```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiIzM2M4M2Y1Ni0wOTNhLTRkNzUtODc0NS0xYmZlM2M2N2E1NjciLCJyb2xlcyI6WyJNQUlOVEVOQU5DRV9URUNITklDSUFOIl0sImlhdCI6MTc3OTg3ODM0NCwiZXhwIjoxNzc5ODgxOTQ0fQ.<signature>"
}
```

Decoded payload:

```json
{
  "sub": "33c83f56-093a-4d75-8745-1bfe3c67a567",
  "roles": ["MAINTENANCE_TECHNICIAN"],
  "iat": 1779878344,
  "exp": 1779881944
}
```

Execution steps:

1. Find the collaborator by `username`.
2. Compare the password with the stored BCrypt hash.
3. Generate the token with `JwtService.generateToken(...)`.

Errors:

| Case | Exception | HTTP Status |
| :--- | :--- | :--- |
| Blank `username` or `password` | `MethodArgumentNotValidException` | `400 Bad Request` (`validationErrors` filled) |
| Malformed JSON | `HttpMessageNotReadableException` | `400 Bad Request` |
| Unknown username | `InvalidCredentialsException` | `409 Conflict` (`"Invalid credentials"`) |
| Wrong password | `InvalidCredentialsException` | `409 Conflict` (`"Invalid credentials"`) |

Notes:

* Unknown username and wrong password return the same message, so the response does not reveal which usernames exist.
* Invalid credentials map to `409`, not `401 Unauthorized`.
* There is no refresh token, logout, or token revocation.

---

### 3.2. Welcome

* **Endpoint:** `GET /api/welcome`
* **Response (`200 OK`):** `Welcome to the SIDIS_G6_3DA` (plain text).

---

## 4. Error Payload (`ErrorDetails`)

```json
{
  "timestamp": "2026-10-29T12:00:00",
  "status": 409,
  "error": "Conflict",
  "message": "Invalid credentials",
  "path": "/api/auth/login",
  "validationErrors": null
}
```

`GlobalExceptionHandler` also maps `ResourceNotFoundException` (`404`), `IllegalArgumentException` (`400`), `IllegalStateException` and `ObjectOptimisticLockingFailureException` (`409`), and any other exception (`500`), but the login flow only raises the exceptions in section 3.1. The `403` from Spring Security is not an `ErrorDetails` payload.
