# Route Service — REST API Specification (As Implemented)

This document reflects the current implementation in `RouteController`, `RouteService`, `RouteExportService`, and related classes.

---

## 1. General Service Configuration

* **Base URL (via API Gateway):** `https://localhost:7979`
* **Base URL (service, local):** `http://localhost:8131`
* **API Prefix:** `/api/routes`
* **Content-Type:** `application/json` (exports use `application/geo+json` and `application/vnd.google-earth.kml+xml`)

### 1.1. Required Headers

| Header | Where it is checked | Effect when missing / invalid |
| :--- | :--- | :--- |
| `Authorization: Bearer <JWT>` | API Gateway (`JwtAuthentication` filter) | `401 Unauthorized` (empty body) from the gateway |
| `X-Gateway-Secret: sidis-g6-secret` | `RouteSecurityConfig.gatewaySecretFilter` | `403 Forbidden`, body `Unauthorized Access! Please use the API Gateway!` |

* The gateway adds `X-Gateway-Secret` and `X-User-Name` after validating the JWT, so clients only send `Authorization`.
* The service itself does not validate the JWT and does not read `X-User-Name`; all requests that pass the secret filter are `permitAll()`.
* Paths containing `/h2-console`, `/swagger-ui` or `/api-docs` skip the secret filter.

---

## 2. Implemented Endpoints Summary

All endpoints additionally return `403` when `X-Gateway-Secret` is missing (and `401` at the gateway when the JWT is missing or invalid).

