# Flight Operations Service (`flight-service`) — Technical Documentation

This folder documents the behavior that is currently implemented in code for the `flight-service` microservice.

---

## 1. Scope and Responsibilities

The service is a Spring Boot microservice focused on flight scheduling and route-based reporting.

Current implemented responsibilities:

* Create a scheduled flight using synchronous validations against external services.
* Query scheduled flights for one aircraft registration.
* Produce route utilization report grouped by route with flight count.
* Return standardized API errors via `GlobalExceptionHandler`.

Important implementation notes:

* There is optimistic locking on `Flight` via `@Version`.
* The utilization report currently aggregates `flightCount` per route (not accumulated flight hours).
* There is no maintenance-service integration in the current code.

---

## 2. Runtime and Configuration

| Resource / Parameter | Current Value | Description |
| :--- | :--- | :--- |
| Service Port | `8084` | Configured in `application.properties` |
| API Prefix | `/api/flights` | Base mapping in `FlightController` |
| Database | `H2 In-Memory` | `jdbc:h2:mem:flighttestdb` |
| H2 Console Path | `/api/flight/h2-console` | Spring H2 console path |

External client base URLs configured in code:

* `route-service`: `http://route-service`
* `aircraft-service`: `https://aircraft-service`
* `airport-service`: `http://airport-service`

---

## 3. Documentation Index

This `docs/` directory is organized as follows:

* `api-specification.md`: Real API contract implemented by controller and services.
* `domain-and-resilience.md`: Domain model, scheduling rules, exception mapping, and concurrency notes.
* `architecture/architecture-c4-level3.md`: Component-level architecture aligned with current classes.
* `architecture/architecture-c4-level3.puml`: PlantUML source of the architecture diagram.
* `sequence-diagrams/README.md`: Dynamic behavior narrative for read and write flows.
* `sequence-diagrams/scenario-1-query-flights.puml`: Sequence for read/report flows.
* `sequence-diagrams/scenario-2-schedule-flight.puml`: Sequence for scheduling flow.

---

## 4. Quick Start

Run locally:

```bash
./mvnw spring-boot:run
```

Open H2 console:

* URL: `http://localhost:8084/api/flight/h2-console`
* JDBC URL: `jdbc:h2:mem:flighttestdb`
* User: `sa`
* Password: empty