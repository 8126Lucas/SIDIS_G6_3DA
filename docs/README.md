# AISafe Flight Management System — Global Architecture & System Documentation

Welcome to the central system architecture repository for the **AISafe Flight Management System**. 

This `docs/` folder contains the high-level system architectural blueprints, C4 level models, inter-service communications, security topologies, and links to individual microservice documentations.

---

## 1. System Overview & Bounded Contexts

The **AISafe Flight Management System** is a distributed, microservice-based platform built on **Java 21** and **Spring Boot 3.x**. It provides full-lifecycle support for airline operations, including fleet management, airport infrastructure, maintenance records, route definitions, authentication, and flight scheduling.

In iteration 2 the system runs on **five PCs**, with **two instances of every component** spread over them, a load balancer in front of the gateways, and a shared data layer for the services migrated to CQRS.

### Core Ecosystem Components

* **HAProxy (`443`, PC1):** Single entry point (`https://192.168.137.1`). Balances connections between the two gateways (round robin) and stops using a gateway that does not answer.
* **API Gateway (`7979` PC1, `7980` PC4, HTTPS):** Spring Cloud Gateway (WebFlux). Validates the JWT and routes requests to the services through Eureka.
* **Discovery Service (`8761` PC1, `8762` PC3):** Two peer Spring Cloud Netflix Eureka Server nodes for the service registry and client-side load balancing.
* **Auth Service (`8101 - 8110`):** Manages user accounts, credentials, and JWT token issuance. SQLite file per instance.
* **Aircraft Service (`8081 - 8090`):** Manages aircraft models, operational specifications, and fleet hangar instances. CQRS: PostgreSQL + Redis + RabbitMQ.
* **Airport Service (`8091 - 8100`):** Manages airport infrastructure, runway specs, and OpenCSV data imports.
* **Maintenance Service (`8121 - 8130`):** Manages checklists, maintenance logs, and scheduled aircraft overhauls. CQRS: PostgreSQL + Redis + RabbitMQ.
* **Route Service (`8131 - 8140`):** Manages flight routes, network distances, and waypoint histories.
* **Flight Operations Service (`8111 - 8120`):** Handles flight schedules, route assignments, real-time status transitions, and utilization metrics.
* **Data and messaging:** PostgreSQL 15 (PC5) as write store, one Redis cluster per CQRS service as read store, RabbitMQ 3.12 for the synchronisation events.
* **Monitoring:** Prometheus (`9090`, PC1) scrapes the Spring Boot Actuator metrics of the instances registered in Eureka.

---

## 2. Global Architecture Diagrams Directory

This root `docs/` directory is structured to host the global system views and cross-cutting specifications:

``
docs/
├── README.md                              # This global index and system blueprint
├── system-context-c4-level1.puml          # C4 Level 1: System Context Diagram (PlantUML)
├── container-diagram-c4-level2.puml       # C4 Level 2: Container Architecture Diagram (PlantUML)
├── global-logical-view-level1.puml        # UML Logical View Level 1 (Socket/Lollipop P2P Forwarding)
├── physical-view-deployment.puml          # Physical View: containers per PC, ports, entry point, data stores
├── domain-model-ddd.puml                  # UML class diagram of the domain model per bounded context (DDD)
├── global-network-and-ports.md            # HAProxy, PC distribution, ports, routing, security, inter-service calls
├── data-architecture-cqrs.md              # PostgreSQL, CQRS with Redis, RabbitMQ synchronisation, known gaps
├── cqrs-data-flow.puml                    # Sequence: command -> PostgreSQL -> RabbitMQ -> Redis -> query
├── ci-cd-pipelines.md                     # GitHub Actions CI (tests on the running system) and CD (Docker images)
└── postman/
    ├── README.md                                     # How to run the collection and how it is organised
    ├── AISafe-Global-System.postman_collection.json  # Global Postman integration collection
    ├── AISafe-Local.postman_environment.json         # Postman environment for the local system
    └── SIDIS HTTP Tests.postman_collection.json      # Previous HTTP test collection
``

---

## 3. High-Level Architectural Blueprints

### 3.1. C4 Level 1 — System Context Diagram
* **File:** `system-context-c4-level1.puml`
* **Scope:** Defines the system boundary of the AISafe platform and its interactions with external actors (*Backoffice Operator*, *ATCC*, *Maintenance Technician*, *Maintenance Supervisor*) via REST/HTTPS clients (Postman).

### 3.2. C4 Level 2 — Container Diagram
* **File:** `container-diagram-c4-level2.puml`
* **Scope:** Illustrates the internal container structure: **HAProxy** in front of the two **API Gateway** instances, the routing to the microservices, the two **Eureka** nodes, the write store (**PostgreSQL**), read store (**Redis cluster**) and message broker (**RabbitMQ**) of the CQRS services, the SQLite of `auth-service`, the H2 databases of the services not yet migrated, and **Prometheus**. Inter-service REST calls are listed in `global-network-and-ports.md`, section 8.

### 3.3. UML Logical View — Level 1
* **File:** `global-logical-view-level1.puml`
* **Scope:** Each component with two `<<instance>>` clones, its provided REST interface (lollipop) and the interfaces it requires from other components (socket). Shows the peer forwarding between clones as implemented: it works in Aircraft Service; Maintenance Service forwards to the wrong service id (`aircraft-service`); Airport, Flight and Route have a `PeerForwarder` class that no controller calls.

