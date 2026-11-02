# Aircraft Service — REST API Specification (As Implemented)

This document reflects the current implementation in `HangarController`, `ModelCatalogController`, `HangarViewController`, and related classes.

---

## 1. General Service Configuration

* **Base URL (via API Gateway):** `https://localhost:7979`
* **Base URL (service, local):** `http://localhost:8081`
* **API Prefixes:** `/api/hangar`, `/api/catalog`
* **Content-Type:** `application/json`

### 1.1. Required Headers

| Header | Where it is checked | Effect when missing / invalid |
| :--- | :--- | :--- |
| `Authorization: Bearer <JWT>` | API Gateway (`JwtAuthentication` filter) | `401 Unauthorized` (empty body) from the gateway |
| `X-Gateway-Secret: sidis-g6-secret` | `AircraftSecurityConfig.gatewaySecretFilter` | `403 Forbidden`, body `Unauthorized Access! Please use the API Gateway!` |

* The gateway adds `X-Gateway-Secret` and `X-User-Name` after validating the JWT, so clients only send `Authorization`.
* The service itself does not validate the JWT and does not read `X-User-Name`; all requests that pass the secret filter are `permitAll()`.
* Paths containing `/h2-console`, `/swagger-ui` or `/api-docs` skip the secret filter.

---

## 2. Implemented Endpoints Summary

All endpoints additionally return `403` when `X-Gateway-Secret` is missing (and `401` at the gateway when the JWT is missing or invalid).

