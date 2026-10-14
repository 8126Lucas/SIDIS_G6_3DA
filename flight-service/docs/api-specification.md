# Catálogo de endpoints REST e respostas

# Flight Operations Service — REST API Specification

This document provides the complete API specification for the **Flight Operations Service** (`flight-service`) in the *AISafe Flight Management System*.

---

## 1. General Service Configuration

* **Base URL:** `http://localhost:8111` (or routed via Gateway at `http://localhost:7979`)
* **API Prefix:** `/api/flights`
* **Default Instance Port:** `8111` (Allocated Port Range: `8111 - 8120`)
* **API Gateway Port:** `7979`
* **Content-Type:** `application/json`

---

## 2. API Endpoints Summary

| Method | Endpoint Path | Description | Implementation Status | Expected Status Code |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/flights/welcome` | Service Health / Welcome check | **Active** | `200 OK` |
| `GET` | `/api/flights` | Query all flights or filter by criteria | **Active** | `200 OK` |
| `GET` | `/api/flights/{flightId}` | Get scheduled flight by ID | **Active** | `200 OK` / `404 Not Found` |
| `GET` | `/api/flights/utilization` | Get route utilization & metrics report | **Active** | `200 OK` |
| `POST` | `/api/flights` | Schedule a new flight (with validations) | **Planned / Skeleton** | `201 Created` / `400` / `409` |
| `PATCH`| `/api/flights/{flightId}/status` | Update flight status (`SCHEDULED`, `DEPARTED`, etc.) | **Planned / Skeleton** | `200 OK` / `404 Not Found` |

---

## 3. Detailed Endpoint Documentation

### 3.1. Service Health Check
* **Endpoint:** `GET /api/flights/welcome`
* **Description:** Simple endpoint to verify if the service is up and running.
* **Request:** No parameters or body required.
* **Response:**
  * **Code:** `200 OK`
  * **Content:** Welcome to Flight Service

---

### 3.2. Search Scheduled Flights
* **Endpoint:** `GET /api/flights`
* **Description:** Retrieves all scheduled flights or filters them by aircraft registration number and date range.
* **Query Parameters (Optional):**
  * `aircraftRegistrationNumber` (string): e.g., `CS-TTP`
  * `from` (string, ISO-8601): e.g., `2026-10-15T08:00:00Z`
  * `to` (string, ISO-8601): e.g., `2026-10-15T20:00:00Z`
* **Request Example:**
  `GET /api/flights?aircraftRegistrationNumber=CS-TTP`
* **Response:**
  * **Code:** `200 OK`
  * **Payload Example:**

    
      ```json
      {
        "flightId": "FL-1001",
        "routeReference": {
          "routeId": "OPO-LIS-01"
        },
        "aircraftReference": {
          "registrationNumber": "CS-TTP"
        },
        "status": "SCHEDULED",
        "scheduledDeparture": "2026-10-15T10:00:00Z",
        "scheduledArrival": "2026-10-15T11:00:00Z",
        "actualDeparture": null,
        "actualArrival": null,
        "flightHours": 1.0,
        "active": true
      }
      ```
    

---

### 3.3. Route Utilization Report
* **Endpoint:** `GET /api/flights/utilization`
* **Description:** Generates an aggregated report of total accumulated flight hours and route usage.
* **Response:**
  * **Code:** `200 OK`
  * **Payload Example:**

    ```json
    {
      "totalFlights": 6,
      "accumulatedHours": 18.5,
      "mostAssignedAircraft": "CS-TTP",
      "routeUsageSummary": [
        {
          "routeId": "OPO-LIS-01",
          "flightCount": 4
        },
        {
          "routeId": "LIS-MAD-02",
          "flightCount": 2
        }
      ]
    }
    ```

---

### 3.4. Schedule New Flight (Planned Flow)
* **Endpoint:** `POST /api/flights`
* **Description:** Schedules a new flight after validating route distance and aircraft range.
* **Request Payload Example:**
    ```json
    {
      "flightId": "FL-2026",
      "routeId": "OPO-LIS-01",
      "aircraftRegistrationNumber": "CS-TTP",
      "scheduledDeparture": "2026-10-20T14:00:00Z",
      "scheduledArrival": "2026-10-20T15:00:00Z"
    }
    ```

* **Response:**
  * **Code:** `201 Created`
  * **Payload Example:**

    ```json
    {
      "flightId": "FL-2026",
      "routeReference": { "routeId": "OPO-LIS-01" },
      "aircraftReference": { "registrationNumber": "CS-TTP" },
      "status": "SCHEDULED",
      "scheduledDeparture": "2026-10-20T14:00:00Z",
      "scheduledArrival": "2026-10-20T15:00:00Z",
      "active": true
    }
    ```
---

## 4. Standardized Error Handling (`GlobalExceptionHandler`)

When a client request fails due to validation errors, non-existent resources, or domain rule violations, the service returns a standardized `ErrorDetails` JSON structure.

### Error Payload Schema (`ErrorDetails`)

```json
    {
      "timestamp": "2026-10-14T12:00:00Z",
      "status": 409,
      "error": "Conflict",
      "message": "Aircraft CS-TTP is currently under maintenance during the scheduled time.",
      "path": "/api/flights"
    }
```

### HTTP Status Code Mappings

| Exception Class | Trigger Condition | HTTP Status |
| :--- | :--- | :--- |
| `ResourceNotFoundException` | Entity or resource ID not found | `404 Not Found` |
| `DomainException` | Business logic/domain rule violation | `409 Conflict` |
| `MethodArgumentNotValidException` | Invalid request body or failed Bean Validation | `400 Bad Request` |
| `HttpMessageNotReadableException` | Malformed JSON payload | `400 Bad Request` |
| `OptimisticLockingFailureException` | Concurrent edit mismatch (`@Version`) | `409 Conflict` |
| `Exception` (Unhandled) | Generic internal error | `500 Internal Server Error` |