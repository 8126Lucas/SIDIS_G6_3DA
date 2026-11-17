# Maintenance Service (`maintenance-service`) — C4 Level 3 Component View (As Implemented)

This document describes the current internal component architecture at C4 Level 3 based on implemented classes.

---

## 1. Current Component Responsibilities

The `maintenance-service` currently exposes two controllers and the following main application services:

* **`MaintenanceSecurityConfig`**
   * Stateless `SecurityFilterChain` with `gatewaySecretFilter`, which rejects requests without `X-Gateway-Secret` (`403`).
* **`MaintenanceRecordController`** (`/api/maintenance`)
   * Templates under `/templates/**`.
   * Record creation under `/record` and record operations under `/records/**`.
   * Reports under `/ongoing`, `/reports/costs`, `/turnaround-average`, `/alerts`.
   * Parts under `/parts/**`.
* **`LandingController`** (`/api`)
   * `GET /welcome`.
* **Command side** (writes to PostgreSQL, then publishes an event):
   * **`AddMaintenanceTemplateUseCase`**: saves a template (`maintenancetemplate.event.created`).
   * **`AddMaintenanceRecordUseCase`**: loads the template from PostgreSQL and saves a record (`maintenance.event.created`).
   * **`MaintenanceRecordProgressService`**: `ONGOING` and `COMPLETED` transitions, template attribute change (`maintenance.event.updated`; also contains the unexposed `updateProgress`).
* **Query side** (reads only from Redis):
   * **`MaintenanceTemplateSearchService`**: template lookup by id and listing; also used to embed the template in every record response.
   * **`MaintenanceRecordSearchService`**: record search (`findAll(Specification, Pageable)` on the Redis repository).
* **`MaintenanceReportService`**
   * Ongoing records, costs per aircraft/model, turnaround average per model.
* **`MaintenanceAlertService`**
   * Alerts by days since last completed maintenance and by flight hours.
* **`PartsInventoryService`**
   * Used parts summary and low-stock alerts.
* **`MaintenanceTemplateCommandRepository`** / **`MaintenanceRecordCommandRepository`**
   * Spring Data JPA over PostgreSQL (`maintenance_db`): the write model.
* **`MaintenanceTemplateQueryRepository`** / **`MaintenanceRecordQueryRepository`**
   * Spring Data Redis over the Redis cluster: hashes `MaintenanceTemplate:<id>` and `MaintenanceRecord:<id>`.
* **`RabbitMQConfig`**
   * Topic exchanges `maintenance.exchange` and `maintenancetemplate.exchange`, durable queues `maintenance.sync.queue` and `maintenancetemplate.sync.queue`, JSON converter.
* **`MaintenanceRecordRedisProjector`** / **`MaintenanceTemplateRedisProjector`**
   * `@RabbitListener`s that write each sync event into Redis; both instances share the queues, so each event is projected once.
* **`GlobalExceptionHandler`**
   * Cross-cutting HTTP error normalization (`ErrorDetails`).
* **`UseCaseLoggingAdvice`**
   * AOP logging of `@UseCase` classes (`@within` pointcut).
* **`Bootstrap`**
   * Seeds 4 templates and 4 records in PostgreSQL only (no events, so they are not in Redis).

External integration clients:

* `AircraftRestClientService` -> `aircraft-service` (methods are `@Retryable`, 3 attempts)
* `PeerForwarder` -> other instances of the given service id (template lookup on a local miss; called with `aircraft-service`)
* Both are built from the shared `GlobalRestClientConfig` builder (2 s timeouts, `JwtTokenInterceptor` for `X-Gateway-Secret` + `Authorization` propagation).

---

## 2. Architecture Diagram Source

PlantUML source file:

* `architecture-c4-level3.puml`

---

## 3. Data Flow and Dependencies

1. Inbound requests arrive through the API Gateway for `/api/maintenance/**`; the gateway validates the JWT and adds `X-Gateway-Secret`.
2. `gatewaySecretFilter` checks the secret header before any controller runs (`403` when missing).
3. Template creation goes to `AddMaintenanceTemplateUseCase`, which saves in PostgreSQL and publishes to `maintenancetemplate.exchange`.
4. Record creation checks the aircraft through `AircraftRestClientService`, then `AddMaintenanceRecordUseCase` loads the template from PostgreSQL, saves the record and publishes to `maintenance.exchange`.
5. Status changes, completion and attribute changes use `MaintenanceRecordProgressService` over PostgreSQL and publish `maintenance.event.updated`.
6. The projectors write the events into the Redis cluster.
7. Template and record reads, searches, reports, alerts and parts summaries read Redis; reports and alerts also call `aircraft-service` for model names and flight hours. A template lookup that misses in Redis is passed to `PeerForwarder`.
8. Exceptions are transformed by `GlobalExceptionHandler` into `ErrorDetails` payloads.
