# AISafe Flight Management System — Data Architecture, CQRS and Asynchronous Synchronisation

This document describes how the services store and synchronise their data in the second iteration, as implemented: the move from SQLite to a central PostgreSQL, the CQRS split between a write store and a Redis read store, and the RabbitMQ events that keep both sides in sync.

Diagram: [`cqrs-data-flow.puml`](cqrs-data-flow.puml).

---

## 1. Why the Persistence Changed

The first iteration gave each service its own embedded database (H2, then SQLite). Iteration 2 runs **two instances of every service on different PCs**. An embedded database is a file or memory area inside one container, so each instance would hold its own copy of the data: an aircraft created through instance 1 would not exist for instance 2, and the copies would drift apart.

The data was therefore split by purpose:

| Store | Role | Why |
| :--- | :--- | :--- |
| **PostgreSQL** (one server on PC5) | Write model (Command side), source of truth | A network database that every instance of a service shares, so all instances see the same data. |
| **Redis** (cluster of 3 nodes per service) | Read model (Query side) | Fast reads spread over several PCs, without loading the write database with queries. |
| **SQLite** (`auth-service` only) | Users and roles | The data is static (seeded users), so a per-instance file is enough. |

---

## 2. Store per Service

| Service | Write side (as implemented) | Read side (as implemented) |
| :--- | :--- | :--- |
| `aircraft-service` | PostgreSQL `aircraft_db` | Redis cluster `redis-aircraft-1..3` |
| `maintenance-service` | PostgreSQL `maintenance_db` | Redis cluster `redis-maintenance-1..3` |
| `auth-service` | SQLite `/data/auth.db`, one file per instance (`./data/auth-service-N`) | — |
| `airport-service` | H2 in memory (code) | — |
| `flight-service` | H2 in memory (code) | — |
| `route-service` | H2 in memory (code) | — |

`postgres-init.sql` also creates `airport_db`, `flight_db` and `route_db`, and `docker-compose.yml` already gives those services a PostgreSQL URL and a Redis cluster (`redis-airport-*`, `redis-flight-*`, `redis-route-*`). Their code has not been migrated yet: the `pom.xml` has only the H2 driver, and there is no Redis or RabbitMQ code. See section 7.

PostgreSQL: image `postgres:15-alpine`, container `postgresql-1`, port `5432`, user `sidis`, data in `./data/postgres`. Hibernate creates the tables (`ddl-auto=update`).

---

## 3. CQRS in Aircraft and Maintenance

Each aggregate has two repositories and two representations:

| Aggregate | Command repository (JPA, PostgreSQL) | Query repository (Spring Data Redis) | Redis hash |
| :--- | :--- | :--- | :--- |
| Aircraft | `AircraftCommandRepository` → `Aircraft` | `AircraftQueryRepository` → `AircraftRedis` | `Aircraft:<registration>` |
| Aircraft model | `AircraftModelCommandRepository` → `AircraftModel` | `AircraftModelQueryRepository` → `AircraftModelRedis` | `AircraftModel:<modelName>` |
| Maintenance record | `MaintenanceRecordCommandRepository` → `MaintenanceRecord` | `MaintenanceRecordQueryRepository` → `MaintenanceRecordRedis` | `MaintenanceRecord:<id>` |
| Maintenance template | `MaintenanceTemplateCommandRepository` → `MaintenanceTemplate` | `MaintenanceTemplateQueryRepository` → `MaintenanceTemplateRedis` | `MaintenanceTemplate:<id>` |

The `*Redis` classes are flat read models: they keep identifiers (model name, aircraft registration, template id) instead of JPA relations.

### 3.1. Which side each use case uses

| Use case | Reads from | Writes to | Publishes |
| :--- | :--- | :--- | :--- |
| `AddModelUseCase` | — | PostgreSQL | `aircraftmodel.event.created` |
| `AircraftModelUpdater` | PostgreSQL | PostgreSQL | `aircraftmodel.event.updated` |
| `AircraftModelSearchService` | Redis | — | — |
| `AddAircraftUseCase` | PostgreSQL (model) | PostgreSQL | `aircraft.event.created` |
| `AircraftLifeCycleUpdaterService` | PostgreSQL | PostgreSQL | `aircraft.event.updated` |
| `AircraftSearchService`, `FuelEfficiencyService`, `FlightSearchService` | Redis | — | — |
| `AddMaintenanceTemplateUseCase` | — | PostgreSQL | `maintenancetemplate.event.created` |
| `AddMaintenanceRecordUseCase` | PostgreSQL (template) | PostgreSQL | `maintenance.event.created` |
| `MaintenanceRecordProgressService` (also serves `GET /records/{id}`) | PostgreSQL | PostgreSQL | `maintenance.event.updated` |
| `MaintenanceTemplateSearchService`, `MaintenanceRecordSearchService`, `MaintenanceReportService`, `MaintenanceAlertService`, `PartsInventoryService` | Redis | — | — |

Commands validate against the write model (for example, the model must exist in PostgreSQL before an aircraft is added). Queries and reports read Redis, with one exception: `GET /api/maintenance/records/{id}` goes through `MaintenanceRecordProgressService` and reads PostgreSQL.

---

## 4. Synchronisation with RabbitMQ

After saving to PostgreSQL, the command publishes a JSON event (`Jackson2JsonMessageConverter`) to a topic exchange. A `@RabbitListener` in the same service (the *projector*) receives it and creates or replaces the Redis hash.

