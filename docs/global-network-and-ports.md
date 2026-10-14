# AISafe Flight Management System — Network Topology, Port Mappings & Security Architecture

This document specifies the global network topology, port allocation matrix, service discovery architecture, routing mechanisms, and security strategies implemented across the **AISafe Flight Management System**.

---

## 1. Network Architecture Overview

The AISafe system is deployed as a distributed microservices platform. Traffic enters the system exclusively through the **API Gateway**, which routes requests dynamically to internal microservices based on paths registered with the **Netflix Eureka Discovery Service**.


---

## 2. Master Port Allocation Matrix

To allow horizontal scaling, cloning, and replica instantiation without port collisions, each service is assigned a dedicated **Port Range** (a base port + 9 replica ports).

| Microservice / Infrastructure | Service Name (`spring.application.name`) | Base Port | Allocated Port Range | Protocol | Access Boundary |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Discovery Service** | `discovery-service` | `8761` | `8761` | HTTP | Internal Infrastructure |
| **API Gateway** | `api-gateway` | `7979` | `7979` (Dev) / `8443` (Prod) | HTTPS / WebFlux | Public Entry Point |
| **Auth Service** | `auth-service` | `8101` | `8101 - 8110` | HTTP | Public (`/auth/**`) & Internal |
| **Aircraft Service** | `aircraft-service` | `8081` | `8081 - 8090` | HTTP | Routed via Gateway |
| **Airport Service** | `airport-service` | `8091` | `8091 - 8100` | HTTP | Routed via Gateway |
| **Flight Operations Service** | `flight-service` | `8111` | `8111 - 8120` | HTTP | Routed via Gateway |
| **Maintenance Service** | `maintenance-service` | `8121` | `8121 - 8130` | HTTP | Routed via Gateway |
| **Route Service** | `route-service` | `8131` | `8131 - 8140` | HTTP | Routed via Gateway |

---

## 3. API Gateway Routing & Path Mapping

The **Spring Cloud Gateway** exposes a unified interface on port `7979`. External clients should never invoke individual microservices directly.

| Request Route Path | Target Microservice | Load Balancing Scheme | Public / Protected |
| :--- | :--- | :--- | :--- |
| `/auth/**` | `auth-service` | `lb://AUTH-SERVICE` | **Public** (Login/Register) |
| `/api/hangar/**` | `aircraft-service` | `lb://AIRCRAFT-SERVICE` | Protected (JWT) |
| `/api/airports/**` | `airport-service` | `lb://AIRPORT-SERVICE` | Protected (JWT) |
| `/api/flights/**` | `flight-service` | `lb://FLIGHT-SERVICE` | Protected (JWT) |
| `/api/maintenance/**` | `maintenance-service` | `lb://MAINTENANCE-SERVICE` | Protected (JWT) |
| `/api/routes/**` | `route-service` | `lb://ROUTE-SERVICE` | Protected (JWT) |

---

## 4. Service Discovery & Dynamic Load Balancing

### Eureka Registration Flow
1. **Startup:** Every microservice instance registers with Eureka (`http://localhost:8761/eureka`) upon startup using its assigned port and instance ID.
2. **Heartbeats:** Services send heartbeat signals every 30 seconds to maintain an active lease.
3. **Client-Side Load Balancing:** Spring Cloud Gateway periodically fetches the service registry. Requests sent to `lb://SERVICE-NAME` are automatically load-balanced across available healthy instances in a round-robin sequence.

---

## 5. Security & Authentication Model

### 5.1. Authentication Flow (JWT)
1. **Authentication Request:** Clients send credentials (`POST /auth/login`) to the API Gateway, which routes the request to `auth-service` (port `8101`).
2. **Token Issuance:** Upon successful validation against `auth_db`, `auth-service` generates a signed **JSON Web Token (JWT)** containing user identity and assigned roles (`ROLE_BACKOFFICE`, `ROLE_ATCC`, `ROLE_TECHNICIAN`, `ROLE_SUPERVISOR`).
3. **Request Authorization:** Subsequent HTTP requests to protected endpoints must include the token in the `Authorization` header:

Authorization: Bearer <JWT_TOKEN_STRING>

### 5.2. Role-Based Access Control (RBAC) Matrix

| Role | Auth | Aircraft | Airport | Flight Ops | Maintenance | Route |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Backoffice Operator** | Admin | Read/Write | Read/Write | Read | Read | Read/Write |
| **ATCC** | User | Read | Read | Read/Write | Read | Read/Write |
| **Maintenance Technician** | User | Read | Read | Read | Read/Write | Read |
| **Maintenance Supervisor**| User | Read | Read | Read | Full Control | Read |

---

## 6. Inter-Service Communication Patterns

Services communicate internally using synchronous REST calls via Spring `RestClient`:

1. **`maintenance-service` -> `aircraft-service`:** Validates aircraft availability and operational status before assigning maintenance logs (`GET /api/hangar/aircraft/{registration}`).
2. **`aircraft-service` -> `flight-service`:** Queries flight hours and route utilization metrics (`GET /api/flights/utilization`).
3. **`flight-service` -> `route-service` / `aircraft-service` / `maintenance-service`:** Validates route existence, aircraft flight range, and maintenance schedules during new flight bookings.