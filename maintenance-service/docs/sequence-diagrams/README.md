# Maintenance Service — Sequence Diagrams and Dynamic Views (As Implemented)

This document describes runtime behavior aligned with the current code implementation.

---

## 1. Scenario 1: Template and Record Queries

### Description

Scenario 1 contains local read operations exposed by `MaintenanceRecordController`:

* **Flow A:** `GET /api/maintenance/templates/{id}` (US117)
* **Flow B:** `GET /api/maintenance/records/{id}` (US119)
* **Flow C:** `GET /api/maintenance/records?aircraft=&attribute=&initialDate=&finalDate=` (US218)
* **Flow D:** rejected requests (gateway `401`, `gatewaySecretFilter` `403`)

In every flow the API Gateway first validates the JWT and forwards the request with `X-Gateway-Secret` and `X-User-Name`; `gatewaySecretFilter` lets it through before the controller runs.

### Sequence Diagram Source

* `scenario-1-query-maintenance.puml`

### Key Steps

#### Flow A — Template by Id

1. Client requests one template.
2. Controller delegates to `MaintenanceTemplateSearchService.findByTemplateId(...)`.
3. A missing template is passed to `PeerForwarder` (with service id `aircraft-service`); the forwarded call fails and the result is `404`.
4. Otherwise the controller maps to `MaintenanceTemplateResponse` and returns `200 OK`.

#### Flow B — Record by Id

1. Client requests one record.
2. Controller delegates to `MaintenanceRecordProgressService.findById(...)`, which calls `findByRecordId(...)`.
3. Missing record throws `DomainException` → `404`.
4. Controller maps to `MaintenanceRecordResponse` and returns `200 OK`.

#### Flow C — Search Records

1. When `aircraft` is set, the controller checks it in `aircraft-service` (`404` if missing).
2. `MaintenanceRecordSearchService` builds a `Specification` on `aircraftRegistration`, attribute and start date range, and returns the page content (`200 OK`).
3. Without `aircraft`, `aircraftRegistration.isEmpty()` throws `NullPointerException` → `500`.

#### Flow D — Rejected Requests

1. A request without a valid `Authorization: Bearer` header is stopped by the gateway with `401` (empty body).
2. A direct call to the service port without `X-Gateway-Secret` is stopped by `gatewaySecretFilter` with `403` and the plain-text body `Unauthorized Access! Please use the API Gateway!`.
3. Paths containing `/h2-console`, `/swagger-ui` or `/api-docs` are not checked.

---

## 2. Scenario 2: Register Template, Register Record and Advance Status

### Description

Scenario 2 describes the write flows in `POST /api/maintenance/templates` (US115), `POST /api/maintenance/record` (US116), `PATCH /api/maintenance/records/{id}/status` and `PATCH /api/maintenance/records/{id}/complete` (US119).

### Sequence Diagram Source

* `scenario-2-register-maintenance.puml`

### Key Steps

#### Register Template

1. Client sends name, expected duration, checklist, operation and attribute.
2. Bean validation or an unknown enum value → `400`.
3. `AddMaintenanceTemplateUseCase` saves the template; the controller returns `200 OK` with the template.

#### Register Record

1. Controller calls `aircraft-service` `GET /api/hangar/aircraft/{registrationNumber}` (`404` if missing).
2. Controller loads the template (`404` if missing).
3. Controller calls `AddMaintenanceRecordUseCase.executeShort(...)` with the registration returned by `aircraft-service`.
4. The use case looks up the aircraft again (`404` if missing) and saves the record as `INLINE`.
5. Controller returns `200 OK` (empty body).

#### Advance Status

1. Controller parses `status` (`400` if unknown) and calls `advanceStatus(...)`.
2. Missing record → `404`.
3. `ONGOING` → `markAsOngoing()`, saved, `200 OK` with the record.
4. `COMPLETED` or `INLINE` → `DomainException` → `404`.

#### Complete Record

1. Controller calls `completeRecord(id, notes, tasksDone, endDate)`.
2. Missing record → `404`.
3. `markAsCompleted(...)` rejects an already completed record (`409`) and a missing or earlier-than-start `endDate` (`400`).
4. The record is saved as `COMPLETED` with the done list and end date; `200 OK` with the record.

---

## 3. Scenario 3: Maintenance Reports and Alerts

### Description

Scenario 3 describes flows that depend on `aircraft-service`:

* **Flow A:** `GET /api/maintenance/reports/costs?groupBy=model` (US220)
* **Flow B:** `GET /api/maintenance/alerts` (US222)

### Sequence Diagram Source

* `scenario-3-maintenance-reports.puml`

### Key Steps

#### Flow A — Costs by Model

1. `MaintenanceReportService` loads all records.
2. For each record, `AircraftRestClientService` calls `GET /api/hangar/aircraft/{registration}` through `JwtTokenInterceptor` and reads `modelName` (`""` on `404`).
3. Costs (`quantity × price` of used parts) are summed per model and sorted.
4. Controller returns `200 OK`.

#### Flow B — Maintenance Alerts

1. `MaintenanceAlertService` loads `COMPLETED` records.
2. `AircraftRestClientService.findAll()` calls `GET /api/hangar/aircraft?size=1000` and reads the JSON array.
3. For each aircraft, the last completed record (by `endDate`) is found; an alert is raised when there is none, when it is at least `calendarDaysThreshold` days old, or when `totalFlightHours >= flightHoursThreshold`.
4. Controller returns `200 OK` with the alerts.

Notes:

* Every client call has a 2-second timeout and up to 3 attempts; after that the error is returned as `500`.
* `GET /api/maintenance/turnaround-average` (US221) follows the same per-record model lookup as Flow A.

---

## 4. Exception and Resilience Summary

| Exception Type | Current Mapping |
| :--- | :--- |
| `ResourceNotFoundException` | `404 Not Found` |
| `MethodArgumentNotValidException` | `400 Bad Request` |
| `HttpMessageNotReadableException` | `400 Bad Request` |
| `IllegalArgumentException` | `400 Bad Request` |
| `DomainException` | `404 Not Found` |
| `ObjectOptimisticLockingFailureException` | `409 Conflict` |
| `IllegalStateException` | `409 Conflict` |
| `Exception` | `500 Internal Server Error` |

Optimistic locking exists (`@Version` on `MaintenanceRecord`) and conflicts map to `409`. Downstream `RestClient` calls have 2-second timeouts and up to 3 attempts; after that they are not handled separately (`500`). The `403` from `gatewaySecretFilter` and the `401` from the gateway are not `ErrorDetails` payloads.
