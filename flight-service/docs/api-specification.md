# Flight Operations Service — REST API Specification (As Implemented)

This document reflects the current implementation in `FlightController`, `RouteService`, and related classes.

---

## 1. General Service Configuration

* **Base URL (local):** `http://localhost:8084`
* **API Prefix:** `/api/flights`
* **Content-Type:** `application/json`

---

## 2. Implemented Endpoints Summary

| Method | Endpoint Path | Description | Status Codes |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/flights/scheduled` | Schedule a new flight after synchronous validations | `201`, `400`, `409`, `500` |
| `GET` | `/api/flights/aircraft/{registrationNumber}` | List scheduled flights for one aircraft | `200`, `500` |
| `GET` | `/api/flights/reports/route-utilization` | Report of route utilization by flight count | `200`, `500` |



---

## 3. Endpoint Details

### 3.1. Schedule Flight

* **Endpoint:** `POST /api/flights/scheduled`
* **Consumes:** `application/json`
* **Validation annotations on request:**
  * `routeId`: `@NotNull`
  * `aircraftRegistrationNumber`: `@NotBlank`
  * `scheduledDeparture`: `@NotNull`
  * `scheduledArrival`: `@NotNull`

#### Request Example

```json
{
  "routeId": 101,
  "aircraftRegistrationNumber": "CS-TTP",
  "scheduledDeparture": "2026-10-20T14:00:00",
  "scheduledArrival": "2026-10-20T15:30:00"
}
```

#### Successful Response (`201 Created`)

```json
{
  "flightId": 1,
  "routeId": 101,
  "aircraftRegistrationNumber": "CS-TTP",
  "status": "SCHEDULED",
  "scheduledDeparture": "2026-10-20T14:00:00",
  "scheduledArrival": "2026-10-20T15:30:00"
}
```

#### Synchronous validations executed before persistence

1. Lookup aircraft in `aircraft-service`.
2. Lookup route in `route-service`.
3. Lookup origin and destination airports in `airport-service`.
4. Validate schedule dates and ordering.
5. Validate route active status.
6. Validate airport operational status.
7. Validate aircraft availability.
8. Validate range compatibility (`aircraftRange >= requiredRange`).
9. Validate no local overlap with existing scheduled flights for same aircraft.

Notes:

* No `maintenance-service` call is implemented.
* No explicit passenger capacity rule is implemented.

---

### 3.2. List Flights by Aircraft

* **Endpoint:** `GET /api/flights/aircraft/{registrationNumber}`
* **Description:** Returns all stored flights for the given aircraft registration.

#### Response Example (`200 OK`)

```json
[
  {
    "flightId": 1,
    "routeId": 101,
    "aircraftRegistrationNumber": "CS-TTP",
    "status": "SCHEDULED",
    "scheduledDeparture": "2026-10-20T14:00:00",
    "scheduledArrival": "2026-10-20T15:30:00"
  }
]
```

---

### 3.3. Route Utilization Report

* **Endpoint:** `GET /api/flights/reports/route-utilization`
* **Description:** Aggregates flights by route and returns route metadata plus `flightCount`.

#### Response Example (`200 OK`)

```json
[
  {
    "routeId": 101,
    "routeName": "OPO-LIS",
    "originIata": "OPO",
    "destinationIata": "LIS",
    "flightCount": 4
  }
]
```

Important:

* This report does **not** compute accumulated flight hours.

---

## 4. Error Handling (`GlobalExceptionHandler`)

Error payload model: `ErrorDetails`

```json
{
  "timestamp": "2026-10-14T12:00:00",
  "status": 409,
  "error": "Conflict",
  "message": "Aircraft range does not comply with route requirements",
  "path": "/api/flights/scheduled",
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
| `Exception` | `500 Internal Server Error` |

Note:

* There is no dedicated handler for `OptimisticLockingFailureException` in current code.