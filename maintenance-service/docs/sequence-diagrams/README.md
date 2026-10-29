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
3. Missing template throws `DomainException` → `409`.
4. Controller maps to `MaintenanceTemplateResponse` and returns `200 OK`.

#### Flow B — Record by Id

1. Client requests one record.
2. Controller delegates to `MaintenanceRecordProgressService.findById(...)`, which calls `findByRecordId(...)`.
3. Missing record throws `DomainException` → `409`.
4. Controller maps to `MaintenanceRecordResponse` and returns `200 OK`.

#### Flow C — Search Records

1. When `aircraft` is set, the controller checks it in `aircraft-service` (`409` if missing).
2. `MaintenanceRecordSearchService` builds a `Specification`.
3. Without `aircraft`, `aircraftRegistration.isEmpty()` throws `NullPointerException`; with `aircraft`, the `Specification` references a non-existent `aircraft` attribute. Both end in `500`.

#### Flow D — Rejected Requests

1. A request without a valid `Authorization: Bearer` header is stopped by the gateway with `401` (empty body).
2. A direct call to the service port without `X-Gateway-Secret` is stopped by `gatewaySecretFilter` with `403` and the plain-text body `Unauthorized Access! Please use the API Gateway!`.
3. Paths containing `/h2-console`, `/swagger-ui` or `/api-docs` are not checked.

---

## 2. Scenario 2: Register Template, Register Record and Advance Status

### Description

Scenario 2 describes the write flows in `POST /api/maintenance/templates` (US115), `POST /api/maintenance/record` (US116) and `PATCH /api/maintenance/records/{id}/status` (US119).

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
3. Controller calls `AddMaintenanceRecordUseCase.executeShort(...)` with `aircraftDto.aircraftRegistration()`, which is `null` because `aircraft-service` returns `registrationNumber`.
4. The use case looks up the aircraft again with `null` (`GET /api/hangar/aircraft/`); the downstream error is not caught → `500`.
5. If that lookup succeeded, the record would be saved as `INLINE` with a `null` registration and `200 OK` (empty body) returned.

#### Advance Status

1. Controller parses `status` (`400` if unknown) and calls `advanceStatus(...)`.
2. Missing record → `409`.
3. `ONGOING` → `markAsOngoing()`, saved, `200 OK` with the record.
4. `COMPLETED` or `INLINE` → `DomainException` → `409`.

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
2. `AircraftRestClientService.findAll()` calls `GET /api/hangar/aircraft` and tries to read the JSON array as a single `AircraftClientDTO`.
3. The deserialization error is not caught → `500`.

Notes:

* The per-aircraft alert logic (no completed maintenance, days since last maintenance, flight hours) is never reached in the current code.
* `GET /api/maintenance/turnaround-average` (US221) follows the same per-record model lookup as Flow A.

---

## 4. Exception and Resilience Summary

| Exception Type | Current Mapping |
| :--- | :--- |
| `ResourceNotFoundException` | `404 Not Found` |
| `MethodArgumentNotValidException` | `400 Bad Request` |
| `HttpMessageNotReadableException` | `400 Bad Request` |
| `IllegalArgumentException` | `400 Bad Request` |
| `DomainException` | `409 Conflict` |
| `ObjectOptimisticLockingFailureException` | `409 Conflict` |
| `IllegalStateException` | `409 Conflict` |
| `Exception` | `500 Internal Server Error` |

Optimistic locking exists (`@Version` on `MaintenanceRecord`) and conflicts map to `409`. Downstream `RestClient` failures are not handled separately (`500`). The `403` from `gatewaySecretFilter` and the `401` from the gateway are not `ErrorDetails` payloads.
