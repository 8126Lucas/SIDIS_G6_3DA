# Airport Service — Domain and Resilience (As Implemented)

## 1. Domain Model

The service has a single aggregate, **Airport**, with embedded value objects and owned child entities.

### 1.1. `Airport` (table `company_registered_airports`)

* `iataCode`: assigned identifier (`@Id`), validated as `[A-Z]{3}` on registration.
* `version`: optimistic locking field (`@Version`).
* `airportType`, `name`: free text.
* `status`: enum `AirportStatus` (`OPERATIONAL`, `CLOSED`, `UNDER_MAINTENANCE`).
* `airportLocation`: embedded `AirportLocation` value object:
  * `city`, `country`, `timezone`, `region`, `latitude`, `longitude` (all required).
* `facilities`: embedded `Facilities` value object:
  * `terminalCount` (required), `gateCount` (required), `services` (element collection in table `facility_services`).
* `routeDistance`: decimal, stored but not used by any business rule.
* `airportRunways`: `@OneToMany` `Runway` (table `company_airports_runways`): `runwayName`, `length`, `orientation`, `status` (`RunwayStatus`: `OPEN`, `IN_USE`, `UNDER_MAINTENANCE`).
* `certifications`: `@OneToMany` `Certification` (table `company_airports_certifications`): `name`, `category`, `startsAt`, `expiresAt`.
* `contacts`: element collection of embedded `Contact` in table `airport_contacts`: `type` (`ContactType`: `PHONE`, `EMAIL`, `FAX`, `SOCIAL_NETWORK`), `value`.
* `airportPhotos`: element collection in table `airport_images`.

Runways and certifications are joined by column `airport_iata_code` and saved in cascade (`CascadeType.ALL`).

Aggregate behavior in `Airport`:

* `addCertification(...)`: appends a certification and sets its back-reference.
* `changeStatus(...)`: sets the new status.
* `updateDetails(name, operationalHours, contacts)`: replaces `name` and `contacts`; `operationalHours` is ignored.

There are no invariants enforced inside the entity (no null checks, no date checks on certifications, no runway rules).

## 2. Lifecycle Rules

Airports are created with the status sent by the client (Bootstrap seeds all of them as `OPERATIONAL`).

`AirportService.updateStatus(...)` accepts any `AirportStatus` value:

| Requested Status | Effect |
| :--- | :--- |
| `OPERATIONAL` | `changeStatus(OPERATIONAL)` |
| `CLOSED` | `changeStatus(CLOSED)` |
| `UNDER_MAINTENANCE` | `changeStatus(UNDER_MAINTENANCE)` |
| Unknown value | Rejected by `AirportStatus.valueOf(...)` with `IllegalArgumentException` (`400`) |

There is no transition guard, and the airport status is not checked by other operations (for example, certifications can be added to a `CLOSED` airport).

`RunwayStatus` is only set on creation; there is no endpoint to change a runway.

## 3. Use Cases

| Use Case / Service | Responsibility |
| :--- | :--- |
| `AirportService.execute(...)` | Validate IATA code and uniqueness, persist a new `Airport` |
| `AirportService.addCertification(...)` | Add a certification to an existing airport |
| `AirportService.getAirportByIata(...)` | Lookup by IATA code |
| `AirportService.searchAirports(...)` | `Specification`-based search by city, country, name |
| `AirportService.updateStatus(...)` | Status change |
| `AirportService.updateDetails(...)` | Replace name and contacts |
| `AirportService.groupAirportsBy(...)` | Group by country or city |
| `AirportService.findRoutesByAirport(...)` | Routes departing from the airport (`route-service`) |
| `AirportService.busiestAirports()` | Rank airports by number of routes (`route-service`) |
| `AirportCsvService.importFile(...)` | Parse a CSV into `Airport` objects (no persistence) |

`AirportService` is annotated with `@UseCase` (Spring `@Service`, `@Validated`, `@Transactional`) and also with `jakarta.transaction.Transactional`; every public method runs in its own transaction.

## 4. External Integrations

