# Discovery Service — Registry Topology (As Implemented)

This document describes how the Eureka nodes are connected and how the other modules use them.

---

## 1. Peer Nodes

With the `node1` and `node2` profiles, each node registers with the other and fetches its registry, so a registration received by one node is replicated to the other.

Diagram source:

* `architecture/registry-topology.puml`

---

## 2. Eureka Clients

Every other module is a Eureka client. Clients send their registration and heartbeats to the first reachable URL in their `defaultZone` list and move to the next one only if it fails; the other node receives the data through peer replication.

| Module | Registered Name | `defaultZone` (local `application.properties`) |
| :--- | :--- | :--- |
| `APIgateway` | `APIGATEWAY` | `8761`, `8762` |
| `auth-service` | `AUTH-SERVICE` | `8761`, `8762` |
| `aircraft-service` | `AIRCRAFT-SERVICE` | `8761`, `8762` |
| `maintenance-service` | `MAINTENANCE-SERVICE` | `8761`, `8762` |
| `airport-service` | `AIRPORT-SERVICE` | `8761` only |
| `route-service` | `ROUTE-SERVICE` | `8761` only |
| `flight-service` | `FLIGHT-SERVICE` | `8761` only |

* `airport-service`, `route-service`, `flight-service` and `maintenance-service` set `eureka.instance.prefer-ip-address=true` (they register their IP instead of the hostname).
* The gateway reads the registry to resolve `lb://<service>` routes; see `APIgateway/docs/routing-and-security.md`.

---

## 3. Environments

| Environment | Nodes Running | Notes |
| :--- | :--- | :--- |
| Local (default profile) | One node on `8761` | No replication. Clients that list `8762` log connection errors for it but keep working. |
| Local (`node1` + `node2`) | `8761` and `8762` | Replication works because both nodes use `localhost`. Airport, route and flight services cannot fail over to `8762`. |
| Docker Compose | `discovery-node1`, `discovery-node2` | All clients get both nodes through `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`. The nodes themselves keep `localhost:8761` / `localhost:8762` as their peer URL, which inside a container points to the container itself, so **the nodes do not replicate to each other**. If `discovery-node1` stops, `discovery-node2` starts with an empty registry until clients re-register. |
| CI (`ci.yml`) | `node1` only | `node2` is never started, so `node1` logs replication errors to `localhost:8762`; registration still works. |

Fixing the Docker case would require setting the peer URL of each node to the other container (for example `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://discovery-node2:8762/eureka/` on `discovery-node1`).
