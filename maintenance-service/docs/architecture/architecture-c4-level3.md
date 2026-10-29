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
* **`AddMaintenanceTemplateUseCase`** / **`MaintenanceTemplateSearchService`**
   * Template persistence, lookup and listing.
* **`AddMaintenanceRecordUseCase`**
   * Aircraft check in `aircraft-service` and record persistence.
* **`MaintenanceRecordProgressService`**
   * Record lookup, `ONGOING` transition, template attribute change (also contains unexposed `updateProgress` and `completeRecord`).
* **`MaintenanceRecordSearchService`**
   * `Specification`-based record search.
* **`MaintenanceReportService`**
   * Ongoing records, costs per aircraft/model, turnaround average per model.
* **`MaintenanceAlertService`**
   * Alerts by days since last completed maintenance and by flight hours.
* **`PartsInventoryService`**
   * Used parts summary and low-stock alerts.
* **`MaintenanceTemplateRepository`** / **`MaintenanceRecordRepository`**
   * Spring Data JPA persistence.
* **`GlobalExceptionHandler`**
   * Cross-cutting HTTP error normalization (`ErrorDetails`).
* **`UseCaseLoggingAdvice`**
   * AOP logging intended for `@UseCase` classes (pointcut currently matches no class).
* **`Bootstrap`**
   * Seeds 4 templates and 4 records.

External integration clients:

* `AircraftRestClientService` -> `aircraft-service`
* Built from the shared `GlobalRestClientConfig` builder, which adds `JwtTokenInterceptor` (`X-Gateway-Secret` + `Authorization` propagation).

---

## 2. Architecture Diagram Source

PlantUML source file:

* `architecture-c4-level3.puml`

---

## 3. Data Flow and Dependencies

1. Inbound requests arrive through the API Gateway for `/api/maintenance/**`; the gateway validates the JWT and adds `X-Gateway-Secret`.
2. `gatewaySecretFilter` checks the secret header before any controller runs (`403` when missing).
3. Template flows go from `MaintenanceRecordController` to `AddMaintenanceTemplateUseCase` / `MaintenanceTemplateSearchService` over `MaintenanceTemplateRepository`.
4. Record creation checks the aircraft through `AircraftRestClientService`, resolves the template, and persists through `AddMaintenanceRecordUseCase`.
5. Record reads, status changes and attribute changes use `MaintenanceRecordProgressService`; searches use `MaintenanceRecordSearchService`.
6. Reports and alerts read `MaintenanceRecordRepository` and, for model names and flight hours, call `aircraft-service`.
7. Exceptions are transformed by `GlobalExceptionHandler` into `ErrorDetails` payloads.
