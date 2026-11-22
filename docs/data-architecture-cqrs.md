# AISafe Flight Management System — Data Architecture, CQRS and Asynchronous Synchronisation

This document describes how the services store and synchronise their data in the second iteration, as implemented at commit `8fbc480`: the move from SQLite to a central PostgreSQL, the CQRS split between a write store and a Redis read store, and the RabbitMQ events that keep both sides in sync.

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

| Service | Write side | Read side | Redis nodes (host ports) |
| :--- | :--- | :--- | :--- |
| `aircraft-service` | PostgreSQL `aircraft_db` | Redis hashes `Aircraft`, `AircraftModel` | `6379`, `6380`, `6381` |
| `maintenance-service` | PostgreSQL `maintenance_db` | Redis hashes `MaintenanceRecord`, `MaintenanceTemplate`, `MaintenanceHoursReport` | `6382`, `6383`, `6384` |
| `airport-service` | PostgreSQL `airport_db` | Redis hash `Airport` | `6385`, `6386`, `6387` |
| `flight-service` | PostgreSQL `flight_db` | Redis hash `Flight` | `6388`, `6389`, `6390` |
| `route-service` | PostgreSQL `route_db` | Redis hash `Route` | `6391`, `6392`, `6393` |
| `auth-service` | SQLite `/data/auth.db`, one file per instance (`./data/auth-service-N`) | — | — |

PostgreSQL: image `postgres:15-alpine`, container `postgresql-1`, port `5432`, user `sidis`, data in `./data/postgres`. `postgres-init.sql` creates the five databases and Hibernate creates the tables (`ddl-auto=update`).

---

## 3. Two Implementations of the Same Pattern

All five services follow CQRS (commands write to PostgreSQL and publish an event; a listener projects it into Redis; queries read Redis), but they were built in two different ways.

| | Aircraft, Maintenance | Airport, Flight, Route |
| :--- | :--- | :--- |
| Repositories | `*CommandRepository` (JPA) + `*QueryRepository` (Spring Data Redis) | JPA repository + `*ReadRepository` (Spring Data Redis) |
| Read model | One `*Redis` class per aggregate, copied field by field | One `*ReadModel` record per aggregate, built by a `*ReadModelMapper` |
| Query code | Search, report and alert services read the query repositories | One `*QueryService` per service |
| Event | One event per aggregate with every read-model field (`AircraftSyncEvent`, ...) | `*ChangedEvent` that wraps the whole read model |
| When the event is sent | Inside the command, with `rabbitTemplate.convertAndSend`, **before** the transaction commits | `@TransactionalEventListener(AFTER_COMMIT)` in a `*EventPublisher`, **only after** PostgreSQL commits |
| Out-of-order events | Last event wins | Each read model carries the JPA `@Version`; the projector ignores an event whose version is not newer than the stored one |
| Startup | `Bootstrap` seeds PostgreSQL if the tables are empty, then republishes **every** row on every start (`republishModels`/`republishAircrafts`, `republishTemplates`/`republishRecords`). Maintenance still also publishes while seeding, so on an empty database each seeded row is sent twice (the projector overwrites the same key). | `Bootstrap` seeds if needed, then republishes **every** row on every start |
| Listener | `@RabbitListener` with default concurrency | `@RabbitListener(concurrency = "1")` |

In both styles Redis is rebuilt from PostgreSQL on every start, so a read side that lost data or holds stale values recovers after a restart.

Sending after commit and checking versions makes the second approach safer: a rolled-back command never reaches Redis, and a late, older event cannot overwrite a newer one.

---

## 4. CQRS per Service

### 4.1. Which side each use case uses

