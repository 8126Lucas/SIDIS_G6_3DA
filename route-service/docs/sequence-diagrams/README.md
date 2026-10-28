# Route Service — Sequence Diagrams and Dynamic Views (As Implemented)

This document describes runtime behavior aligned with the current code implementation.

---

## 1. Scenario 1: Read Flows

### Description

Scenario 1 contains local read operations exposed by `RouteController`:

* **Flow A:** `GET /api/routes/{routeId}`
* **Flow B:** `GET /api/routes?origin=&destination=`
* **Flow C:** `GET /api/routes/active?sortBy=popularity|distance`

### Sequence Diagram Source

* `scenario-1-query-routes.puml`

### Key Steps

#### Flow A — Route by Id

1. Client requests one route.
2. Controller delegates to `RouteService.getRouteDetails(...)`.
3. Service calls `RouteRepository.findByRouteId(...)`.
4. Missing route throws `DomainException` → `409`.
5. Controller maps to `RouteResponse` and returns `200 OK`.

#### Flow B — Filtered Search

1. Client sends optional `origin` / `destination`.
2. Service validates the IATA format of the given filters (`409`).
3. Service composes a JPA `Specification` and the repository executes it.
4. Controller returns the list as `200 OK`.

#### Flow C — Active Routes

1. Client sends the sort criteria (default `popularity`).
2. Service builds a `Sort` (`distanceKm` asc or `routeHistory.routeUsage` desc).
3. Repository executes `findByStatus(ACTIVE, sort)`; controller returns `200 OK`.

---

## 2. Scenario 2: Create and Update Route

### Description

Scenario 2 describes the write flows in `POST /api/routes` and `PATCH /api/routes/{routeId}`, which depend on `airport-service`.

### Sequence Diagram Source

* `scenario-2-create-update-route.puml`

### Key Steps

#### Flow A — Create Route

1. Client sends `POST /api/routes` with name, airports, flight time, type, and requirements.
2. Controller rejects missing `requirements` (`409`).
3. `RouteService` validates IATA codes, distinct airports, positive flight time, and unique name (`409`).
4. For each airport, `AirportRestClientService` calls `GET /api/airports/{iataCode}` on `airport-service`.
5. Non-operational airports are rejected (`409`).
6. The airport DTO is converted into an `AirportReference`; with the current DTO mismatch (`location` vs `airportLocation`) this throws `NullPointerException` → `500`.
7. Intended result (not reachable today): the route is saved as `ACTIVE` with `distanceKm = 1.0` and `201 Created` is returned.

#### Flow B — Update Route

1. Client sends `PATCH /api/routes/{routeId}` with any of destination, flight time, status.
2. Service loads the route (`409` if missing).
3. A new destination is validated and loaded from `airport-service` (same `500` as Flow A for operational airports).
4. Flight time and status changes are applied through domain methods; invalid transitions throw `IllegalStateException` → `500`.
5. The route is saved (version incremented) and `200 OK` is returned.

---

## 3. Scenario 3: Network Export and Total Distance

### Description

Scenario 3 describes the network-wide flows:

* **Flow A:** `GET /api/routes/export/geojson` (US228)
* **Flow B:** `GET /api/routes/export/kml` (US228)
* **Flow C:** `GET /api/routes/total-distance`

### Sequence Diagram Source

* `scenario-3-export-network.puml`

### Key Steps

#### Flow A / B — GeoJSON and KML Export

1. Controller delegates to `RouteExportService`.
2. Service loads all routes.
3. For each route, the origin and destination `AirportLocation` coordinates are written as a `LineString`.
4. Controller returns `200 OK` with `application/geo+json` or `application/vnd.google-earth.kml+xml`.

Note:

* Seeded routes have no `AirportLocation`, so step 3 throws `NullPointerException` → `500` while they exist.

#### Flow C — Total Distance

1. Service loads all routes and sums `distanceKm` (`null` → `0.0`).
2. Controller returns `{"totalDistanceKm": ...}` with `200 OK`.

---

## 4. Exception and Resilience Summary

| Exception Type | Current Mapping |
| :--- | :--- |
| `ResourceNotFoundException` | `404 Not Found` (not thrown by current code) |
| `MethodArgumentNotValidException` | `400 Bad Request` (not raised; no constraints) |
| `HttpMessageNotReadableException` | `400 Bad Request` |
| `IllegalArgumentException` | `400 Bad Request` |
| `DomainException` | `409 Conflict` |
| `Exception` (incl. `IllegalStateException`, `RestClient` errors) | `500 Internal Server Error` |

Optimistic locking exists (`@Version` on `Route`), but there is no dedicated handler for `OptimisticLockingFailureException` in the current `GlobalExceptionHandler`. Downstream `RestClient` failures are not handled separately.
