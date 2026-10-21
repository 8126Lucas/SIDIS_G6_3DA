# Flight Operations Service — Domain and Resilience (As Implemented)

## 1. Domain Model

The core aggregate is `Flight` persisted in table `company_flights`.

Main fields and value objects:

* `flightId`: surrogate identifier (`@Id`, generated).
* `version`: optimistic locking field (`@Version`).
* `routeId`: embedded `RouteReference` value object.
* `aircraftRegistrationNumber`: embedded `AircraftReference` value object.
* `flightStatus`: enum `FlightStatus`.
* `scheduledDeparture`, `scheduledArrival`.
* `timeOfDeparture`, `timeOfArrival`.

## 2. Scheduling Rules

Scheduling is triggered by `POST /api/flights/scheduled`.

Execution path:

1. `FlightController.scheduleFlight(...)` validates input DTO and looks up aircraft.
2. `RouteService.scheduleFlight(...)` performs route and airport lookups, applies business validations, updates route usage, and persists the new `Flight`.

Implemented validations in `RouteService.validateFlightSchedule(...)`:

* Aircraft must exist.
* `scheduledArrival` must be after `scheduledDeparture`.
* Route status must be `ACTIVE`.
* Origin and destination airport status must be `OPERATIONAL`.
* Aircraft status must be `AVAILABLE`.
* Aircraft range must satisfy route required range.
* There must be no overlapping local flight schedule for the same aircraft.

## 3. External Integrations

Synchronous `RestClient` integrations currently used:

* `route-service`
	* `GET /api/routes/{routeId}`
	* `PATCH /api/routes/{routeId}`
* `aircraft-service`
	* `GET /api/hangar/aircraft/{registrationNumber}`
* `airport-service`
	* `GET /api/airports/{iataCode}`

Not implemented:

* `maintenance-service` validation flow.
* Explicit aircraft passenger capacity validation.

## 4. Route Utilization Reporting

`GET /api/flights/reports/route-utilization` returns an ordered list of route utilization records.

Current aggregation behavior:

* Source: all flights from repository.
* Group key: `RouteReference`.
* Metric: `flightCount` (`Collectors.counting()`).
* Sort: descending by `flightCount`, then ascending `routeName`.

The report does not currently aggregate accumulated flight hours.

## 5. Resilience and Concurrency

Optimistic locking:

* Implemented with `@Version` on `Flight.version`.

Exception handling:

* Centralized by `GlobalExceptionHandler`.
* `DomainException` maps to `409 Conflict`.
* Bean validation and malformed JSON map to `400 Bad Request`.
* `ResourceNotFoundException` maps to `404 Not Found`.
* Unhandled exceptions map to `500 Internal Server Error`.

Current gap:

* No dedicated exception handler for `OptimisticLockingFailureException`.

## 6. Bootstrap

A `Bootstrap` application runner exists in the codebase and is part of startup responsibilities. This documentation page only reflects externally observable behavior and implemented API/runtime rules.