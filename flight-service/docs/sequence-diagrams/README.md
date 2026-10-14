# Explicação detalhada dos 2 cenários dinâmicos

# Flight Operations Service — Sequence Diagrams & Dynamic Views

This document describes the dynamic runtime behavior and message flows of the **Flight Operations Service** (`flight-service`) within the *AISafe Flight Management System*. 

The interactions are divided into two primary execution scenarios:
1. **Scenario 1:** Search/Querying Scheduled Flights and Route Utilization Metrics (Read Flow).
2. **Scenario 2:** Flight Scheduling & External Service Integration (Planned Write/Validation Flow).

---

## 1. Scenario 1: Query Flights & Route Utilization Report

### Description
Scenario 1 covers the read-only operations executed by client applications (e.g., Postman or frontend consoles) routed through the **API Gateway**. It illustrates how read requests are processed within the service boundary without external dependencies.

It encompasses two execution flows:
* **Flow A (Flight Search):** Retrieves all flights or filters flights by aircraft registration number and date range.
* **Flow B (Route Utilization Report):** Aggregates flight data to generate route utilization metrics and total accumulated flight hours.
* **Alternative Path (Error Handling):** Demonstrates how unhandled exceptions or invalid input queries are intercepted by the `GlobalExceptionHandler` and normalized into a standard `ErrorDetails` payload.

### Sequence Diagram
![Scenario 1 - Query Flights & Route Utilization Report](scenario-1-query-flights.png)

### Key Message Flow Steps

#### Flow A — Query & Filter Flights
1. **Client Request:** The client sends an HTTP `GET /api/flights` request (with optional query parameters like `aircraftRegistration` or `date`) to the **API Gateway**.
2. **Gateway Forwarding:** The Gateway routes the request to `FlightController` on port `8084`.
3. **Service Delegation:** `FlightController` delegates the read operation to `FlightSearchService`.
4. **Data Retrieval:** `FlightSearchService` executes query methods on `FlightRepository` (`findAll()` or `findByAircraftRegistrationNumberAndScheduledDepartureBetween()`).
5. **Database Execution:** `FlightRepository` reads records from the `company_flights` table in the **H2 In-Memory Database**.
6. **Response Mapping:** Retrieved `Flight` entities are mapped into DTO payloads and returned as an HTTP `200 OK` response containing a JSON array.

#### Flow B — Route Utilization Reporting
1. **Client Request:** The client issues an HTTP `GET /api/flights/utilization` request.
2. **Reporting Execution:** `FlightController` delegates the calculation to `FlightUtilizationReportService`.
3. **Aggregation:** The report service queries `FlightRepository`, aggregates accumulated flight hours per route, and packages the results into a `RouteUtilizationReport` record.
4. **Response Delivery:** The resulting metrics payload is returned to the client as HTTP `200 OK`.

---

## 2. Scenario 2: Flight Scheduling & External Service Integration

### Description
Scenario 2 illustrates the write flow when scheduling a new flight. To enforce domain integrity, the `flight-service` must coordinate synchronously with external microservices via HTTP REST calls (`RestClient`) before persisting new records:
* **`route-service`:** Validates route existence and retrieves total route distance.
* **`aircraft-service`:** Validates aircraft active status, passenger capacity, and maximum flight range.
* **`maintenance-service`:** Verifies that the aircraft is not currently undergoing maintenance during the scheduled timeframe.

### Sequence Diagram
![Scenario 2 - Flight Scheduling & External Service Integration](scenario-2-schedule-flight.png)

### Key Message Flow Steps

1. **Scheduling Request:** The client sends an HTTP `POST /api/flights` payload containing `routeId`, `aircraftRegistrationNumber`, and `scheduledDeparture` timestamps.
2. **External Validation Phase (`RestClient`):**
   * `Flight Operations Service` performs a `GET /api/routes/{routeId}` call to **`route-service`**.
   * Performs a `GET /api/hangar/aircraft/{registration}` call to **`aircraft-service`**.
   * Performs a `GET /api/maintenance/records/ongoing` call to **`maintenance-service`**.
3. **Domain Integrity Verification:** The service verifies that `aircraft.maxRange >= route.distance` and confirms no scheduling/maintenance overlaps exist.
4. **Successful Persistence (Happy Path):**
   * If all validations pass, the `Flight` entity is created and saved via `FlightRepository.save()`.
   * An `INSERT` query is executed against the `company_flights` table in H2.
   * The client receives an HTTP `201 Created` response containing the created flight resource.
5. **Validation Failure (Alternative Path):**
   * If any external service check fails or aircraft range is insufficient, a `DomainException` is thrown.
   * `GlobalExceptionHandler` catches the exception and returns an HTTP `409 Conflict` or `400 Bad Request` with an `ErrorDetails` JSON object.

---

## 3. Resilience & Exception Handling Summary

| Exception Type | Trigger Cause | Handled By | HTTP Status Code |
| :--- | :--- | :--- | :--- |
| `ResourceNotFoundException` | Requested Flight ID or route reference does not exist | `GlobalExceptionHandler` | `404 Not Found` |
| `DomainException` | Business rule violation (e.g., aircraft range shortfall or maintenance conflict) | `GlobalExceptionHandler` | `409 Conflict` |
| `MethodArgumentNotValidException` | Malformed JSON or failed Bean Validation | `GlobalExceptionHandler` | `400 Bad Request` |
| `OptimisticLockingFailureException` | Concurrent updates on the same `Flight` entity (`@Version` mismatch) | `GlobalExceptionHandler` | `409 Conflict` |