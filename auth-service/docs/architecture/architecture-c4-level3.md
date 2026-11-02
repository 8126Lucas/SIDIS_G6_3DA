# Auth Service (`auth-service`) — C4 Level 3 Component View (As Implemented)

This document describes the current internal component architecture at C4 Level 3 based on implemented classes.

---

## 1. Current Component Responsibilities

The `auth-service` currently exposes two controllers and the following main components:

* **`SecurityConfig`**
   * Stateless `SecurityFilterChain`: login and welcome are public, everything else requires authentication.
   * `BCryptPasswordEncoder` bean.
* **`AuthController`** (`/api/auth`)
   * `POST /login`.
* **`LandingController`** (`/api`)
   * `GET /welcome`.
* **`AuthenticateCollaboratorUseCase`**
   * Username lookup, BCrypt password check, token generation.
* **`JwtService`**
   * Token generation (`HS384`, `sub` = `collaboratorId`, `roles`, 1-hour expiration) and token parsing helpers.
* **`JwtAuthenticationFilter`**
   * Parses a Bearer token into a Spring `Authentication`; registered as a servlet filter only, not in the security chain.
* **`CollaboratorDetailsService`**
   * `UserDetailsService` implementation; not used.
* **`CollaboratorRepository`**
   * Spring Data JPA persistence of `Collaborator`.
* **`GlobalExceptionHandler`**
   * Cross-cutting HTTP error normalization (`ErrorDetails`).
* **`UseCaseLoggingAdvice`**
   * AOP logging of `@UseCase` classes (`@within` pointcut); arguments of `@SuppressArgLogging` classes are logged as `[SUPPRESSED]`.
* **`Bootstrap`**
   * Seeds five collaborators, one per role.

External integration clients:

* None. The service makes no outgoing calls; the API Gateway validates its tokens with the shared secret.

---

## 2. Architecture Diagram Source

PlantUML source file:

* `architecture-c4-level3.puml`

---

## 3. Data Flow and Dependencies

1. Login requests arrive through the API Gateway on `/api/auth/**` (no JWT filter) or directly on port `8101`.
2. `SecurityConfig` lets `POST /api/auth/login` through.
3. `AuthController` delegates to `AuthenticateCollaboratorUseCase`, which reads `CollaboratorRepository` and checks the password with `PasswordEncoder`.
4. `JwtService` signs the token with `security.jwt.secret`; the controller returns `TokenResponse`.
5. Exceptions are transformed by `GlobalExceptionHandler` into `ErrorDetails` payloads.
6. Clients send the token to the gateway, which validates it with the same secret before routing to the other services.
