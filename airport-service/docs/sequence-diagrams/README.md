# Airport Service — Sequence Diagrams and Dynamic Views (As Implemented)

This document describes runtime behavior aligned with the current code implementation.

---

## 1. Scenario 1: Read and Grouping Flows

### Description

Scenario 1 contains local read operations exposed by `AirportController`:

* **Flow A:** `GET /api/airports/{iataCode}`
* **Flow B:** `GET /api/airports?city=&country=&name=`
* **Flow C:** `GET /api/airports/grouped?by=country|city`

### Sequence Diagram Source

* `scenario-1-query-airports.puml`

### Key Steps

#### Flow A — Airport by IATA Code

1. Client requests one airport.
2. Controller delegates to `AirportService.getAirportByIata(...)`.
3. Service calls `AirportRepository.findByIataCode(...)`.
4. Missing airport throws `DomainException` → `409`.
5. Controller maps to `AirportResponse` and returns `200 OK`.

#### Flow B — Filtered Search

1. Client sends optional filters.
2. Controller delegates to `AirportService.searchAirports(...)`.
3. Service composes a JPA `Specification` (city, country, name; exact match).
4. Repository executes the query; controller returns the list as `200 OK`.

#### Flow C — Grouped Airports

1. Client sends the `by` criteria (default `country`).
2. Service loads all airports and groups them in memory.
3. Unknown criteria throws `DomainException` → `409`.
4. Controller maps each group to `AirportResponse` and returns `200 OK`.

---

## 2. Scenario 2: Register Airport, Import CSV, and Change Status

### Description

Scenario 2 describes the write flows in `POST /api/airports`, `POST /api/airports/import`, and `PATCH /api/airports/{iataCode}/status`.

### Sequence Diagram Source

* `scenario-2-register-airport.puml`

### Key Steps

#### Flow A — Register Airport

1. Client sends `POST /api/airports` with IATA code, location, facilities, runways, certifications, contacts, and photos.
2. Controller builds the `Airport` aggregate; unknown enum names fail with `IllegalArgumentException` → `400`.
3. `AirportService.execute(...)` validates the IATA format (`409`) and uniqueness (`409`).
4. Repository persists the airport and its child rows; `201 Created` is returned.

#### Flow B — Import CSV

1. Client uploads a CSV file (`multipart/form-data`, part `file`).
2. `AirportCsvService` skips the header and rows with fewer than 15 columns, and builds one `Airport` per row.
3. Controller calls `AirportService.execute(...)` for each airport, each in its own transaction.
4. A failing row stops the import (`409` / `500`); previously saved airports remain. Otherwise `200 OK` is returned.

#### Flow C — Change Status

1. Client sends `PATCH /api/airports/{iataCode}/status`.
2. Unknown status → `400`.
3. Service loads the airport (`409` if missing), calls `changeStatus(...)`, and saves it (version incremented).
4. Controller returns the updated `AirportResponse` with `200 OK`.

---

## 3. Scenario 3: Inter-Service Route Analytics

### Description

Scenario 3 describes flows that depend on `route-service`:

* **Flow A:** `GET /api/airports/{iataCode}/routes` → `GET /api/routes/from/{iataCode}`
* **Flow B:** `GET /api/airports/busiest` → `GET /api/routes`

### Sequence Diagram Source

* `scenario-3-route-analytics.puml`

### Key Steps

#### Flow A — Airport Routes

1. Service checks that the airport exists locally (`409` if missing).
2. `RouteRestClientService` calls `GET /api/routes/from/{iataCode}` on `route-service`.
3. Each `RouteClientDTO` is mapped to `RouteResponse`.
4. Controller returns `200 OK` (only routes departing from the airport).

#### Flow B — Busiest Airports

1. `RouteRestClientService` calls `GET /api/routes` on `route-service`.
2. For each route, the origin and destination airports are looked up locally.
3. Each airport found is counted once per route.
4. Results are sorted by number of routes (descending); controller returns `200 OK`.

Note:

* There is no timeout, retry, or fallback; any `route-service` error is returned as `500`.

---

## 4. Exception and Resilience Summary

| Exception Type | Current Mapping |
| :--- | :--- |
| `ResourceNotFoundException` | `404 Not Found` (not thrown by current code) |
| `MethodArgumentNotValidException` | `400 Bad Request` (not raised; no constraints) |
| `HttpMessageNotReadableException` | `400 Bad Request` |
| `IllegalArgumentException` | `400 Bad Request` |
| `DomainException` | `409 Conflict` |
| `Exception` | `500 Internal Server Error` |

Optimistic locking exists (`@Version` on `Airport`), but there is no dedicated handler for `OptimisticLockingFailureException` in the current `GlobalExceptionHandler`. Downstream `RestClient` failures are not handled separately.
