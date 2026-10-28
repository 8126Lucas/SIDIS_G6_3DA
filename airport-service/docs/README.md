# Airport Service (`airport-service`) — Technical Documentation

This folder documents the behavior that is currently implemented in code for the `airport-service` microservice.

---

## 1. Scope and Responsibilities

The service is a Spring Boot microservice focused on the registry of airports used by the company network.

Current implemented responsibilities:

* Register airports with location, facilities, runways, certifications, contacts, and photos.
* Bulk import airports from a CSV file (OpenCSV).
* Add certifications to an existing airport.
* Query airports by IATA code and by filters (city, country, name).
* Change airport status and update airport details (name, contacts).
* Group airports by country or city.
* List the routes departing from an airport and rank airports by number of routes using `route-service` data.
* Return standardized API errors via `GlobalExceptionHandler`.

Important implementation notes:

* There is optimistic locking on `Airport` via `@Version`.
* A missing airport is reported as `DomainException` (`409 Conflict`), not `404`; `ResourceNotFoundException` exists but is never thrown.
* Request records have no Bean Validation constraints, so `@Valid` has no effect; invalid input fails in the domain, in enum parsing, or at persistence.
* `operationalHours` is accepted by `PATCH /details` but is not stored (no such field in `Airport`).
* The `UseCaseLoggingAdvice` pointcut does not match `AirportService` (see `domain-and-resilience.md`, section 6).

---

## 2. Runtime and Configuration

| Resource / Parameter | Current Value | Description |
| :--- | :--- | :--- |
| Service Port | `8091` | Configured in `application.properties` |
| API Prefix | `/api/airports` | Base mapping in `AirportController` |
| Database | `H2 In-Memory` | `jdbc:h2:mem:airporttestdb` |
| Schema Strategy | `update` | `spring.jpa.hibernate.ddl-auto` |
| H2 Console Path | `/api/airports/h2-console` | Spring H2 console path |
| Service Registry | `http://localhost:8761/eureka/` | Eureka client registration |

External client base URLs configured in code (resolved through Eureka via `@LoadBalanced` `RestClient.Builder`):

* `route-service`: `http://route-service`

API Gateway routing:

* `/api/airports/**` is routed to `lb://airport-service` by the gateway, so every endpoint of this service is reachable through it.

---

## 3. Documentation Index

This `docs/` directory is organized as follows:

* `api-specification.md`: Real API contract implemented by the controller and services.
* `domain-and-resilience.md`: Domain model, lifecycle rules, integrations, exception mapping, and concurrency notes.
* `architecture/architecture-c4-level3.md`: Component-level architecture aligned with current classes.
* `architecture/architecture-c4-level3.puml`: PlantUML source of the architecture diagram.
* `sequence-diagrams/README.md`: Dynamic behavior narrative for read, write, and inter-service flows.
* `sequence-diagrams/scenario-1-query-airports.puml`: Sequence for read and grouping flows.
* `sequence-diagrams/scenario-2-register-airport.puml`: Sequence for airport registration, status change, and CSV import.
* `sequence-diagrams/scenario-3-route-analytics.puml`: Sequence for flows that depend on `route-service`.

---

## 4. Quick Start

Run locally:

```bash
./mvnw spring-boot:run
```

Open H2 console:

* URL: `http://localhost:8091/api/airports/h2-console`
* JDBC URL: `jdbc:h2:mem:airporttestdb`
* User: `sa`
* Password: empty

There is no Swagger UI: `springdoc-openapi` is not a dependency of this service.
