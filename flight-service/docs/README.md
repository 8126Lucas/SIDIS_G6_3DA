# Flight Operations Service (`flight-service`) — Technical Documentation

Welcome to the central documentation repository for the **Flight Operations Service** (`flight-service`), an essential component of the *AISafe Flight Management System*.

---

## 1. Executive Summary & Service Responsibilities

The `flight-service` is an independent, Spring Boot-based microservice designed to handle core flight operational management. Its primary domain boundaries include:

* **Flight Schedule Management:** Maintaining current and upcoming flight schedules, status transitions (`SCHEDULED`, `DEPARTED`, `ARRIVED`, `CANCELED`), and operational timestamps.
* **Operational Route Metrics:** Aggregating flight utilization statistics, accumulated flight hours, and route popularity analytics.
* **Domain Isolation:** Decoupled architecture using Value Objects (`AircraftReference` and `RouteReference`) to prevent tight JPA/DB dependencies on external services.
* **Concurrency Protection:** Multi-threaded write safety through Optimistic Locking (`@Version`).

---

## 2. Infrastructure & Network Configuration

| Resource / Parameter | Value / Range | Description |
| :--- | :--- | :--- |
| **Primary Instance Port** | `8111` | Default port for local execution |
| **Allocated Port Range** | `8111 - 8120` | Reserved ports for additional instances/clones |
| **API Gateway Port** | `7979` | Entry point for external REST routing |
| **Service Discovery** | `8761` | Eureka Server registration |
| **Database** | In-Memory H2 | Embedded storage for `company_flights` table |

---

## 3. Documentation Directory Index

This `docs/` directory is structured into modular sections to cover architectural, behavioral, and functional aspects of the microservice:

### 📄 1. Architecture & Component View
* **File:** `architecture-c4-level3.md`
* **Focus:** C4 Level 3 Component Diagram in PlantUML. Detailed breakdown of internal controllers, services, JPA repositories, domain models, and external skeleton interfaces.

### 📄 2. REST API Specification
* **File:** `api-specification.md`
* **Focus:** Catalog of all exposed endpoints (`GET /api/flights`, `GET /api/flights/utilization`, etc.), sample JSON payloads, query parameters, and `GlobalExceptionHandler` error schemas.

### 📄 3. Domain Model, Concurrency & Resilience
* **File:** `domain-and-resilience.md`
* **Focus:** Deep dive into Domain-Driven Design (DDD) principles, Optimistic Locking strategy (`@Version`), data bootstrapping via `Bootstrap.java`, and fault tolerance.

### 📁 4. Sequence Diagrams & Dynamic Views
* **Folder:** `sequence-diagrams/`
* **Focus:**
  * `sequence-diagrams/README.md`: Functional walkthrough of runtime interactions.
  * `scenario-1-query-flights.puml`: Sequence diagram for read flows and route utilization reports.
  * `scenario-2-schedule-flight.puml`: Sequence diagram for write flows and external service validations (`aircraft-service`, `route-service`, `maintenance-service`).

---

## 4. Quick Start & Execution Guide

### Local Standalone Run
To run the service locally without full campus infrastructure:

    ./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8111"

### Accessing H2 Console
When running locally, the H2 Database console is available at:
* **URL:** `http://localhost:8111/h2-console`
* **JDBC URL:** `jdbc:h2:mem:flightdb`
* **User:** `sa`
* **Password:** *(empty)*