| Method | Endpoint Path | Description | User Story | Status Codes |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/catalog/model` | Add an aircraft model to the catalog | US101, US202 | `201`, `400`, `500` |
| `PATCH` | `/api/catalog/model/{modelName}` | Update an aircraft model | US201 | `200`, `404`, `409`, `500` |
| `GET` | `/api/catalog/rankings?mode=` | Top 5 models by assignments or flight hours | US204 | `200`, `500` |
| `GET` | `/api/catalog/{modelName}` | Get one aircraft model | — | `200`, `404` |
| `POST` | `/api/hangar/aircraft` | Register a new aircraft in the hangar | US102 | `201`, `400`, `404`, `500` |
| `GET` | `/api/hangar/aircraft/{registrationNumber}` | Get one aircraft | US103 | `200`, `404` |
| `GET` | `/api/hangar/aircraft` | List aircraft with optional filters and paging | US104 | `200`, `400` |
| `PATCH` | `/api/hangar/aircraft/{registrationNumber}` | Change aircraft availability | US105 | `200`, `400`, `404`, `409` |
| `GET` | `/api/hangar/aircraft/{registrationNumber}/compatible-routes` | Routes compatible with the aircraft | US203 | `200`, `404`, `500` |
| `GET` | `/api/hangar/aircraft/{registrationNumber}/status` | Current availability of one aircraft | US205 | `200`, `404` |
| `GET` | `/api/hangar/analytics/aircraft-availability` | Registrations grouped by availability | US205 | `200` |
| `GET` | `/api/hangar/analytics/status-summary` | Count of aircraft per availability | US205 | `200` |
| `GET` | `/api/hangar/analytics/operational-hours` | Aircraft ordered by operational hours | US206 | `200` |
| `GET` | `/api/hangar/analytics/fuel-efficiency/aircraft` | Fuel consumption (L/km) per aircraft | US227 | `200` |
| `GET` | `/api/hangar/analytics/fuel-efficiency/routes` | Estimated fuel per route | US227 | `200`, `500` |
| `GET` | `/api/hangar/analytics/fuel-efficiency` | Legacy combined fuel efficiency map | — | `200`, `500` |
| `GET` | `/api/hangar/aircraft-features` | Aircraft having all requested features | — | `200`, `400` |
| `GET` | `/api/hangar/view/utilization` | Thymeleaf utilization page (placeholder) | — | `200`, `500` |
| `GET` | `/api/welcome` | Landing message (not routed by the gateway) | — | `200` |

---

## 3. Endpoint Details

### 3.1. Add Aircraft Model

* **Endpoint:** `POST /api/catalog/model`
* **Consumes:** `application/json`
* **Validation annotations on request:**
  * `modelName`: `@NotBlank`
  * `manufacturer`: `@NotBlank`
  * `specs`: `@Valid` (no constraints inside `ModelSpecsRequest`)

#### Request Example

```json
{
  "modelName": "A330-900neo",
  "manufacturer": "Airbus",
  "modelImage": ["a330neo-1.jpg", "a330neo-2.jpg"],
  "specs": {
    "fuelCapacityLiters": 139090.0,
    "maximumRangeKm": 13334.0,
    "cruisingSpeedKph": 871.0,
    "standardSeatingCapacity": 287
  }
}
```

#### Successful Response (`201 Created`)

Empty body.

Notes:

* `modelName` is the assigned `@Id` and `AircraftModel` has no `@Version`, so `save(...)` performs a merge: posting an existing `modelName` overwrites that model instead of being rejected.
* If `specs` is omitted, the controller throws `IllegalArgumentException` (`"Object 'specs' is mandatory in the request body."`) → `400`.
* `fuelCapacityLiters`, `maximumRangeKm`, and `cruisingSpeedKph` are `nullable = false` columns; missing values fail at persistence (`500`).

---

### 3.2. Update Aircraft Model

* **Endpoint:** `PATCH /api/catalog/model/{modelName}`
* **Description:** Adds new images (without duplicates), changes manufacturer, and changes any non-null spec field.

#### Request Example

```json
{
  "modelName": "A320neo",
  "modelImage": ["a320neo-cabin.jpg"],
  "manufacturer": "Airbus",
  "specs": {
    "fuelCapacityLiters": 80000.0,
    "maximumRangeKm": 6500.0,
    "cruisingSpeedKph": null,
    "standardSeatingCapacity": null
  }
}
```

#### Successful Response (`200 OK`)

Empty body.

Validations:

1. Path `modelName` must equal body `modelName` → otherwise `409` (`"Non matching info"`).
2. At least one of `modelImage`, `manufacturer`, `specs` must be present → otherwise `409` (`"Bad request"`).
3. Model must exist → otherwise `404`.

`specs` is optional: when it is omitted, only `modelImage` and/or `manufacturer` are changed.

---

### 3.2.0. Get Aircraft Model

* **Endpoint:** `GET /api/catalog/{modelName}`
* **Description:** Returns the model with its specs. Added for `flight-service`, because `GET /api/hangar/aircraft/{registrationNumber}` only returns the model name.
* **Errors:** unknown model → `404` (`"Model does not exist in catalog"`).

#### Response Example (`200 OK`)

```json
{
  "modelName": "A320neo",
  "modelImage": ["a320neo-1.jpg", "a320neo-2.jpg", "a320neo-cockpit.jpg"],
  "manufacturer": "Airbus",
  "specs": {
    "fuelCapacityLiters": 79000.0,
    "maximumRangeKm": 6300.0,
    "cruisingSpeedKph": 828.0,
    "standardSeatingCapacity": 180
  }
}
```

---

### 3.2.1. Model Rankings (US204)

* **Endpoint:** `GET /api/catalog/rankings?mode={assignments|flight-hours}`
* **Description:** Returns the top 5 models, ordered descending.
  * `assignments`: loads all flights from `flight-service` through `FlightSearchService`, counts flights per aircraft registration, and sums the counts per model.
  * `flight-hours`: sums `totalFlightHours` of the hangar aircraft per model (local data only).
  * Any other `mode` value returns an empty list.

#### Response Example (`200 OK`, `mode=flight-hours`)

```json
[
  {
    "modelName": "737-800",
    "modelImage": ["b737-800-front.jpg", "b737-800-side.jpg", "b737-800-cabin.jpg"],
    "manufacturer": "Boeing",
    "specs": {
      "fuelCapacityLiters": 70500.0,
      "maximumRangeKm": 5436.0,
      "cruisingSpeedKph": 842.0,
      "standardSeatingCapacity": 189
    }
  }
]
```

Notes:

* `mode` is a required `@RequestParam`; omitting it raises `MissingServletRequestParameterException`, which falls into the generic handler (`500`).
* `mode=assignments` calls `GET /api/flights` on `flight-service` and counts flights per aircraft registration (mapped from `aircraftRegistrationNumber` through `@JsonAlias`). If `flight-service` is unavailable after the retries, it ends in `500`.

---

### 3.3. Register Aircraft

* **Endpoint:** `POST /api/hangar/aircraft`
* **Consumes:** `application/json`

#### Request Example

```json
{
  "registrationNumber": "CS-TTP",
  "modelName": "A320neo",
  "manufacturingDate": "2021-06-15",
  "totalOperationalHours": 5000.0,
  "totalFlightHours": 4200.0,
  "availability": "AVAILABLE"
}
```

#### Successful Response (`201 Created`)

Empty body.

Execution steps:

1. Look up model by `modelName` in the catalog → `404` if missing.
2. Build a new `Aircraft` (initial status `INACTIVE`, `meanRange = 0.0`).
3. Apply `availability` through `AircraftLifeCycleUpdaterService`.
4. Persist through `AddAircraftUseCase`.

Notes:

* `availability` accepts `AVAILABLE`, `MAINTENANCE`, `INACTIVE`. `IN_FLIGHT` or an unknown value returns `400`.
* There is no explicit duplicate check for `registrationNumber`; a duplicate fails at persistence (`500`).

---

### 3.4. Get Aircraft

* **Endpoint:** `GET /api/hangar/aircraft/{registrationNumber}`

#### Response Example (`200 OK`)

```json
{
  "registrationNumber": "D-ABXA",
  "modelName": "737-800",
  "status": "AVAILABLE",
  "manufacturingDate": "2016-02-18",
  "totalOperationalHours": 18500.0,
  "totalFlightHours": 14200.0,
  "meanRange": 0.0,
  "features": ["Wi-Fi"]
}
```

When the aircraft is not in the local database, the controller asks the other registered instances of `aircraft-service` through `PeerForwarder` before answering `404` (`"Resource not found in any instance"`); see `domain-and-resilience.md`, section 6.

This endpoint is also consumed by `flight-service` (flight scheduling), `route-service` and `maintenance-service` (record creation and filtering). All of them send `X-Gateway-Secret` through their `JwtTokenInterceptor`.

---

### 3.5. List Aircraft with Filtering

* **Endpoint:** `GET /api/hangar/aircraft`
* **Query parameters (all optional):**
  * `model`: exact model name.
  * `status`: availability (case-insensitive).
  * `manufacturingDate`: ISO date (`yyyy-MM-dd`).
  * `page` (default `0`), `size` (default `10`).

#### Response Example (`200 OK`)

```json
[
  {
    "registrationNumber": "F-HXKA",
    "modelName": "A320neo",
    "status": "MAINTENANCE",
    "manufacturingDate": "2020-09-05",
    "totalOperationalHours": 8200.0,
    "totalFlightHours": 6100.0,
    "meanRange": 0.0,
    "features": ["Wi-Fi", "In-flight Entertainment"]
  }
]
```

Notes:

* Filtering uses a JPA `Specification`.
* The response is the page content only (a JSON array), without paging metadata.
* An unknown `status` value raises `IllegalArgumentException` → `400`.

---

### 3.6. Change Aircraft Availability

* **Endpoint:** `PATCH /api/hangar/aircraft/{registrationNumber}`

#### Request Example

```json
{
  "registrationNumber": "D-ABXA",
  "availability": "MAINTENANCE"
}
```

#### Successful Response (`200 OK`)

Empty body.

Validations:

1. Path and body `registrationNumber` must match → otherwise `409`.
2. Aircraft must exist → otherwise `404`.
3. Allowed transitions: `AVAILABLE`, `MAINTENANCE`, `INACTIVE`. `IN_FLIGHT` → `400` (`"Unsupported status transition"`).

---

### 3.7. Compatible Routes

* **Endpoint:** `GET /api/hangar/aircraft/{registrationNumber}/compatible-routes`
* **Description:** Loads all routes from `route-service` and keeps those where `route.capacity <= aircraftCapacity` and `route.distanceKm <= aircraftRange`.

#### Response Example (`200 OK`)

```json
[
  {
    "routeId": 101,
    "routeName": "OPO-LIS",
    "originAirportIata": null,
    "destinationAirportIata": null,
    "distanceKm": 274.0,
    "routeRequirements": {
      "requiredCapacity": 150
    }
  }
]
```

Notes:

* Capacity comes from the aircraft `SeatingPack`, or falls back to the model `standardSeatingCapacity`.
* Route capacity is read from `routeRequirements.requiredCapacity` (`RouteClientDTO.capacity()`), which now matches `route-service`'s response.
* In `RouteSearchService`, the fallback to model `maximumRange` is overwritten by `meanRange`; since `meanRange` starts at `0.0`, only zero-distance routes match unless `meanRange` was updated.
* `originAirportIata` and `destinationAirportIata` are always `null`, because `route-service` returns `originIataCode` and `destinationIataCode`.

---

### 3.8. Aircraft Status

* **Endpoint:** `GET /api/hangar/aircraft/{registrationNumber}/status`

#### Response Example (`200 OK`)

```json
{
  "status": "AVAILABLE",
  "timestamp": "2026-10-25T14:32:00Z"
}
```

The timestamp is `Instant.now()` truncated to minutes.

---

### 3.9. Availability Analytics

* **Endpoint:** `GET /api/hangar/analytics/aircraft-availability`

#### Response Example (`200 OK`)

```json
{
  "AVAILABLE": ["D-ABXA"],
  "IN_FLIGHT": ["PR-EMJ"],
  "MAINTENANCE": ["F-HXKA"],
  "INACTIVE": ["CS-TUA", "G-X321", "N789DL", "TEST-001", "TEST-002", "TEST-003"]
}
```

* **Endpoint:** `GET /api/hangar/analytics/status-summary`

#### Response Example (`200 OK`)

```json
{
  "AVAILABLE": 1,
  "IN_FLIGHT": 1,
  "MAINTENANCE": 1,
  "INACTIVE": 6
}
```

---

### 3.10. Operational Hours

* **Endpoint:** `GET /api/hangar/analytics/operational-hours`
* **Query parameters:** `page` (default `0`), `size` (default `5`).
* **Description:** Page of aircraft sorted descending by `totalOperationalHours`.

#### Response Example (`200 OK`)

```json
{
  "D-ABXA": 18500.0,
  "CS-TUA": 12500.0,
  "F-HXKA": 8200.0,
  "N789DL": 6000.0,
  "G-X321": 5400.0
}
```

---

### 3.11. Fuel Efficiency per Aircraft (US227)

* **Endpoint:** `GET /api/hangar/analytics/fuel-efficiency/aircraft`
* **Description:** `fuelConsumptionLitPerKm = fuelCapacityLiters / maximumRangeKm` (rounded to 2 decimals), sorted ascending.

#### Response Example (`200 OK`)

```json
[
  {
    "registrationNumber": "PR-EMJ",
    "modelName": "E195-E2",
    "fuelCapacityLiters": 61500.0,
    "maxRangeKm": 4815.0,
    "fuelConsumptionLitPerKm": 12.77
  }
]
```

---

### 3.12. Fuel Efficiency per Route (US227)

* **Endpoint:** `GET /api/hangar/analytics/fuel-efficiency/routes`
* **Description:** Loads all flights from `flight-service`, groups by `routeId`, averages the L/km of the aircraft that flew each route, and multiplies by route `distanceKm` from `route-service`.

#### Response Example (`200 OK`)

```json
[
  {
    "routeId": 101,
    "routeName": "OPO-LIS",
    "originIata": null,
    "destinationIata": null,
    "distanceKm": 274.0,
    "avgFuelConsumptionLitPerKm": 12.97,
    "estimatedFuelLiters": 3553.78
  }
]
```

Note:

* Depends on `GET /api/flights` in `flight-service` and `GET /api/routes/{routeId}` in `route-service`; if either is unavailable after the retries, it ends in `500`.
* The L/km is taken from each aircraft's model specs (`fuelCapacityLiters / maximumRangeKm`); aircraft that are not in this service count as `0.0` in the average.
* `originIata` and `destinationIata` are always `null` (see `domain-and-resilience.md`, section 4).

---

### 3.13. Other Endpoints

* `GET /api/hangar/analytics/fuel-efficiency`: legacy map mixing per-aircraft L/km and `"Flight {id}:"` entries; `page`/`size` are accepted but not used. For every flight from `GET /api/flights` it divides by `flight.range()`, which `flight-service` never returns (`null`), so the endpoint ends in `500` as soon as there is at least one flight.
* `GET /api/hangar/aircraft-features`: GET with a JSON body `{"features": ["Wi-Fi"]}`; an empty or missing list throws `IllegalArgumentException` (`"Feature list cannot be empty!"`) → `400`.
* `GET /api/hangar/view/utilization`: `CalculationsService` calls `GET /api/flights/aircraft/{registration}` on `flight-service` for every aircraft and builds 12 months of utilization (`flightHours / hoursInMonth × 100`, rounded to 2 decimals). The `utilization.html` template is a placeholder ("in progress") that does not render `utilizationData`. `departure` is now mapped from `scheduledDeparture`, but `flight-service` does not return `flightHours`, so every flight is skipped and every rate is `0.0`. Any downstream failure ends in `500`.

---

## 4. Error Handling (`GlobalExceptionHandler`)

Error payload model: `ErrorDetails`

```json
{
  "timestamp": "2026-10-25T12:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Aircraft does not exist in hangar",
  "path": "/api/hangar/aircraft/XX-0000",
  "validationErrors": null
}
```

### Exception to HTTP Mapping (current code)

| Exception Class | HTTP Status |
| :--- | :--- |
| `ResourceNotFoundException` | `404 Not Found` |
| `MethodArgumentNotValidException` | `400 Bad Request` |
| `HttpMessageNotReadableException` | `400 Bad Request` |
| `IllegalArgumentException` | `400 Bad Request` |
| `DomainException` | `409 Conflict` |
| `ObjectOptimisticLockingFailureException` | `409 Conflict` (`"The record was modified by another user."`) |
| `IllegalStateException` | `409 Conflict` |
| `Exception` | `500 Internal Server Error` |

Notes:

* There is no dedicated handler for `RestClient` errors from downstream services; after the retries (up to 3 attempts) they map to `500`.
* The `403` from the gateway secret filter is written directly by the filter as plain text, not as `ErrorDetails`.
