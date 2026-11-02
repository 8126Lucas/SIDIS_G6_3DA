# Flight Operations Service — REST API Specification (As Implemented)

This document reflects the current implementation in `FlightController`, `RouteService`, and related classes.

---

## 1. General Service Configuration

* **Base URL (via API Gateway):** `https://localhost:7979`
* **Base URL (service, local):** `http://localhost:8111`
* **API Prefix:** `/api/flights`
* **Content-Type:** `application/json`

### 1.1. Required Headers

| Header | Where it is checked | Effect when missing / invalid |
| :--- | :--- | :--- |
| `Authorization: Bearer <JWT>` | API Gateway (`JwtAuthentication` filter) | `401 Unauthorized` (empty body) from the gateway |
| `X-Gateway-Secret: sidis-g6-secret` | `FlightSecurityConfig.gatewaySecretFilter` | `403 Forbidden`, body `Unauthorized Access! Please use the API Gateway!` |

* The gateway adds `X-Gateway-Secret` and `X-User-Name` after validating the JWT, so clients only send `Authorization`.
* The service itself does not validate the JWT and does not read `X-User-Name`; all requests that pass the secret filter are `permitAll()`.
* Paths containing `/h2-console`, `/swagger-ui` or `/api-docs` skip the secret filter.

---

## 2. Implemented Endpoints Summary

All endpoints additionally return `403` when `X-Gateway-Secret` is missing (and `401` at the gateway when the JWT is missing or invalid).

