# Maintenance Service — REST API Specification (As Implemented)

This document reflects the current implementation in `MaintenanceRecordController`, `LandingController`, and related classes.

---

## 1. General Service Configuration

* **Base URL (via API Gateway):** `https://localhost:7979`
* **Base URL (service, local):** `http://localhost:8121`
* **API Prefix:** `/api/maintenance`
* **Content-Type:** `application/json`

### 1.1. Required Headers

| Header | Where it is checked | Effect when missing / invalid |
| :--- | :--- | :--- |
| `Authorization: Bearer <JWT>` | API Gateway (`JwtAuthentication` filter) | `401 Unauthorized` (empty body) from the gateway |
| `X-Gateway-Secret: sidis-g6-secret` | `MaintenanceSecurityConfig.gatewaySecretFilter` | `403 Forbidden`, body `Unauthorized Access! Please use the API Gateway!` |

* The gateway adds `X-Gateway-Secret` and `X-User-Name` after validating the JWT, so clients only send `Authorization`.
* The service itself does not validate the JWT and does not read `X-User-Name`; all requests that pass the secret filter are `permitAll()`. There are no role checks (for example, technician vs. supervisor).
* Paths containing `/h2-console`, `/swagger-ui` or `/api-docs` skip the secret filter.

---

## 2. Implemented Endpoints Summary

All endpoints additionally return `403` when `X-Gateway-Secret` is missing (and `401` at the gateway when the JWT is missing or invalid).