| Service | Commands (PostgreSQL + event) | Queries (Redis) |
| :--- | :--- | :--- |
| Aircraft | `AddModelUseCase`, `AircraftModelUpdater`, `AddAircraftUseCase`, `AircraftLifeCycleUpdaterService` | `AircraftModelSearchService`, `AircraftSearchService`, `FuelEfficiencyService`, `FlightSearchService` |
| Maintenance | `AddMaintenanceTemplateUseCase`, `AddMaintenanceRecordUseCase`, `MaintenanceRecordProgressService` | `MaintenanceTemplateSearchService`, `MaintenanceRecordSearchService`, `MaintenanceReportService`, `MaintenanceAlertService`, `PartsInventoryService` |
| Airport | `AirportService` (create, update details, status, certifications; the CSV import parses the file with `AirportCsvService` and creates each airport through `AirportService`) | `AirportQueryService` (by IATA, search, grouping, busiest airports, routes per airport) |
| Flight | `FlightService` (schedule a flight) | `FlightQueryService` (all flights, flights per aircraft, route utilization report) |
| Route | `RouteService` (create, update) | `RouteQueryService` (by id, details, history, per airport, search, active routes, total distance, alternatives, compatible routes), `RouteExportService` (GeoJSON, KML) |

Commands validate against the write model (for example, the model must exist in PostgreSQL before an aircraft is added). One read still uses the write side: `GET /api/maintenance/records/{id}` goes through `MaintenanceRecordProgressService` and reads PostgreSQL.

### 4.2. RabbitMQ exchanges and queues

All exchanges are topic exchanges, all queues are durable, and messages are JSON (`Jackson2JsonMessageConverter`).

| Exchange | Routing keys | Queue | Event | Projector |
| :--- | :--- | :--- | :--- | :--- |
| `aircraft.exchange` | `aircraft.event.created`, `.updated` | `aircraft.sync.queue` | `AircraftSyncEvent` | `AircraftRedisProjector` |
| `aircraftmodel.exchange` | `aircraftmodel.event.created`, `.updated` | `aircraftmodel.sync.queue` | `AircraftModelSyncEvent` | `AircraftModelRedisProjector` |
| `maintenance.exchange` | `maintenance.event.created`, `.updated` | `maintenance.sync.queue` | `MaintenanceRecordSyncEvent` | `MaintenanceRecordRedisProjector` |
| `maintenancetemplate.exchange` | `maintenancetemplate.event.created`, `.updated` (attribute change) | `maintenancetemplate.sync.queue` | `MaintenanceTemplateSyncEvent` | `MaintenanceTemplateRedisProjector` |
| `airport.exchange` | `airport.event.updated` | `airport.sync.queue` | `AirportChangedEvent` | `AirportRedisProjector` |
| `flight.exchange` | `flight.event.updated` | `flight.sync.queue` | `FlightChangedEvent` | `FlightRedisProjector` |
| `route.exchange` | `route.event.updated` | `route.sync.queue` | `RouteChangedEvent` | `RouteRedisProjector` |

Events stay inside their own service: no service consumes another service's events. Calls between services are still synchronous HTTP (see `global-network-and-ports.md`, section 8).

### 4.3. Two instances, one queue

Both instances of a service declare and listen to the **same queue**, so RabbitMQ delivers each event to only one of them (competing consumers). This is enough because the Redis cluster is shared: whichever instance projects the event, both instances read the same result.

### 4.4. Consistency

The read side is **eventually consistent**. A command returns as soon as PostgreSQL is updated and the event is published; the query side changes a few milliseconds later. A read sent immediately after a write can still return the old value or `404`.

### 4.5. Indexes

Spring Data Redis answers queries on non-key fields only for fields marked `@Indexed`:

| Read model | Indexed fields |
| :--- | :--- |
| `AircraftRedis` | `modelName`, `status`, `manufacturingDate`, `seatingPack`, `certifications` |
| `AircraftModelRedis` | `modelName` |
| `MaintenanceRecordRedis` | `aircraftRegistration`, `maintenanceTemplateId`, `startDate`, `endDate`, `durationHours`, `status` |
| `MaintenanceTemplateRedis` | `templateId`, `name`, `operation`, `attribute` |
| `AirportReadModel` | `status`, `city`, `country` |
| `FlightReadModel` | `aircraftRegistrationNumber` |
| `RouteReadModel` | `status`, `iataCode` |