| Method | Endpoint Path | Description | User Story | Status Codes |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/routes` | Create a route | — | `201`, `400`, `409`, `500` |
| `PATCH` | `/api/routes/{routeId}` | Update destination, flight time, status, history | — | `200`, `400`, `409`, `500` |
| `GET` | `/api/routes/{routeId}` | Get one route | — | `200`, `409` |
| `GET` | `/api/routes/{routeId}/history` | Get the route history | — | `200`, `409` |
| `GET` | `/api/routes` | List routes with optional origin/destination filters | — | `200`, `409` |
| `GET` | `/api/routes/from/{airportIataCode}` | Routes departing from an airport | — | `200`, `409` |
| `GET` | `/api/routes/active` | Active routes sorted by popularity or distance | — | `200` |
| `GET` | `/api/routes/alternatives` | Active routes for the same origin/destination pair | — | `200`, `500` |
| `GET` | `/api/routes/total-distance` | Sum of `distanceKm` of all routes | — | `200` |
| `GET` | `/api/routes/export/geojson` | Route network as GeoJSON | US228 | `200`, `409` |
| `GET` | `/api/routes/export/kml` | Route network as KML | US228 | `200`, `500` |

User stories are only referenced in the code for the export endpoints (comments in `RouteController`).

---

## 3. Endpoint Details

### 3.1. Create Route

* **Endpoint:** `POST /api/routes`
* **Consumes:** `application/json`
* **Validation annotations on request:** none.

#### Request Example

```json
{
  "routeName": "Porto - Paris",
  "originIataCode": "OPO",
  "destinationIataCode": "CDG",
  "estimatedFlightTimeHours": 2.2,
  "type": "DIRECT",
  "requirements": {
    "requiredRange": 1300.0,
    "requiredCapacity": 180
  }
}
```

#### Successful Response (`201 Created`)

The created `RouteResponse` (same shape as 3.3).

Validations (in execution order):

1. `requirements` must be present → otherwise `409` (`"Route requirements are required"`).
2. Both IATA codes must match `[A-Z]{3}` → otherwise `409`.
3. Origin and destination must differ → otherwise `409`.
4. `estimatedFlightTimeHours` must be positive → otherwise `409`.
5. `routeName` must be unique → otherwise `409` (`"Route already exists"`).
6. Both airports are loaded from `airport-service` (`GET /api/airports/{iataCode}`).
7. Both airports must be `OPERATIONAL` → otherwise `409` (`"Airport is not operational"`).

Notes:

* The route starts as `ACTIVE`, `type` defaults to `DIRECT`, and history starts today with `routeUsage = 0`.
* `distanceKm` is always set to `1.0`; it is not calculated from the airport coordinates.
* A missing airport (`409` from `airport-service`) is caught by `AirportRestClientService` and rethrown as `DomainException("Airport does not exist")` → `409`.
* The airport location (`location` in `airport-service`'s response, mapped through `@JsonProperty`) is copied into the route's `AirportReference`, so new routes can be exported.
* Any other `airport-service` error (unavailable, timeout after 2 s) → `500`.
* An invalid `type` value returns `400` (malformed JSON).

---

### 3.2. Update Route

* **Endpoint:** `PATCH /api/routes/{routeId}`
* **Description:** Changes only the non-null fields among `destinationIataCode`, `estimatedFlightTimeHours`, `status`, and `history`.

#### Request Example

```json
{
  "destinationIataCode": "MAD",
  "estimatedFlightTimeHours": 1.4,
  "status": "INACTIVE",
  "history": { "routeBegin": "2026-01-01", "routeFinish": null, "routeUsage": 3 }
}
```

#### Successful Response (`200 OK`)

The updated `RouteResponse`.

Validations:

1. Route must exist → otherwise `409` (`"Route does not exist"`).
2. New destination: IATA format and different from origin (`409`), loaded from `airport-service` (`409` if missing) and must be `OPERATIONAL` (`409`).
3. `estimatedFlightTimeHours <= 0` → `400` (`IllegalArgumentException`).
4. Status transitions (see `domain-and-resilience.md`, section 2):
   * `INACTIVE` → `ACTIVE` throws `IllegalStateException` → `409`.
   * `ARCHIVED` → `INACTIVE` throws `IllegalStateException` → `409`.
   * `ARCHIVED` also sets `routeHistory.routeFinish` to today.
5. A non-null `history` replaces the whole route history (`routeBegin`, `routeFinish`, `routeUsage`) after the other changes, so it also overwrites the `routeFinish` set by `ARCHIVED`.

Note:

* `flight-service` uses this endpoint to increment `routeUsage` each time a flight is scheduled.

---

### 3.3. Get Route

* **Endpoint:** `GET /api/routes/{routeId}` (`routeId` must be numeric)

#### Response Example (`200 OK`)

```json
{
  "routeId": 6,
  "routeName": "Iberia Connect",
  "status": "ACTIVE",
  "type": "DIRECT",
  "originIataCode": "OPO",
  "destinationIataCode": "MAD",
  "estimatedFlightTimeHours": 1.5,
  "distanceKm": 500.0,
  "routeRequirements": {
    "requiredRange": 8500.0,
    "requiredCapacity": 180
  },
  "routeHistory": {
    "routeBegin": "2019-03-15",
    "routeFinish": null,
    "routeUsage": 2
  }
}
```

A missing route returns `409` (`"Route does not exist"`).

This endpoint is also consumed by `aircraft-service` and `flight-service`.

---

### 3.4. Route History

* **Endpoint:** `GET /api/routes/{routeId}/history`

#### Response Example (`200 OK`)

```json
{
  "routeBegin": "2019-03-15",
  "routeFinish": null,
  "routeUsage": 2
}
```

A missing route returns `409`.

---

### 3.5. List Routes with Filtering

* **Endpoint:** `GET /api/routes`
* **Query parameters (all optional):**
  * `origin`: origin IATA code.
  * `destination`: destination IATA code.

#### Response Example (`200 OK`)

A JSON array of `RouteResponse` (same shape as 3.3).

Notes:

* Filtering uses a JPA `Specification` combining the given filters with `AND`; without parameters, all routes are returned.
* A filter that is not 3 uppercase letters returns `409`.
* There is no paging.
* This endpoint is consumed by `aircraft-service`, `airport-service`, and `flight-service`.

---

### 3.6. Routes from Airport

* **Endpoint:** `GET /api/routes/from/{airportIataCode}`
* **Description:** Routes whose **origin** is the given airport (routes arriving there are not included).

#### Response

A JSON array of `RouteResponse`. An invalid IATA code returns `409`.

This endpoint is consumed by `airport-service` and `flight-service`.

---

### 3.7. Active Routes

* **Endpoint:** `GET /api/routes/active`
* **Query parameters:** `sortBy` = `popularity` (default) or `distance`.

#### Response

A JSON array of `ACTIVE` routes:

* `distance`: sorted by `distanceKm` ascending.
* Any other value: sorted by `routeHistory.routeUsage` descending.

Note:

* `routeUsage` only changes through the `history` field of `PATCH /api/routes/{routeId}`, which `flight-service` sends with `routeUsage + 1` when it schedules a flight.

---

### 3.8. Same-Pair Routes ("Alternatives")

* **Endpoint:** `GET /api/routes/alternatives`
* **Query parameters (required):** `origin`, `destination`.

#### Response

A JSON array of `ACTIVE` routes with exactly the given origin and destination (for example, `OPO`→`LIS` returns `TEST ROUTE 1` and `Pati&Patatá`).

Notes:

* No connecting routes (via a third airport) are calculated.
* The IATA codes are not validated.
* A missing parameter raises `MissingServletRequestParameterException`, which is handled by the generic handler → `500`.

---

### 3.9. Total Distance

* **Endpoint:** `GET /api/routes/total-distance`

#### Response Example (`200 OK`)

```json
{
  "totalDistanceKm": 6004.0
}
```

Sum of `distanceKm` of all routes, regardless of status (`null` counts as `0.0`).

---

### 3.10. Export Network (US228)

* **Endpoints:**
  * `GET /api/routes/export/geojson` → `application/geo+json`
  * `GET /api/routes/export/kml` → `application/vnd.google-earth.kml+xml`
* **Description:** Builds the document with string concatenation from all routes (any status), one `LineString` per route from origin to destination coordinates.

#### GeoJSON Feature Example

```json
{
  "type": "Feature",
  "geometry": {
    "type": "LineString",
    "coordinates": [[-8.68139, 41.2421], [2.55, 49.0097]]
  },
  "properties": {
    "routeId": 14,
    "routeName": "Porto - Paris",
    "status": "ACTIVE",
    "type": "DIRECT",
    "distanceKm": 1.0,
    "origin": "OPO",
    "destination": "CDG"
  }
}
```

#### KML Placemark Example

```xml
<Placemark>
  <name><![CDATA[Porto - Paris]]></name>
  <description><![CDATA[ACTIVE | 1.0 km]]></description>
  <LineString>
    <coordinates>-8.68139,41.2421
      2.55,49.0097</coordinates>
  </LineString>
</Placemark>
```

Notes:

* Seeded routes carry the airport coordinates, so both exports work on the seeded data.
* In the GeoJSON export, a route whose airport has no location or no coordinates throws `IllegalStateException` (`"Cannot export route: airport location is missing"` / `"... coordinates are missing"`) → `409`. The KML export reads the coordinates without this check, so the same case ends in `NullPointerException` → `500`.
* KML text values are wrapped in `CDATA`, so names such as `Pati&Patatá` are valid. GeoJSON values are still not escaped (a route name containing `"` produces invalid JSON).

---

## 4. Error Handling (`GlobalExceptionHandler`)

Error payload model: `ErrorDetails`

```json
{
  "timestamp": "2026-10-27T12:00:00",
  "status": 409,
  "error": "Conflict",
  "message": "Route does not exist",
  "path": "/api/routes/999",
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
