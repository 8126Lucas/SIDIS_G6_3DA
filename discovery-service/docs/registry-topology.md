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

Every client module now uses the same configuration: `defaultZone=${EUREKA_URL_1},${EUREKA_URL_2}` and `prefer-ip-address=true`.

| Module | Registered Name | Instances (PC) |
| :--- | :--- | :--- |
| `APIgateway` | `APIGATEWAY` | `7979` (PC1), `7980` (PC4) |
| `auth-service` | `AUTH-SERVICE` | `8101` (PC2), `8102` (PC3) |
| `aircraft-service` | `AIRCRAFT-SERVICE` | `8081` (PC2), `8082` (PC5) |
| `maintenance-service` | `MAINTENANCE-SERVICE` | `8121` (PC2), `8122` (PC3) |
| `airport-service` | `AIRPORT-SERVICE` | `8091` (PC3), `8092` (PC4) |
| `route-service` | `ROUTE-SERVICE` | `8131` (PC4), `8132` (PC5) |
| `flight-service` | `FLIGHT-SERVICE` | `8111` (PC4), `8112` (PC5) |

* In Docker each instance sets `EUREKA_INSTANCE_IP_ADDRESS=${IP_PC<n>}` (the LAN IP of its PC, not the container IP) and `EUREKA_INSTANCE_INSTANCE_ID=<ip>:<service>:<port>`. That is what lets an instance on one PC call an instance on another.
* The gateway reads the registry to resolve `lb://<service>` routes; see `APIgateway/docs/routing-and-security.md`.
* Prometheus (PC1) uses the registry of `discovery-node1` as its list of scrape targets.

---

## 3. Environments

| Environment | Nodes Running | Notes |
| :--- | :--- | :--- |
| Local (default profile) | One node on `8761` | No replication. Clients need `EUREKA_URL_1` (and `EUREKA_URL_2`, which can point to the same node). |
| Local (`node1` + `node2`) | `8761` and `8762` | Replication works when `EUREKA_URL_1` / `EUREKA_URL_2` point to `localhost:8761` / `localhost:8762`. |
| Lab, 5 PCs (`docker-compose.yml`) | `discovery-node1` on PC1, `discovery-node2` on PC3 | `discovery-node1` gets `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=${EUREKA_URL_2}` and `discovery-node2` gets `${EUREKA_URL_1}`, so each node replicates to the other over the LAN. If one PC goes down, clients fall back to the other node, which already holds the registry. |
| CI (`ci.yml`) | `node1` only | The workflow does not set `EUREKA_URL_1` / `EUREKA_URL_2`, which every module now reads (see `docs/ci-cd-pipelines.md`, section 1.4). |

`discovery-node2` sets `EUREKA_INSTANCE_HOSTNAME=discovery-node2` and no `EUREKA_INSTANCE_PREFER_IP_ADDRESS`; it still registers by IP because `application-node2.properties` sets `prefer-ip-address=true`.
