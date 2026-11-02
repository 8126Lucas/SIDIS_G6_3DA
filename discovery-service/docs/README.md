# Discovery Service (`discovery-service`) — Technical Documentation

This folder documents the behavior that is currently implemented in code for the `discovery-service` module.

---

## 1. Scope and Responsibilities

The service is a Netflix Eureka Server (`@EnableEurekaServer`). It has no business logic, database or controllers of its own; all behavior comes from Spring Cloud Netflix configuration.

Current implemented responsibilities:

* Keep the registry of running instances of the gateway and the six business services.
* Serve that registry to the gateway (`lb://` routing) and to services that call each other through Eureka.
* Run as a single standalone node (default profile) or as two peer nodes that replicate to each other (`node1` / `node2` profiles).
* Expose the Eureka dashboard and REST API.

Important implementation notes:

* There is no security dependency: the dashboard and the `/eureka/**` API are open to anyone who can reach the port.
* Eureka defaults are kept (30 s heartbeats, 90 s lease expiration, self-preservation enabled).
* In Docker the two nodes cannot replicate to each other (see `registry-topology.md`, section 3).

---

## 2. Runtime and Configuration

| Profile | Port | Registers with / Fetches from | Use |
| :--- | :--- | :--- | :--- |
| default | `8761` | Nothing (`register-with-eureka=false`, `fetch-registry=false`) | Standalone, used by the module test |
| `node1` | `8761` | `http://localhost:8762/eureka/` | First peer (CI and Docker) |
| `node2` | `8762` | `http://localhost:8761/eureka/` | Second peer (Docker) |

* Application name: `discovery-service` (`application.properties`, shared by all profiles).
* `eureka.instance.hostname`: `localhost` in both node profiles; overridden in Docker with `EUREKA_INSTANCE_HOSTNAME=discovery-node1` / `discovery-node2`.
* Docker (`docker-compose.yml`): containers `discovery-node1` (`8761:8761`) and `discovery-node2` (`8762:8762`), same image, profile chosen with `SPRING_PROFILES_ACTIVE`.

Useful endpoints (HTTP):

| Endpoint | Description |
| :--- | :--- |
| `GET /` | Eureka dashboard (registered instances, replicas, lease status) |
| `GET /eureka/apps` | Full registry (XML by default, JSON with `Accept: application/json`) |
| `GET /eureka/apps/{APP-NAME}` | Instances of one application, e.g. `AIRCRAFT-SERVICE` |

---

## 3. Documentation Index

This `docs/` directory is organized as follows:

* `registry-topology.md`: Peer nodes, which clients register where, and environment differences (local, Docker, CI).
* `architecture/registry-topology.puml`: PlantUML source of the topology diagram.
* `sequence-diagrams/README.md`: Dynamic behavior of registration and lookup.
* `sequence-diagrams/scenario-1-register-and-resolve.puml`: Sequence for a service registering and the gateway resolving it.

---

## 4. Quick Start

Single node:

```bash
./mvnw spring-boot:run
```

Two peer nodes (two terminals):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=node1
./mvnw spring-boot:run -Dspring-boot.run.profiles=node2
```

Then open `http://localhost:8761` (and `http://localhost:8762`) to see the dashboard.

---

## 5. Tests

`DiscoveryServiceApplicationTests` starts the server on a random port with the default profile and checks that `GET /eureka/apps` returns `200` with an `applications` body. It needs no other service running.
