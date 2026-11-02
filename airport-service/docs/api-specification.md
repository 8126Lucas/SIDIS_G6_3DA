# Airport Service — REST API Specification (As Implemented)

This document reflects the current implementation in `AirportController`, `AirportService`, `AirportCsvService`, and related classes.

---

## 1. General Service Configuration

* **Base URL (via API Gateway):** `https://localhost:7979`
* **Base URL (service, local):** `http://localhost:8091`
* **API Prefix:** `/api/airports`
* **Content-Type:** `application/json` (except the CSV import, which is `multipart/form-data`)

### 1.1. Required Headers

| Header | Where it is checked | Effect when missing / invalid |
| :--- | :--- | :--- |
| `Authorization: Bearer <JWT>` | API Gateway (`JwtAuthentication` filter) | `401 Unauthorized` (empty body) from the gateway |
| `X-Gateway-Secret: sidis-g6-secret` | `AirportSecurityConfig.gatewaySecretFilter` | `403 Forbidden`, body `Unauthorized Access! Please use the API Gateway!` |

* The gateway adds `X-Gateway-Secret` and `X-User-Name` after validating the JWT, so clients only send `Authorization`.
* The service itself does not validate the JWT and does not read `X-User-Name`; all requests that pass the secret filter are `permitAll()`.
* Paths containing `/h2-console`, `/swagger-ui` or `/api-docs` skip the secret filter.

---

## 2. Implemented Endpoints Summary

All endpoints additionally return `403` when `X-Gateway-Secret` is missing (and `401` at the gateway when the JWT is missing or invalid).