### 3.4. Physical View — Deployment
* **File:** `physical-view-deployment.puml`
* **Scope:** The five PCs and the containers each one starts (`docker compose --profile pc<n>`): two instances of every component, HAProxy and Prometheus on PC1, PostgreSQL and two RabbitMQ nodes on PC5, and the Redis nodes spread over the PCs, with their host ports.

### 3.5. Data Architecture — CQRS and Synchronisation
* **Files:** `data-architecture-cqrs.md`, `cqrs-data-flow.puml`
* **Scope:** Why SQLite was replaced by a central PostgreSQL, how `aircraft-service` and `maintenance-service` split commands (PostgreSQL) from queries (Redis cluster), the RabbitMQ exchanges and queues that project each write into Redis, and the gaps found in the current implementation.

### 3.6. Domain Model (DDD) — UML Class Diagram
* **File:** `domain-model-ddd.puml`
* **Scope:** One package per bounded context (Flight Operations, Maintenance, Route, Aircraft, Identity and Access, Airport) with its aggregate roots, entities and value objects as mapped in JPA. Contexts reference each other only by identifier (registration number, IATA code, route id, usernames), resolved over HTTP/REST.

### 3.7. CI/CD
* **File:** `ci-cd-pipelines.md`
* **Scope:** The CI workflow compiles first (fail-fast), then starts the system on the runner for the Java end-to-end tests and the Postman tests; the CD workflow publishes Docker images to the GitHub Container Registry after a successful CI run.

---

## 4. Master Network Topology & Allocated Service Ports

All microservices run within designated port ranges to facilitate cloning and scaling. Clients only use HAProxy (`443`); the other ports are used between PCs.

| Service / Infrastructure Component | Ports (instance 1 / instance 2) | Protocol / Technology |
| :--- | :--- | :--- |
| **HAProxy** | `443` (PC1), stats `8404` | TCP (TLS pass-through) / HTTP |
| **API Gateway** | `7979` (PC1) / `7980` (PC4) | HTTPS / WebFlux |
| **Eureka Discovery Service** | `8761` (PC1) / `8762` (PC3) | HTTP / Eureka Server |
| **Aircraft Service** | `8081` / `8082` (range `8081 - 8090`) | HTTP / Spring Web / JPA / Redis / AMQP |
| **Airport Service** | `8091` / `8092` (range `8091 - 8100`) | HTTP / Spring Web / OpenCSV |
| **Authentication Service** | `8101` / `8102` (range `8101 - 8110`) | HTTP / Spring Security / JWT |
| **Flight Operations Service** | `8111` / `8112` (range `8111 - 8120`) | HTTP / Spring Web / JPA |
| **Maintenance Service** | `8121` / `8122` (range `8121 - 8130`) | HTTP / Spring Web / JPA / Redis / AMQP |
| **Route Service** | `8131` / `8132` (range `8131 - 8140`) | HTTP / Spring Web / JPA |
| **PostgreSQL** | `5432` (PC5) | JDBC |
| **RabbitMQ** | `5672`, `5673` (PC5), `5674` (PC3) | AMQP |
| **Redis** | `6379 - 6393` (3 nodes per service) | Redis cluster |
| **Prometheus** | `9090` (PC1) | HTTP |

Placement of every container per PC: `global-network-and-ports.md`, section 3.

---

## 5. Individual Microservice Documentation

For specific component-level architecture, REST API endpoints, sequence diagrams, and domain implementation details, refer to each microservice's local `docs/` directory:

* 📄 [Flight Operations Service Documentation](../flight-service/docs/README.md)
* 📄 [Aircraft Service Documentation](../aircraft-service/docs/README.md)
* 📄 [Airport Service Documentation](../airport-service/docs/README.md)
* 📄 [Maintenance Service Documentation](../maintenance-service/docs/README.md)
* 📄 [Route Service Documentation](../route-service/docs/README.md)
* 📄 [Auth Service Documentation](../auth-service/docs/README.md)
* 📄 [API Gateway Documentation](../APIgateway/docs/README.md)
* 📄 [Discovery Service Documentation](../discovery-service/docs/README.md)

---

## 6. Target Documentation Checklist for Final Delivery

To ensure a complete and successful project submission, the root `/docs/` folder should contain the following items by final delivery:

- [x] **C4 Level 1 — System Context Diagram** (`.puml` + `.png`)
- [x] **C4 Level 2 — Container Diagram** (`.puml` + `.png`)
- [x] **UML Logical View — Level 1** (`.puml`) showing system component interfaces (*Lollipop / Socket*), `<<instance>>` clones and peer-to-peer forwarding.
- [x] **Physical View — Deployment Diagram** (`.puml`) showing the containers per PC, ports and multiple instances per component.
- [x] **Data Architecture — CQRS** (`data-architecture-cqrs.md` + `cqrs-data-flow.puml`) with the PostgreSQL decision, the Redis read model and the RabbitMQ synchronisation.
- [x] **Domain Model (DDD) — UML Class Diagram** (`.puml`) with aggregates, entities and value objects per bounded context.
- [x] **Master Network & Security Topology Document** (`global-network-and-ports.md`) detailing the HAProxy entry point, PC distribution, JWT authentication flow, Eureka load balancing, and port allocations.
- [x] **Global Postman Integration Test Collection** (`postman/AISafe-Global-System.postman_collection.json`) featuring end-to-end user journeys across all microservices.