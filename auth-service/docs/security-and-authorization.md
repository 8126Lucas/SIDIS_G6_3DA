# Auth Service — Security and Authorization (As Implemented)

## 1. Collaborator Model

### 1.1. `Collaborator` (table `company_colaborators`)

* `pk`: generated identifier (`IDENTITY`).
* `version`: optimistic locking field (`@Version`).
* `collaboratorId`: random `UUID`, unique; used as the JWT subject.
* `username`: unique, required.
* `password`: BCrypt hash, required.
* `email`: required (not unique).
* `roles`: element collection (`EAGER`) in table `company_colaborators_roles`, enum `Role`:
  * `ADMIN`, `ATCC`, `BACKOFFICE`, `MAINTENANCE_TECHNICIAN`, `MAINTENANCE_SUPERVISOR`.

The constructor rejects blank `username`, `email`, `password` and `null` roles (`Assert`). The entity is declared with `@Inheritance(JOINED)`, but it has no subclasses.

`CollaboratorRepository` offers `findByUsername(...)` (used by login) and `findByEmail(...)` (not used).

## 2. Security Filter Chain (`SecurityConfig`)

* CSRF disabled; sessions are `STATELESS`.
* `POST /api/auth/login` and `GET /api/welcome` are `permitAll()`.
* Every other request must be authenticated. There is no form login or HTTP Basic, so unauthenticated requests receive `403 Forbidden` from Spring Security.
* `PasswordEncoder` is `BCryptPasswordEncoder`.
* `@EnableMethodSecurity` is enabled, but no method uses `@PreAuthorize` or similar annotations.

Current gaps:

* The `if (h2ConsoleEnabled) { }` block inside `authorizeHttpRequests` is empty, so `/h2-console` is not permitted and returns `403`. Only the frame options are relaxed for it.
* `/api/docs` and `/api/swagger-ui` are not permitted either, so Swagger UI returns `403`.
* `JwtAuthenticationFilter` is a `@Component` but is not registered with `addFilterBefore(...)`. Spring Boot registers it as a plain servlet filter, which runs after `springSecurityFilterChain`, so it never authenticates a request before the authorization check. Since all routes are either public or blocked, this has no visible effect today.
* `CollaboratorDetailsService` (`UserDetailsService`) is defined but not used by any `AuthenticationManager` or controller.

## 3. Token Issuing (`JwtService`)

| Item | Value |
| :--- | :--- |
| Library | `jjwt` `0.12.6` |
| Key | `Keys.hmacShaKeyFor(security.jwt.secret)`; 49 bytes (392 bits) |
| Algorithm | `HS384` (chosen by `jjwt` from the key length) |
| `sub` | `collaboratorId` (UUID) |
| `roles` | list of role names, without the `ROLE_` prefix |
| `iat` / `exp` | issue time / issue time + 1 hour |

`JwtService` also has `isTokenValid(...)`, `extractSubject(...)` and `extractRoles(...)`, used only by the inactive `JwtAuthenticationFilter`.

`AuthenticateCollaboratorUseCase` is `@UseCase` with `@SuppressArgLogging` so the password is not logged, but the `UseCaseLoggingAdvice` pointcut is `within(com.sidis.authservice.UseCase)`, which matches no class, so nothing is logged anyway.

## 4. Token Validation Across the System

`auth-service` only issues tokens. Validation is done by the API Gateway:

1. `/api/auth/**` is routed without a filter.
2. Every other route (`aircraft`, `airport`, `maintenance`, `route`, `flight`) uses `JwtAuthenticationGatewayFilterFactory`, which:
   * requires `Authorization: Bearer <token>` (`401` otherwise);
   * verifies the signature with the same secret (hardcoded in the gateway class) and the expiration (`401` on any parsing error);
   * forwards the request with `X-User-Name: <sub>` and `X-Gateway-Secret: sidis-g6-secret`.
3. `aircraft-service` and `maintenance-service` reject requests without `X-Gateway-Secret` (`403`). `airport-service`, `route-service` and `flight-service` do not check it.
4. `aircraft-service` and `maintenance-service` forward the incoming `Authorization` header and the secret on their outgoing calls (`JwtTokenInterceptor`).

Role usage:

* The gateway does not read the `roles` claim.
* No downstream service reads `roles` or `X-User-Name`; there is no role-based authorization anywhere in the system.

Known weaknesses:

* The JWT secret is duplicated: `security.jwt.secret` in `auth-service` and a hardcoded constant in the gateway filter. Changing one breaks validation.
* `X-User-Name` carries the UUID subject, not the username.
* `auth-service` does not check `X-Gateway-Secret`, so login also works directly on port `8101`.
* There is no token revocation; a token stays valid for one hour after issue.

## 5. Exception Handling

* `InvalidCredentialsException` extends `DomainException` → `409 Conflict` (`"Invalid credentials"`).
* Bean validation and malformed JSON → `400 Bad Request`.
* Other handlers (`404`, `409`, `500`) exist in `GlobalExceptionHandler` but are not reached by the login flow.

## 6. Bootstrap

`Bootstrap` (`ApplicationRunner`) seeds five collaborators when the table is empty, one per role, with BCrypt-encoded passwords (`admin`/`admin123`, `atcc`/`atcc123`, `backoffice`/`backoffice123`, `technician`/`technician123`, `supervisor`/`supervisor123`). The `@Profile("dev")` annotation is commented out, so the seed runs in every environment.