Synchronous `RestClient` integration (Eureka load-balanced, bean `routesRestClient` with base URL `http://route-service`):

* `route-service`
  * `GET /api/routes/from/{airportIataCode}` (airport routes)
  * `GET /api/routes` (busiest airports)
  * `GET /api/routes?origin=&destination=` (declared in `findByOriginAirport_IataCodeOrDestinationAirport_IataCode`, not used)

All three endpoints are exposed by `route-service`, and `RouteClientDTO` matches its `RouteResponse` (`routeId`, `routeName`, `status`, `type`, `originIataCode`, `destinationIataCode`, `estimatedFlightTimeHours`, `distanceKm`, `routeRequirements`, `routeHistory`).

Contract notes:

* `GET /api/routes/from/{iata}` returns only routes with that airport as **origin**.
* `RouteResponse` re-exposes the data with different field names (`routeStatus`, `routeType`, `originAirportIata`, `destinationAirportIata`, `routeRequirementsDTO`, `routeHistoryDTO`).
* `GlobalRestClientConfig` declares a second `@LoadBalanced` `RestClient.Builder` that is not injected anywhere.

Inbound consumers of this service:

* `flight-service` → `GET /api/airports/{iataCode}`.
* `route-service` → `GET /api/airports/{iataCode}`.

Both consumers receive `409 Conflict` (not `404`) when the airport does not exist.

## 5. Analytics

* **Busiest airports:** for each route, the origin and destination airports found locally are incremented by one; results are sorted by count descending. Airports without routes are omitted.
* **Grouping:** in-memory `Collectors.groupingBy` over all airports, keyed by `country` or `city`.

## 6. Resilience and Concurrency

Optimistic locking:

* Implemented with `@Version` on `Airport.version`.
* `updateStatus`, `updateDetails`, and `addCertification` load and save the airport in the same transaction; a concurrent update fails with `ObjectOptimisticLockingFailureException`.

Exception handling:

* Centralized by `GlobalExceptionHandler`.
* `DomainException` maps to `409 Conflict`; it is used for invalid IATA codes, duplicates, missing airports, and unknown grouping criteria.
* Malformed JSON and `IllegalArgumentException` (unknown enum names) map to `400 Bad Request`.
* `ResourceNotFoundException` maps to `404 Not Found`, but is never thrown.
* Unhandled exceptions map to `500 Internal Server Error`.

Current gaps:

* Missing resources return `409` instead of `404`.
* No Bean Validation constraints on requests; missing nested objects (`location`, `facilities`, `runways`) cause `NullPointerException` (`500`).
* No dedicated handler for `OptimisticLockingFailureException`.
* No timeout, retry, or circuit breaker around `RestClient` calls; any `route-service` failure becomes `500`.
* The CSV import is not atomic: each airport is saved in a separate transaction.
* `UseCaseLoggingAdvice` uses the pointcut `within(UseCase)`, which matches the `UseCase` annotation type itself rather than classes annotated with it (`within(@com.sidis.airportservice.UseCase *)`), so no use case calls are logged.

Security:

* `airport-service` has no security dependency and performs no authentication or authorization.
* Role rules for `/api/airports/**` are declared in `auth-service`'s `SecurityConfig`, which only applies to requests handled by `auth-service`.

## 7. Bootstrap

`Bootstrap` (`ApplicationRunner`) seeds data when the airport table is empty:

* 5 airports, all `OPERATIONAL` and of type `International`:
  * `OPO` — Francisco Sá Carneiro Airport (Porto, Portugal), 1 runway.
  * `LIS` — Humberto Delgado Airport (Lisbon, Portugal), 2 runways (one `UNDER_MAINTENANCE`).
  * `MAD` — Adolfo Suárez Madrid–Barajas Airport (Madrid, Spain), 4 runways.
  * `CDG` — Charles de Gaulle Airport (Paris, France), 3 runways.
  * `LHR` — London Heathrow Airport (London, United Kingdom), 2 runways.
* Each airport has facilities, certifications, contacts (phone and email), and photos.