Two queries no longer rely on Redis queries and filter in memory instead: `countByAvailability` (aircraft status summary) reads every `Aircraft` hash and counts by status, and the maintenance record search (`GET /api/maintenance/records`) reads every `MaintenanceRecord`, filters by registration, attribute (through the template) and dates, sorts by id (newest first) and builds the page itself. Both work, but read the whole hash set on every call.

---

## 5. Brokers and Redis Clusters

**RabbitMQ.** Three RabbitMQ 3.12 nodes are declared (`rabbitmq-1` and `rabbitmq-2` on PC5, `rabbitmq-3` on PC3). Services receive them in `spring.rabbitmq.addresses` and connect to the first one that answers.

**Redis.** Each service has three `redis:7-alpine` nodes on different PCs. The cluster settings (`spring.data.redis.cluster.nodes`, `max-redirects=3`) moved from `application.properties` to `application-docker.properties`, and `docker-compose.yml` starts every service with `SPRING_PROFILES_ACTIVE=docker`, so in Docker the services use the cluster client and keys are spread over the nodes by hash slot. Without the `docker` profile a service uses one standalone Redis (`localhost:6379` by default; the `test` profile sets one port per service: aircraft `6379`, maintenance `6382`, airport `6385`, flight `6388`, route `6391`).

| Cluster | Nodes (container → host port, PC) |
| :--- | :--- |
| Aircraft | `redis-aircraft-1..3` → `6379` (PC1), `6380` (PC2), `6381` (PC3) |
| Maintenance | `redis-maintenance-1..3` → `6382` (PC1), `6383` (PC2), `6384` (PC3) |
| Airport | `redis-airport-1..3` → `6385` (PC4), `6386` (PC5), `6387` (PC5) |
| Flight | `redis-flight-1..3` → `6388` (PC4), `6389` (PC5), `6390` (PC5) |
| Route | `redis-route-1..3` → `6391` (PC4), `6392` (PC5), `6393` (PC5) |

---

## 6. Write and Read Flow (Example: Add an Aircraft)

1. `POST /api/hangar/aircraft` reaches one instance of `aircraft-service` through HAProxy and the gateway.
2. `AddAircraftUseCase` loads the model from PostgreSQL, saves the `Aircraft` and publishes `aircraft.event.created`.
3. `AircraftLifeCycleUpdaterService.changeAvailability` sets the requested availability and publishes `aircraft.event.updated`.
4. RabbitMQ routes the events to `aircraft.sync.queue`; one instance's `AircraftRedisProjector` writes `Aircraft:<registration>` to the Redis cluster.
5. `GET /api/hangar/aircraft/{reg}` on either instance reads the hash from Redis.

---

## 7. Known Gaps (As Implemented)

Checked by running commit `8fbc480` on one machine with the `test` profile (PostgreSQL, RabbitMQ and one standalone Redis per service in Docker; the eight modules as jars). Writes reach Redis in all five services, and `Bootstrap` rebuilds Redis on every start. The remaining gaps:

* **A failing projection is retried forever.** The listeners have no error handling and no dead-letter queue, so an event that cannot be projected goes back to the queue endlessly. Seen with `PATCH /api/maintenance/records/{id}` and `"attribute": null`: PostgreSQL stores `null`, `MaintenanceTemplateRedisProjector` throws `No enum constant ...MaintenanceAttribute.null`, and the message was redelivered about a thousand times per second until the service was stopped. Other events on the same queue were still projected. Because `Bootstrap` republishes every template on start, the loop comes back on every restart while the template keeps `null`.
* **Redis is not started as a cluster.** The Redis containers in `docker-compose.yml` run with the default configuration (`cluster-enabled no`) and nothing runs `redis-cli --cluster create`, while the services use the cluster client. A cluster has to be created by hand before the read side works.
* **The RabbitMQ nodes are not clustered.** They share an Erlang cookie, but no node joins the others, so they are three separate brokers. Queues and messages are not replicated, and the extra addresses only act as fallbacks.
* **`route-service` test profile uses the aircraft database.** `route-service/src/main/resources/application-test.properties` points to `jdbc:postgresql://localhost:5432/aircraft_db`, so in CI the route tables are created in `aircraft_db` and `route_db` stays empty (confirmed: `company_routes` is in `aircraft_db`).