| Method | Endpoint Path | Description | Status Codes |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/airports` | Register a new airport | `201`, `400`, `409`, `500` |
| `POST` | `/api/airports/import` | Import airports from a CSV file | `200`, `400`, `409`, `500` |
| `POST` | `/api/airports/{iataCode}/certifications` | Add a certification to an airport | `201`, `400`, `409` |
| `GET` | `/api/airports/{iataCode}` | Get one airport | `200`, `409` |
| `GET` | `/api/airports` | List airports with optional filters | `200` |
| `PATCH` | `/api/airports/{iataCode}/status` | Change airport status | `200`, `400`, `409`, `500` |
| `PATCH` | `/api/airports/{iataCode}/details` | Update name, operational hours and contacts | `200`, `400`, `409` |
| `GET` | `/api/airports/{iataCode}/routes` | Routes departing from or arriving at the airport | `200`, `409`, `500` |
| `GET` | `/api/airports/busiest` | Airports ranked by number of routes | `200`, `500` |
| `GET` | `/api/airports/grouped` | Airports grouped by country, city or region | `200`, `409` |

---

## 3. Endpoint Details

### 3.1. Register Airport

* **Endpoint:** `POST /api/airports`
* **Consumes:** `application/json`
* **Validation annotations on request:** none (`@Valid` is present, but `AddAirportRequest` has no constraints).

#### Request Example

```json
{
  "iataCode": "FAO",
  "airportType": "International",
  "name": "Faro Airport",
  "status": "OPERATIONAL",
  "location": {
    "city": "Faro",
    "longitude": -7.97,
    "latitude": 37.0144,
    "region": "Algarve",
    "timezone": "Europe/Lisbon",
    "country": "Portugal"
  },
  "facilities": {
    "terminalCount": 1,
    "services": ["Duty Free", "WiFi"],
    "gateCount": 22
  },
  "routeDistance": 0.0,
  "runways": [
    { "runwayName": "10/28", "length": 2490.0, "orientation": "Asphalt", "status": "OPEN" }
  ],
  "certifications": [
    { "name": "EASA Aerodrome License", "category": "REGULATORY", "startsAt": "2024-01-01", "expiresAt": "2029-01-01" }
  ],
  "contacts": [
    { "type": "PHONE", "value": "+351 289 800 800" }
  ],
  "airportPhotos": ["fao_terminal.jpg"]
}
```

#### Successful Response (`201 Created`)

Empty body.

Validations (in execution order):

1. `status`, runway `status`, and contact `type` are parsed with `Enum.valueOf(...)` → an unknown value returns `400`.
2. `iataCode` must match `[A-Z]{3}` → otherwise `409` (`"IATA code must have 3 uppercase letters"`).
3. `iataCode` must not exist → otherwise `409` (`"Airport already exists"`).

Notes:

* `location`, `facilities`, `runways`, and `status` are dereferenced without null checks; omitting any of them ends in `500`.
* `certifications`, `contacts`, and `airportPhotos` are optional (an empty list is used).
* All `location` fields and `facilities.terminalCount` / `facilities.gateCount` are `nullable = false` columns; missing values fail at persistence (`500`).
* In the seeded data, runway `orientation` holds the surface material (`"Asphalt"`, `"Concrete"`).

---

### 3.2. Import Airports from CSV

* **Endpoint:** `POST /api/airports/import`
* **Consumes:** `multipart/form-data`, part name `file`.
* **Description:** Parses the CSV through `AirportCsvService` and registers every airport through `AirportService.execute(...)`.

#### Expected CSV Layout

The first line is a header and is skipped. Rows with fewer than 15 columns are silently ignored.

| Column | Field | Format |
| :--- | :--- | :--- |
| 0 | `iataCode` | text |
| 1 | `airportType` | text |
| 2 | `name` | text |
| 3 | `status` | `AirportStatus` name |
| 4 – 9 | `city`, `longitude`, `latitude`, `region`, `timezone`, `country` | text / decimal |
| 10 | `routeDistance` and `facilities.gateCount` | decimal |
| 11 | runways (also used as `facilities.services`) | `name\|length\|orientation\|status;...` |
| 12 | certifications | `name;name;...` |
| 13 | contacts | `TYPE\|value;...` |
| 14 | photos | `file;file;...` |

#### Example Row

```csv
iata,type,name,status,city,lon,lat,region,timezone,country,capacity,runways,certifications,contacts,photos
FAO,International,Faro Airport,OPERATIONAL,Faro,-7.97,37.0144,Algarve,Europe/Lisbon,Portugal,22,10/28|2490|Asphalt|OPEN,EASA Aerodrome License,PHONE|+351 289 800 800,fao_terminal.jpg
```

#### Successful Response (`200 OK`)

Empty body.

Notes:

* Column 10 is used both as `routeDistance` and, truncated to an integer, as `gateCount`.
* `facilities.services` is filled with the raw runway entries of column 11; `terminalCount` is always `1`.
* Imported certifications have category `"STANDARD"` and `startsAt = expiresAt = today`.
* Invalid decimals are stored as `0.0`; invalid enum names return `400`.
* Each airport is saved in its own `AirportService` transaction: if a later row fails (for example a duplicate IATA → `409`), the airports imported before it remain persisted.

---

### 3.3. Add Certification

* **Endpoint:** `POST /api/airports/{iataCode}/certifications`

#### Request Example

```json
{
  "name": "ICAO Safety Certification",
  "category": "SAFETY",
  "startsAt": "2026-01-01",
  "expiresAt": "2030-01-01"
}
```

#### Successful Response (`201 Created`)

```json
{
  "name": "ICAO Safety Certification",
  "category": "SAFETY",
  "startsAt": "2026-01-01",
  "expiresAt": "2030-01-01"
}
```

Notes:

* Airport must exist → otherwise `409` (`"Airport does not exist"`).
* `expiresAt` before `startsAt` fails the `@AssertTrue` check on the request → `400` (`validationErrors` contains `"Expiry date cannot be before start date"`). Missing dates pass this check.
* Duplicate certification names are not checked.

---

### 3.4. Get Airport

* **Endpoint:** `GET /api/airports/{iataCode}`

#### Response Example (`200 OK`)

```json
{
  "iataCode": "OPO",
  "airportType": "International",
  "name": "Francisco Sá Carneiro Airport",
  "status": "OPERATIONAL",
  "location": {
    "city": "Porto",
    "longitude": -8.68139,
    "latitude": 41.2421,
    "region": "Porto District",
    "timezone": "Europe/Lisbon",
    "country": "Portugal"
  },
  "facilities": {
    "terminalCount": 1,
    "services": ["Cargo Terminal", "Restaurants", "Duty Free", "Free WiFi", "Parking"],
    "gateCount": 35
  },
  "routeDistance": 313.0,
  "runways": [
    { "runwayName": "17/35", "length": 3480.0, "orientation": "Asphalt", "status": "IN_USE" }
  ],
  "certifications": [
    { "name": "ICAO Annex 14 Compliance", "category": "SAFETY", "startsAt": "2023-01-01", "expiresAt": "2027-01-01" }
  ],
  "contacts": [
    { "type": "PHONE", "value": "+351 22 943 2400" },
    { "type": "EMAIL", "value": "info@ana.pt" }
  ],
  "airportPhotos": ["opo_terminal.jpg", "opo_runway.jpg", "opo_night_view.jpg"],
  "operationalHours": "07:00-22:00"
}
```

Notes:

* A missing airport returns `409` (`"Airport does not exist"`), not `404`.
* `operationalHours` defaults to `"07:00-22:00"` for every airport until it is changed with `PATCH /details`.
* This endpoint is also consumed by `flight-service` and `route-service`.

---

### 3.5. List Airports with Filtering

* **Endpoint:** `GET /api/airports`
* **Query parameters (all optional):**
  * `city`: exact city.
  * `country`: exact country.
  * `name`: exact airport name.

#### Response Example (`200 OK`)

A JSON array of `AirportResponse` (same shape as 3.4).

Notes:

* Filtering uses a JPA `Specification` combining the given filters with `AND`.
* Comparisons are exact and case-sensitive (`city=porto` does not match `Porto`).
* There is no paging; all matching airports are returned.

---

### 3.6. Change Airport Status

* **Endpoint:** `PATCH /api/airports/{iataCode}/status`

#### Request Example

```json
{
  "status": "UNDER_MAINTENANCE"
}
```

#### Successful Response (`200 OK`)

The updated `AirportResponse`.

Validations:

1. `status` must be a valid `AirportStatus` (`OPERATIONAL`, `CLOSED`, `UNDER_MAINTENANCE`) → otherwise `400`; a missing `status` ends in `500`.
2. Airport must exist → otherwise `409`.

Any status can move to any other status (no transition guard).

---

### 3.7. Update Airport Details

* **Endpoint:** `PATCH /api/airports/{iataCode}/details`

#### Request Example

```json
{
  "name": "Porto Airport",
  "operationalHours": "24h",
  "contacts": [
    { "type": "EMAIL", "value": "porto.airport@ana.pt" }
  ]
}
```

#### Successful Response (`200 OK`)

The updated `AirportResponse`.

Notes:

* Despite being a `PATCH`, `name` and `contacts` are fully replaced: an omitted `name` sets it to `null`, and omitted `contacts` clear all contacts.
* `operationalHours` is only changed when it is sent; a blank value is rejected by `Airport.changeOperationalHours(...)` with `IllegalArgumentException` → `400` (`"Operational hours cannot be empty"`). The format is not checked.
* An unknown contact `type` returns `400`; a missing airport returns `409`.

---

### 3.8. Airport Routes

* **Endpoint:** `GET /api/airports/{iataCode}/routes`
* **Description:** Checks that the airport exists locally, loads all routes with `GET /api/routes` on `route-service`, and keeps those whose origin or destination is the airport.

#### Response Example (`200 OK`)

```json
[
  {
    "routeId": 1,
    "routeName": "OPO-LIS",
    "routeStatus": "ACTIVE",
    "routeType": "DIRECT",
    "originAirportIata": "OPO",
    "destinationAirportIata": "LIS",
    "estimatedFlightTimeHours": 0.75,
    "distanceKm": 274.0,
    "routeRequirementsDTO": { "requiredRange": 300.0, "requiredCapacity": 150 },
    "routeHistoryDTO": { "routeBegin": "2026-01-01", "routeFinish": null, "routeUsage": 0 }
  }
]
```

Notes:

* Both departing and arriving routes are included; the filtering is done in this service.
* Field names are renamed by `RouteResponse.from(...)` (`status` → `routeStatus`, `originIataCode` → `originAirportIata`, etc.).
* Any `route-service` error (unavailable, timeout after 2 s, `4xx`/`5xx`) is returned as `500`.

---

### 3.9. Busiest Airports

* **Endpoint:** `GET /api/airports/busiest`
* **Description:** Loads all routes with `GET /api/routes` from `route-service`, counts each local airport once per route where it is origin or destination, and sorts descending.

#### Response Example (`200 OK`)

```json
[
  { "iataCode": "LIS", "name": "Humberto Delgado Airport", "numberOfRoutes": 4 },
  { "iataCode": "OPO", "name": "Francisco Sá Carneiro Airport", "numberOfRoutes": 3 }
]
```

Notes:

* Airports with no routes are not included.
* Route endpoints whose IATA code is not registered locally are ignored.
* Two repository lookups are made per route (origin and destination).

---

### 3.10. Grouped Airports

* **Endpoint:** `GET /api/airports/grouped`
* **Query parameters:** `by` = `country` (default), `city` or `region`.

#### Response Example (`200 OK`)

```json
{
  "Portugal": [ { "iataCode": "OPO", "...": "..." }, { "iataCode": "LIS", "...": "..." } ],
  "Spain": [ { "iataCode": "MAD", "...": "..." } ]
}
```

Notes:

* Values are full `AirportResponse` objects.
* Any other `by` value returns `409` (`"Unknown grouping criteria: ..."`), unless there are no airports, in which case `{}` is returned.

---

## 4. Error Handling (`GlobalExceptionHandler`)

Error payload model: `ErrorDetails`

```json
{
  "timestamp": "2026-10-25T12:00:00",
  "status": 409,
  "error": "Conflict",
  "message": "Airport does not exist",
  "path": "/api/airports/XXX",
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
| `ObjectOptimisticLockingFailureException` | `409 Conflict` (`"Concurrent modification detected. Please retry the operation."`) |
| `IllegalStateException` | `409 Conflict` |
| `Exception` | `500 Internal Server Error` |

Notes:

* `ResourceNotFoundException` is never raised by the service's own lookups; `MethodArgumentNotValidException` is only raised by the certification date check.
* There is no dedicated handler for `RestClient` errors from `route-service`; they map to `500`.
* The `403` from the gateway secret filter is written directly by the filter as plain text, not as `ErrorDetails`.