| Exchange (topic) | Routing keys sent | Queue (durable) | Binding | Event | Projector |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `aircraft.exchange` | `aircraft.event.created`, `aircraft.event.updated` | `aircraft.sync.queue` | `aircraft.event.#` | `AircraftSyncEvent` | `AircraftRedisProjector` |
| `aircraftmodel.exchange` | `aircraftmodel.event.created`, `aircraftmodel.event.updated` | `aircraftmodel.sync.queue` | `aircraftmodel.event.#` | `AircraftModelSyncEvent` | `AircraftModelRedisProjector` |
| `maintenance.exchange` | `maintenance.event.created`, `maintenance.event.updated` | `maintenance.sync.queue` | `maintenance.event.#` | `MaintenanceRecordSyncEvent` | `MaintenanceRecordRedisProjector` |
| `maintenancetemplate.exchange` | `maintenancetemplate.event.created` | `maintenancetemplate.sync.queue` | `maintenancetemplate.event.#` | `MaintenanceTemplateSyncEvent` | `MaintenanceTemplateRedisProjector` |

Each event carries the whole read model (for example `AircraftSyncEvent`: registration, model name, status, manufacturing date, operational and flight hours, mean range, features), so the projector does not query PostgreSQL.

### 4.1. Two instances, one queue

Both instances of a service declare and listen to the **same queue**, so RabbitMQ delivers each event to only one of them (competing consumers). This is enough because the Redis cluster is shared: whichever instance projects the event, both instances read the same result.

### 4.2. Consistency

The read side is **eventually consistent**. A command returns as soon as PostgreSQL is updated and the event is published; the query side changes a few milliseconds later. A read sent immediately after a write can still return the old value or `404`.

### 4.3. Brokers

Three RabbitMQ 3.12 nodes are declared (`rabbitmq-1` and `rabbitmq-2` on PC5, `rabbitmq-3` on PC3). Services receive all three in `spring.rabbitmq.addresses` and connect to the first one that answers (3-second connection timeout).

---

## 5. Redis Cluster

Each service with a read model has three `redis:7-alpine` nodes on different PCs:

| Cluster | Nodes (container → host port, PC) |
| :--- | :--- |
| Aircraft | `redis-aircraft-1` → `6379` (PC1), `redis-aircraft-2` → `6380` (PC2), `redis-aircraft-3` → `6381` (PC3) |
| Maintenance | `redis-maintenance-1` → `6382` (PC1), `redis-maintenance-2` → `6383` (PC2), `redis-maintenance-3` → `6384` (PC3) |

The services use the Spring Data Redis cluster client (`spring.data.redis.cluster.nodes`, `max-redirects=3`): keys are spread over the nodes by hash slot, and the client follows redirects to the node that owns a key.

---

## 6. Write and Read Flow (Example: Add an Aircraft)

1. `POST /api/hangar/aircraft` reaches one instance of `aircraft-service` through HAProxy and the gateway.
2. `AddAircraftUseCase` loads the model from PostgreSQL, saves the `Aircraft` and publishes `aircraft.event.created`.
3. RabbitMQ routes the event to `aircraft.sync.queue`; one instance's `AircraftRedisProjector` writes `Aircraft:<registration>` to the Redis cluster.
4. `GET /api/hangar/aircraft/{reg}` on either instance reads the hash from Redis.

---

## 7. Known Gaps (As Implemented)

Found when running the system and reading the code at commit `230065c`:

* **Redis is not started as a cluster.** The Redis containers in `docker-compose.yml` run with the default configuration (`cluster-enabled no`) and nothing runs `redis-cli --cluster create`, while the services use the cluster client. A cluster has to be created by hand before the read side works.
* **Third Redis node address.** `SPRING_REDIS_CLUSTER_NODES` of aircraft and maintenance points the third node to `${IP_PC4}` (`6381` / `6384`), but `redis-aircraft-3` and `redis-maintenance-3` run on PC3.
* **The RabbitMQ nodes are not clustered.** They share an Erlang cookie, but no node joins the others, so they are three separate brokers. Queues and messages are not replicated, and the extra addresses only act as fallbacks.
* **Seeded data is not projected.** Both `Bootstrap` classes save the initial data only in PostgreSQL and publish no events, so the seeded aircraft, models, templates and records do not exist on the read side until they are changed.
* **Lookups by non-key fields return nothing.** Spring Data Redis only answers derived queries on fields marked `@Indexed`, and no read model has one. `findByModelName` (catalog lookup), `findByTemplateId` (template lookup, and the records listed by `GET /api/maintenance/records`, which embed their template), `findByStatus` (ongoing activities, turnaround, alerts) and `countByStatus` (availability summary) therefore find nothing, although the hashes exist; `findById` would read the same hash by its key.
* **Attribute changes are not projected.** `PATCH /api/maintenance/records/{id}` changes the template's attribute in PostgreSQL (by dirty checking) but publishes no event, so `MaintenanceTemplate:<id>` in Redis keeps the old attribute.
* **Adding an aircraft fails.** `HangarController.addAircraft` calls `changeAvailability` before `AddAircraftUseCase`, so it looks for the aircraft before creating it and answers `404 Aircraft not found in database`.
* **Airport, flight and route are not migrated.** Their code still uses H2. The PostgreSQL URL that `docker-compose.yml` passes them needs a driver they do not have, and their Redis containers are not used.
