# AISafe Flight Management System — Global Architecture & System Documentation

Welcome to the central system architecture repository for the **AISafe Flight Management System**. 

This `docs/` folder contains the high-level system architectural blueprints, C4 level models, inter-service communications, security topologies, and links to individual microservice documentations.

---

## 1. System Overview & Bounded Contexts

The **AISafe Flight Management System** is a distributed, microservice-based platform built on **Java 21** and **Spring Boot 3.x**. It provides full-lifecycle support for airline operations, including fleet management, airport infrastructure, maintenance records, route definitions, authentication, and flight scheduling.

### Core Ecosystem Components

* **API Gateway (`7979`, HTTPS):** Central entry point using Spring Cloud Gateway (WebFlux). Validates the JWT and routes requests to the services through Eureka.
* **Discovery Service (`8761` / `8762`):** Two peer Spring Cloud Netflix Eureka Server nodes for the service registry and client-side load balancing.
* **Auth Service (`8101 - 8110`):** Manages user accounts, credentials, and JWT token issuance.
* **Aircraft Service (`8081 - 8090`):** Manages aircraft models, operational specifications, and fleet hangar instances.
* **Airport Service (`8091 - 8100`):** Manages airport infrastructure, runway specs, and OpenCSV data imports.
* **Maintenance Service (`8121 - 8130`):** Manages checklists, maintenance logs, and scheduled aircraft overhauls.
* **Route Service (`8131 - 8140`):** Manages flight routes, network distances, and waypoint histories.
* **Flight Operations Service (`8111 - 8120`):** Handles flight schedules, route assignments, real-time status transitions, and utilization metrics.

---

## 2. Global Architecture Diagrams Directory

This root `docs/` directory is structured to host the global system views and cross-cutting specifications:

``
docs/
├── README.md                              # This global index and system blueprint
├── system-context-c4-level1.puml          # C4 Level 1: System Context Diagram (PlantUML)
├── container-diagram-c4-level2.puml       # C4 Level 2: Container Architecture Diagram (PlantUML)
├── global-logical-view-level1.puml        # UML Logical View Level 1 (Socket/Lollipop P2P Forwarding)
├── global-network-and-ports.md            # Network topology, ports, routing, security, inter-service calls
├── ci-cd-pipelines.md                     # GitHub Actions CI (tests on the running system) and CD (Docker images)
└── postman/
    ├── AISafe-Global-System.postman_collection.json  # Global Postman integration collection
    └── SIDIS HTTP Tests.postman_collection.json      # HTTP test collection
``

---

## 3. High-Level Architectural Blueprints

### 3.1. C4 Level 1 — System Context Diagram
* **File:** `system-context-c4-level1.puml`
* **Scope:** Defines the system boundary of the AISafe platform and its interactions with external actors (*Backoffice Operator*, *ATCC*, *Maintenance Technician*, *Maintenance Supervisor*) via REST/HTTPS clients (Postman).

### 3.2. C4 Level 2 — Container Diagram
* **File:** `container-diagram-c4-level2.puml`
* **Scope:** Illustrates the internal container structure, showing how the **API Gateway** routes traffic to microservices, how services register with the two **Eureka Discovery Service** nodes, their dedicated **H2 In-Memory Databases**, and inter-service REST interactions (see `global-network-and-ports.md`, section 6).

### 3.3. CI/CD
* **File:** `ci-cd-pipelines.md`
* **Scope:** The CI workflow starts the whole system on the runner and runs the end-to-end tests; the CD workflow publishes Docker images to the GitHub Container Registry after a successful CI run.

---

## 4. Master Network Topology & Allocated Service Ports

All microservices run within designated port ranges to facilitate cloning and scaling:

| Service / Infrastructure Component | Port Range / Base Port | Protocol / Technology |
| :--- | :--- | :--- |
| **Eureka Discovery Service** | `8761` / `8762` | HTTP / Eureka Server |
| **API Gateway** | `7979` | HTTPS / WebFlux |
| **Aircraft Service** | `8081 - 8090` | HTTP / Spring Web / JPA |
| **Airport Service** | `8091 - 8100` | HTTP / Spring Web / OpenCSV |
| **Authentication Service** | `8101 - 8110` | HTTP / Spring Security / JWT |
| **Flight Operations Service** | `8111 - 8120` | HTTP / Spring Web / JPA |
| **Maintenance Service** | `8121 - 8130` | HTTP / Spring Web / JPA |
| **Route Service** | `8131 - 8140` | HTTP / Spring Web / JPA |

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
- [ ] **UML Logical View — Level 1** (`.puml` + `.png`) showing system component interfaces (*Lollipop / Socket*) and peer-to-peer forwarding.
- [ ] **Master Network & Security Topology Document** (`04-global-network-and-ports.md`) detailing JWT authentication flow, Eureka load balancing, and port allocations.
- [ ] **Global Postman Integration Test Collection** (`postman/AISafe-Global-System.postman_collection.json`) featuring end-to-end user journeys across all microservices.