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
* In the 5-PC deployment `discovery-node1` runs on PC1 and `discovery-node2` on PC3; each one takes the other's URL from `EUREKA_URL_2` / `EUREKA_URL_1`, so they replicate across machines (see `registry-topology.md`, section 3).
* Both nodes expose `/actuator/prometheus` for Prometheus, which also uses `discovery-node1` to find its scrape targets.

---

## 2. Runtime and Configuration

| Profile | Port | Registers with / Fetches from | Use |
| :--- | :--- | :--- | :--- |
| default | `8761` | Nothing (`register-with-eureka=false`, `fetch-registry=false`) | Standalone, used by the module test |
| `node1` | `8761` | `${EUREKA_URL_2}` (node 2) | First peer, PC1 (CI uses it alone) |
| `node2` | `8762` | `${EUREKA_URL_1}` (node 1) | Second peer, PC3 |

* Application name: `discovery-service` (`application.properties`, shared by all profiles).
* `EUREKA_URL_1` / `EUREKA_URL_2` must be set on every PC (`http://<IP_PC1>:8761/eureka/`, `http://<IP_PC3>:8762/eureka/`); `docker-compose.yml` passes them to the containers.
* Docker (`docker-compose.yml`): `discovery-node1` (profile `pc1`, `8761:8761`) registers with `EUREKA_INSTANCE_IP_ADDRESS=${IP_PC1}` and instance id `${IP_PC1}:discovery-node1:8761`; `discovery-node2` (profile `pc3`, `8762:8762`) with `${IP_PC3}`. Same image, profile chosen with `SPRING_PROFILES_ACTIVE`.

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

Two peer nodes (two terminals, same machine):

```bash
export EUREKA_URL_1=http://localhost:8761/eureka/ EUREKA_URL_2=http://localhost:8762/eureka/
./mvnw spring-boot:run -Dspring-boot.run.profiles=node1
./mvnw spring-boot:run -Dspring-boot.run.profiles=node2
```

In the lab: `docker compose --profile pc1 up -d` on PC1 and `docker compose --profile pc3 up -d` on PC3.

Then open `http://localhost:8761` (and `http://localhost:8762`) to see the dashboard.

---

## 5. Tests

`DiscoveryServiceApplicationTests` starts the server on a random port with the default profile and checks that `GET /eureka/apps` returns `200` with an `applications` body. It needs no other service running.