| Method | Endpoint Path | Description | User Story | Status Codes |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/maintenance/templates` | Create a maintenance template | US115 | `200`, `400` |
| `GET` | `/api/maintenance/templates/{id}` | Get one template | US117 | `200`, `404` |
| `GET` | `/api/maintenance/templates` | List all templates | US117 | `200` |
| `POST` | `/api/maintenance/record` | Create a maintenance record for an aircraft | US116 | `200`, `400`, `404`, `500` |
| `GET` | `/api/maintenance/records/{id}` | Get one record | US119 | `200`, `404` |
| `PATCH` | `/api/maintenance/records/{id}/status` | Move a record to `ONGOING` | US119 | `200`, `400`, `404` |
| `PATCH` | `/api/maintenance/records/{id}/complete` | Complete a record | US119 | `200`, `400`, `404`, `409` |
| `PATCH` | `/api/maintenance/records/{id}` | Change the component attribute of a record's template | US217 | `200`, `400`, `404` |
| `GET` | `/api/maintenance/records` | Search records by aircraft, attribute and dates | US218 | `200`, `404`, `500` |
| `GET` | `/api/maintenance/ongoing` | Records in `ONGOING` status | US219 | `200` |
| `GET` | `/api/maintenance/reports/costs?groupBy=` | Parts cost per aircraft or per model | US220 | `200`, `500` |
| `GET` | `/api/maintenance/turnaround-average` | Average maintenance days per aircraft model | US221 | `200`, `500` |
| `GET` | `/api/maintenance/alerts` | Aircraft due for maintenance | US222 | `200`, `500` |
| `GET` | `/api/maintenance/parts/inventory` | Used parts summary | US226 | `200` |
| `GET` | `/api/maintenance/parts/low-stock?threshold=` | Parts used at or above a threshold | US226 | `200` |
| `GET` | `/api/welcome` | Landing message (not routed by the gateway) | — | `200` |

---

## 3. Endpoint Details

### 3.1. Create Maintenance Template (US115)

* **Endpoint:** `POST /api/maintenance/templates`
* **Validation annotations on request:**
  * `name`: `@NotBlank`
  * `expectedDuration`: `@NotNull`
  * `templateChecklist`: `@NotNull` (map of check item → required flag)
  * `operation`: `@NotBlank`, parsed with `MaintenanceType.valueOf(...)` (`INSPECTION`, `SCHEDULED`, `OVERHAUL`, `MODIFICATION`)
  * `attribute`: `@NotBlank`, parsed with `MaintenanceAttribute.valueOf(...)` (`ENGINE`, `AIRFRAME`, `AVIONICS`, `INTERIOR`, `EXTERIOR`)

#### Request Example

```json
{
  "name": "A-Check Engine Inspection",
  "expectedDuration": 6.5,
  "templateChecklist": {
    "Check oil levels": true,
    "Inspect engine turbine blades": true,
    "Run engine ground test": true
  },
  "operation": "INSPECTION",
  "attribute": "ENGINE"
}
```

#### Successful Response (`200 OK`)

```json
{
  "templateId": 5,
  "name": "A-Check Engine Inspection",
  "expectedDuration": 6.5,
  "templateChecklist": {
    "Check oil levels": true,
    "Inspect engine turbine blades": true,
    "Run engine ground test": true
  },
  "operation": "INSPECTION",
  "attribute": "ENGINE"
}
```

Notes:

* The endpoint has no `@ResponseStatus(CREATED)`, so it returns `200`, not `201`.
* An unknown `operation` or `attribute` raises `IllegalArgumentException` → `400`.
* Template names are not unique; posting the same `name` twice creates two templates.

---

### 3.2. Get Template / List Templates (US117)

* **Endpoint:** `GET /api/maintenance/templates/{id}`
* **Endpoint:** `GET /api/maintenance/templates`

Both return `MaintenanceTemplateResponse` (single object or JSON array) with the same shape as section 3.1.

Note:

* When the template is not in the local database, the controller calls `PeerForwarder.forwardGet("aircraft-service", "/api/maintenance/templates/{id}", ...)`. The service id should be `maintenance-service`; with `aircraft-service` it asks the wrong service, the call fails, and the endpoint returns `404` (`"Resource not found in any instance"`).

---

### 3.3. Create Maintenance Record (US116)

* **Endpoint:** `POST /api/maintenance/record` (singular `record`)
* **Validation annotations on request:**
  * `registrationNumber`: `@NotBlank`
  * `templateId`: `@NotNull`
  * `durationHours`: `@NotNull`
  * `description`: `@NotBlank`
  * `startDate`: `@NotNull` (ISO date)

#### Request Example

```json
{
  "registrationNumber": "CS-TUA",
  "templateId": 1,
  "durationHours": 6.5,
  "description": "Scheduled A-check maintenance including hydraulic system inspection and oil filter replacement.",
  "startDate": "2026-05-21"
}
```

#### Successful Response (`200 OK`)

Empty body.

Execution steps:

1. Controller calls `aircraft-service` `GET /api/hangar/aircraft/{registrationNumber}` → `404` (`"Aircraft does not exist in hangar"`) if it returns `404`.
2. Controller loads the template → `404` (`"Maintenance Template does not exist"`) if missing.
3. Controller calls `AddMaintenanceRecordUseCase.executeShort(aircraftDto.aircraftRegistration(), ...)` (the registration is mapped from `aircraft-service`'s `registrationNumber`).
4. The use case looks up the aircraft again and saves a new `MaintenanceRecord` with status `INLINE`.

Notes:

* The aircraft is fetched twice from `aircraft-service` (controller and use case).
* Only `HttpClientErrorException.NotFound` is caught in `AircraftRestClientService`; any other downstream error (`403`, `5xx`, timeout) is retried up to 3 times and then becomes `500`.
* The endpoint returns `200`, not `201`.

---

### 3.4. Get Record (US119)

* **Endpoint:** `GET /api/maintenance/records/{id}`

#### Response Example (`200 OK`)

```json
{
  "id": 4,
  "aircraftRegistration": "CS-TUA",
  "template": {
    "templateId": 4,
    "name": "ENGINE-REPAIR",
    "expectedDuration": 40.0,
    "templateChecklist": {
      "Replace fuel pump": true,
      "Replace oil filter": true,
      "Ground engine test": true
    },
    "operation": "OVERHAUL",
    "attribute": "ENGINE"
  },
  "durationHours": 40.0,
  "description": "In Progress",
  "startDate": "2026-10-29",
  "status": "INLINE"
}
```

Notes:

* A missing record throws `DomainException("Maintenance Record does not exist")` → `404`.
* The response does not include `endDate`, `usedParts`, `techniciansUsername`, `supervisorUsername`, or the `doneList`.

---

### 3.5. Advance Record Status (US119)

* **Endpoint:** `PATCH /api/maintenance/records/{id}/status`

#### Request Example

```json
{
  "status": "ONGOING"
}
```

#### Successful Response (`200 OK`)

The updated record (`MaintenanceRecordResponse`, same shape as section 3.4).

Transition rules (`MaintenanceRecordProgressService.advanceStatus`):

| Requested Status | Result |
| :--- | :--- |
| `ONGOING` | `markAsOngoing()`, saved → `200` |
| `COMPLETED` | `DomainException("Use the complete endpoint to mark as completed")` → `404` |
| `INLINE` | `DomainException("Invalid status transition")` → `404` |
| Unknown value | `IllegalArgumentException` from `valueOf` → `400` |

Notes:

* Rejected transitions return `404` because `DomainException` is mapped to `Not Found`.
* `markAsOngoing()` has no guard, so a `COMPLETED` record can be moved back to `ONGOING`.
* A missing record → `404` (`"Maintenance record does not exist"`).

---

### 3.5.1. Complete Record (US119)

* **Endpoint:** `PATCH /api/maintenance/records/{id}/complete`
* **Request body:** `notes` (text), `tasksDone` (map of task → done flag), `endDate` (ISO date). There are no bean validation annotations.

#### Request Example

```json
{
  "notes": "All checks passed",
  "tasksDone": {
    "Replace fuel pump": true,
    "Replace oil filter": true,
    "Ground engine test": true
  },
  "endDate": "2026-11-01"
}
```

#### Successful Response (`200 OK`)

The completed record (`MaintenanceRecordResponse`, same shape as section 3.4, with `status: "COMPLETED"`).

Rules (`MaintenanceRecord.markAsCompleted(notes, tasksDone, endDate)`):

| Case | Result |
| :--- | :--- |
| Record not found | `404` (`"Maintenance record does not exist"`) |
| Record already `COMPLETED` | `IllegalStateException` → `409` |
| `endDate` missing | `IllegalArgumentException` → `400` |
| `endDate` before `startDate` | `IllegalArgumentException` → `400` |

Notes:

* An `INLINE` record can be completed directly; it does not have to be `ONGOING` first.
* `tasksDone` is not compared with the template checklist.
* The response does not include `endDate` or the done list, even though both are stored.

---

### 3.6. Change Record Attribute (US217)

* **Endpoint:** `PATCH /api/maintenance/records/{id}`

#### Request Example

```json
{
  "attribute": "AIRFRAME"
}
```

#### Successful Response (`200 OK`)

Empty body.

Notes:

* The attribute is changed on the record's `MaintenanceTemplate`, not on the record, so every record that shares that template changes category.
* There is no explicit `save`; the change is persisted by dirty checking, because open-session-in-view keeps the template managed across the two service calls.
* `attribute` is not validated: an empty body or `"attribute": null` sets the template attribute to `null`. An unknown value fails JSON parsing → `400`.
* A missing record → `404`.

---

### 3.7. Ongoing Maintenance (US219)

* **Endpoint:** `GET /api/maintenance/ongoing`
* **Description:** All records with status `ONGOING`, as `MaintenanceRecordResponse[]`.

Note:

* The seeded "In Progress" record is created as `INLINE`, so the list is empty until a record is moved with `PATCH /records/{id}/status`.

---

### 3.8. Maintenance Costs (US220)

* **Endpoint:** `GET /api/maintenance/reports/costs?groupBy={aircraft|model}` (default `aircraft`)
* **Description:** Sums `quantity × price` of the used parts of every record, grouped by aircraft registration or by aircraft model name, sorted by group.

#### Response Example (`200 OK`, seeded data, `groupBy=aircraft`)

```json
[
  { "group": "CS-TUA", "totalCost": 6700.0 },
  { "group": "D-ABXA", "totalCost": 5600.0 },
  { "group": "F-HXKA", "totalCost": 0.0 }
]
```

Notes:

* Any `groupBy` other than `model` (case-insensitive) groups by aircraft.
* `groupBy=model` calls `aircraft-service` once per record. The model name is `""` when the aircraft is not found.

---

### 3.9. Average Turnaround (US221)

* **Endpoint:** `GET /api/maintenance/turnaround-average`
* **Description:** For `COMPLETED` records with an `endDate`, computes `days(startDate → endDate)`, groups by aircraft model name from `aircraft-service`, and averages.

#### Response Shape (`200 OK`)

```json
[
  { "aircraftType": "<model name>", "averageDays": 0.0 }
]
```

Notes:

* With the seeded data (all completed yesterday, started 10, 5 and 2 days ago) the result is `737-800` → `6.5` (`CS-TUA` 9 days, `D-ABXA` 4 days) and `A320neo` → `1.0` (`F-HXKA`).
* Unknown aircraft are grouped under `""`.
* Any non-`404` error from `aircraft-service` is retried up to 3 times and then ends in `500`.

---

### 3.10. Search Records (US218)

* **Endpoint:** `GET /api/maintenance/records`
* **Query parameters (all optional):**
  * `aircraft`: aircraft registration.
  * `attribute`: `MaintenanceAttribute` name.
  * `initialDate`, `finalDate`: ISO dates, compared with `startDate`.
  * `page` (default `0`), `size` (default `10`).
* **Response:** page content as `MaintenanceRecordResponse[]` (no paging metadata).

Current behavior:

* When `aircraft` is set, the controller first checks it in `aircraft-service` → `404` (`"Aircraft does not exist"`) if missing; then the `Specification` filters on `aircraftRegistration` and the other given filters → `200`.
* Without `aircraft`, `MaintenanceRecordSearchService` calls `aircraftRegistration.isEmpty()` on `null` → `NullPointerException` → `500`. Searching only by attribute or dates is therefore not possible.
* An unknown `attribute` value fails inside the `Specification` (`500`).

---

### 3.11. Maintenance Alerts (US222)

* **Endpoint:** `GET /api/maintenance/alerts?calendarDaysThreshold=180&flightHoursThreshold=500`
* **Description:** For every aircraft in `aircraft-service`, raise an alert if there is no completed maintenance, if the last completed maintenance is at least `calendarDaysThreshold` days old, or if `totalFlightHours >= flightHoursThreshold`.

#### Response Example (`200 OK`, seeded data evaluated on 2026-11-01, first entry)

```json
[
  {
    "registrationNumber": "CS-TUA",
    "aircraftModel": "737-800",
    "totalFlightHours": 9800.0,
    "lastMaintenanceDate": "2026-10-31",
    "daysSinceLastMaintenance": 1,
    "alertReason": "Total flight hours 9800.0 reached threshold of 500.0"
  }
]
```

Notes:

* `AircraftRestClientService.findAll()` calls `GET /api/hangar/aircraft?size=1000`, so only the first 1000 aircraft are checked.
* A `4xx` from `aircraft-service` returns an empty list (no alerts); other errors are retried up to 3 times and then end in `500`.
* An aircraft with `totalFlightHours = null` makes the threshold comparison throw (`500`).

---

### 3.12. Parts Inventory and Low Stock (US226)

* **Endpoint:** `GET /api/maintenance/parts/inventory`
* **Description:** Groups used parts of all records by `partSerialNumber`, sorted by serial.

#### Response Example (`200 OK`, seeded data)

```json
[
  { "partSerialNumber": "BRAKE-200", "totalQuantityUsed": 2, "totalCost": 2400.0, "timesUsedInRecords": 1 },
  { "partSerialNumber": "ENG-001", "totalQuantityUsed": 2, "totalCost": 3000.0, "timesUsedInRecords": 1 },
  { "partSerialNumber": "FUEL-PUMP-01", "totalQuantityUsed": 1, "totalCost": 3200.0, "timesUsedInRecords": 1 },
  { "partSerialNumber": "OIL-FILTER-22", "totalQuantityUsed": 2, "totalCost": 500.0, "timesUsedInRecords": 1 },
  { "partSerialNumber": "TIRE-900", "totalQuantityUsed": 4, "totalCost": 3200.0, "timesUsedInRecords": 1 }
]
```

* **Endpoint:** `GET /api/maintenance/parts/low-stock?threshold=10` (default `10`)
* **Description:** Parts whose `totalQuantityUsed >= threshold`.

#### Response Example (`200 OK`, `threshold=3`)

```json
[
  {
    "partSerialNumber": "TIRE-900",
    "totalQuantityUsed": 4,
    "timesUsedInRecords": 1,
    "alertMessage": "Part TIRE-900 used 4 units across 1 record(s); consider restocking (threshold: 3)"
  }
]
```

Notes:

* There is no stock model: "low stock" is derived only from how many units were used.
* `timesUsedInRecords` counts `UsedPart` entries, not distinct records.
* With the default threshold and seeded data, the low-stock list is empty.

---

## 4. Error Handling (`GlobalExceptionHandler`)

Error payload model: `ErrorDetails`

```json
{
  "timestamp": "2026-10-29T12:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Maintenance Record does not exist",
  "path": "/api/maintenance/records/99",
  "validationErrors": null
}
```

### Exception to HTTP Mapping (current code)

| Exception Class | HTTP Status |
| :--- | :--- |
| `ResourceNotFoundException` | `404 Not Found` |
| `MethodArgumentNotValidException` | `400 Bad Request` (`validationErrors` filled) |
| `HttpMessageNotReadableException` | `400 Bad Request` |
| `IllegalArgumentException` | `400 Bad Request` |
| `DomainException` | `404 Not Found` |
| `ObjectOptimisticLockingFailureException` | `409 Conflict` (`"The record was modified by another user."`) |
| `IllegalStateException` | `409 Conflict` |
| `Exception` | `500 Internal Server Error` (message is the exception message; stack trace printed) |

Notes:

* `DomainException` is used both for missing resources and for business-rule rejections (for example invalid status transitions), so both are reported as `404`.
* There is no dedicated handler for `RestClient` errors from `aircraft-service`; after the retries they map to `500`.
* The `403` from the gateway secret filter is written directly by the filter as plain text, not as `ErrorDetails`.