| Method | Endpoint Path | Description | User Story | Status Codes |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/flights` | List all flights | — | `200` |
| `POST` | `/api/flights/scheduled` | Schedule a new flight after synchronous checks | US212 | `201`, `400`, `409`, `500` |
| `GET` | `/api/flights/aircraft/{registrationNumber}` | List the flights of one aircraft | US213 | `200` |
| `GET` | `/api/flights/reports/route-utilization` | Number of flights per route | US229 | `200` |

---

## 3. Endpoint Details

### 3.1. List Flights

* **Endpoint:** `GET /api/flights` (also `/api/flights/`)
* **Description:** Returns every stored flight as `FlightResponse[]`, in repository order.
* **Consumers:** `aircraft-service` uses it for the US204 `assignments` ranking and for fuel efficiency per route.

---

### 3.2. Schedule Flight (US212)

* **Endpoint:** `POST /api/flights/scheduled`
* **Validation annotations on request:**
  * `routeId`: `@NotNull`
  * `aircraftRegistrationNumber`: `@NotBlank`
  * `scheduledDeparture`: `@NotNull`
  * `scheduledArrival`: `@NotNull`

#### Request Example

```json
{
  "routeId": 1,
  "aircraftRegistrationNumber": "D-ABXA",
  "scheduledDeparture": "2026-12-01T08:00:00",
  "scheduledArrival": "2026-12-01T09:00:00"
}
```

#### Successful Response (`201 Created`)

```json
{
  "flightId": 7,
  "routeId": 1,
  "aircraftRegistrationNumber": "D-ABXA",
  "status": "SCHEDULED",
  "scheduledDeparture": "2026-12-01T08:00:00",
  "scheduledArrival": "2026-12-01T09:00:00"
}
```

#### Execution steps

1. `aircraft-service` `GET /api/hangar/aircraft/{registrationNumber}` → `409` (`"Aircraft does not exist"`) if not found.
2. `aircraft-service` `GET /api/catalog/{modelName}` (model specs) → `409` (`"Aircraft does not exist"`) if not found.
3. `route-service` `GET /api/routes/{routeId}` → `409` (`"Route does not exist"`) when `route-service` answers `409`.
4. `airport-service` `GET /api/airports/{iataCode}` for origin and destination → `409` (`"Airport does not exist"`) on any `4xx`.
5. Business rules, in order (all `DomainException` → `409`):
   1. `scheduledArrival` must be after `scheduledDeparture` (`"Scheduled arrival must be after departure"`).
   2. Route status must be `ACTIVE` (`"Route is not active"`).
   3. Both airports must be `OPERATIONAL` (`"Airport is not operational"`).
   4. Aircraft status must be `AVAILABLE` (`"Aircraft is not available"`).
   5. Aircraft range ≥ required range (`"Aircraft range does not comply with route requirements"`). Aircraft range is `meanRange` when it is greater than `0`, otherwise the model's `maximumRangeKm`; required range is `routeRequirements.requiredRange`, otherwise the route's `distanceKm`.
   6. Model `standardSeatingCapacity` ≥ `routeRequirements.requiredCapacity`, when the route defines one (`"Aircraft has insufficient capacity"`).
   7. No stored flight of the same aircraft overlaps the new interval (`"Aircraft is already scheduled for this period"`).
6. `route-service` `PATCH /api/routes/{routeId}` with the same destination, flight time and status, and the route history with `routeUsage + 1`.
7. The new `Flight` is saved as `SCHEDULED`.

Notes:

* With the seeded data, only `D-ABXA` is `AVAILABLE` in `aircraft-service`; the other seeded aircraft fail step 5.4.
* The overlap check (5.7) also counts cancelled and arrived flights, because it does not filter by status.
* The route usage is updated before the flight is saved; if saving fails, the counter is not rolled back.
* A model without `specs` makes the capacity check throw `NullPointerException` (`500`).
* Other downstream errors (unavailable service, timeout, `5xx`) are returned as `500`.

---

### 3.3. Flights by Aircraft (US213)

* **Endpoint:** `GET /api/flights/aircraft/{registrationNumber}`
* **Description:** Returns all stored flights of the aircraft (any status) as `FlightResponse[]`. An unknown registration returns `[]`.
* **Consumers:** `aircraft-service` uses it for the utilization view.

#### Response Example (`200 OK`, seeded data)

```json
[
  {
    "flightId": 2,
    "routeId": 1,
    "aircraftRegistrationNumber": "D-ABXA",
    "status": "SCHEDULED",
    "scheduledDeparture": "2026-06-15T10:30:00",
    "scheduledArrival": "2026-06-15T12:00:00"
  }
]
```

---

### 3.4. Route Utilization Report (US229)

* **Endpoint:** `GET /api/flights/reports/route-utilization`
* **Description:** Groups all flights by route, counts them, and sorts by `flightCount` descending, then `routeName` ascending.

#### Response Example (`200 OK`, seeded data)

```json
[
  { "routeId": 3, "routeName": "Madrid-Paris", "originIata": "MAD", "destinationIata": "CDG", "flightCount": 2 },
  { "routeId": 1, "routeName": "Porto-Lisbon", "originIata": "OPO", "destinationIata": "LIS", "flightCount": 2 },
  { "routeId": 2, "routeName": "Lisbon-Madrid", "originIata": "LIS", "destinationIata": "MAD", "flightCount": 1 },
  { "routeId": 4, "routeName": "Paris-London", "originIata": "CDG", "destinationIata": "LHR", "flightCount": 1 }
]
```

Notes:

* Route names come from the copy stored with each flight, not from `route-service`.
* The report does not compute accumulated flight hours.

---

## 4. Error Handling (`GlobalExceptionHandler`)

Error payload model: `ErrorDetails`

```json
{
  "timestamp": "2026-11-01T12:00:00",
  "status": 409,
  "error": "Conflict",
  "message": "Aircraft is not available",
  "path": "/api/flights/scheduled",
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
| `DomainException` | `409 Conflict` |
| `ObjectOptimisticLockingFailureException` | `409 Conflict` (`"Concurrent modification detected. Please retry the operation."`) |
| `IllegalStateException` | `409 Conflict` |
| `Exception` | `500 Internal Server Error` |

Notes:

* `ResourceNotFoundException` is only thrown by the unused `PeerForwarder`.
* There is no dedicated handler for `RestClient` errors from downstream services; errors that the clients do not turn into `DomainException` map to `500`.
* The `403` from the gateway secret filter is written directly by the filter as plain text, not as `ErrorDetails`.
