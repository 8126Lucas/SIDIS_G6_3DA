# Postman collection

Integration tests for the whole AISafe system, sent through the API Gateway.

| File | Purpose |
| --- | --- |
| `AISafe-Global-System.postman_collection.json` | The collection |
| `AISafe-Lab.postman_environment.json` | URLs of the system spread over the 5 PCs (entry through HAProxy) |
| `AISafe-Local.postman_environment.json` | URLs of a system running on one machine (gateway on `7979`) |
| `SIDIS HTTP Tests.postman_collection.json` | Older collection, kept up to date by the team and run by CI (see below) |

## Collection run by CI

The `postman-tests` job in `ci.yml` runs `SIDIS HTTP Tests.postman_collection.json` (not the global one) with `newman run ... --insecure`. It has 65 requests (aircraft, maintenance, security and validation), calls `https://localhost:7979` directly (no environment file), and logs in once per role at the start (`Setup - Login - Backoffice/Technician/Supervisor/ATCC`), saving `backofficeToken`, `technicianToken`, `supervisorToken` and `atccToken` in collection variables so each request uses the role it needs.

## Environments

| Variable | `AISafe - Lab (5 PCs)` | `AISafe - Local` |
| --- | --- | --- |
| `baseUrl` | `https://192.168.137.1` (HAProxy on PC1, port `443`) | `https://localhost:7979` |
| `discoveryUrl` | `http://192.168.137.1:8761` | `http://localhost:8761` |
| `aircraftDirectUrl` | `http://IP_PC2:8081`: replace `IP_PC2` with PC2's address | `http://localhost:8081` |

In the lab every request goes to HAProxy, without a port. HAProxy hands each connection to one of the two gateways (`7979` on PC1, `7980` on PC4), so the collection does not need to know which gateway answers. Live traffic per gateway: `http://192.168.137.1:8404/stats`.

`aircraftDirectUrl` is only used by the test that calls a service without the gateway, to check the `403`.

## Running it

Start the system first and wait until the services are registered in Eureka.

**Postman app**

1. Import the collection and the environment, and select `AISafe - Lab (5 PCs)` or `AISafe - Local`.
2. Turn off *Settings > General > SSL certificate verification*. HAProxy passes TLS through, so the client still sees the gateway's self-signed certificate.
3. Open any request and send it, or use *Run collection* on the collection or on a folder.

**Command line**

```sh
npx newman run docs/postman/AISafe-Global-System.postman_collection.json \
  -e docs/postman/AISafe-Lab.postman_environment.json -k
```

Use `AISafe-Local.postman_environment.json` instead when the system runs on one machine.

`-k` accepts the self-signed certificate. Add `--folder "06 - Flight Service"` to run one folder.

## Organisation

| Folder | Content |
| --- | --- |
| `00 - Setup` | Logs in and resets the generated data. Use it to repeat a run in the Postman app |
| `01 - Auth Service` | Login by role, login errors |
| `02 - Gateway and Discovery` | JWT filter, direct access to a service, unknown routes, Eureka registry |
| `03 - Aircraft Service` | Catalog, Hangar, Analytics |
| `04 - Airport Service` | Registration, Lookup, Updates, Analytics |
| `05 - Route Service` | Create, Lookup, Update, Analytics and export |
| `06 - Flight Service` | Schedule, Lookup and reports |
| `07 - Maintenance Service` | Templates, Records, Reports, Parts, Complete record |
| `08 - End-to-End Journeys` | New aircraft to scheduled flight; maintenance lifecycle |
| `09 - Known Issues` | Bugs found in the API, pinned at their current behaviour |

Request names follow `Action - expected status [user story]`, for example `Create model - 201 [US101]`.

## Authentication and data

* Requests inherit `Bearer {{token}}` from the collection. A collection-level script logs in as `backoffice` when the token is missing or about to expire, so any request can be sent on its own.
* The same script generates unique values for each run (`modelName`, `aircraftReg`, `airportIata`, ...). In the app these values persist between runs, so run `00 - Setup` before repeating the collection. Newman starts fresh every time.
* Each folder runs on its own, but the requests inside a service folder depend on each other. Run them in order.
* Some tests check the seeded data, such as the costs of CS-TUA. If they fail after manual changes, restart the services to reset their in-memory databases.
* Since iteration 2, `aircraft-service` checks roles (`@PreAuthorize`), so some requests need a token of another role than `backoffice`. The role tokens saved by `01 - Auth Service` can be used for that. `maintenance-service`, `airport-service`, `flight-service` and `route-service` declare roles with `@PreAuthorize` but do not enforce them (see [`../global-network-and-ports.md`](../global-network-and-ports.md), section 7.2).

## Expected statuses

The tests describe the API as implemented. In particular, `DomainException` answers `409` in most services (an unknown airport or route, invalid credentials) and `404` in maintenance-service.

## Known issues

Each test in `09 - Known Issues` pins a wrong behaviour, so a run stays green and the bug stays visible. If one of those requests starts failing, the bug was probably fixed: update the expectation and move the request to its service folder.

| Issue | Current | Should be |
| --- | --- | --- |
| Duplicate model overwrites the existing one | `201` | `409` |
| `GET /api/hangar/analytics/fuel-efficiency` when flights exist | `500` | `200` |
| `GET /api/hangar/aircraft/{reg}/compatible-routes` | Always empty while `meanRange` is 0 | The routes the aircraft can fly |
| Reactivating a suspended route | `409` | `200` |
| `GET /api/maintenance/records` without `aircraft` | `500` | `200` |
| `GET /api/routes/alternatives` without a required parameter | `500` | `400` |

## Status after iteration 2

The expectations describe the API of iteration 1, and every request is sent as `backoffice`. Against commit `1935a82`, 89 of 323 assertions fail:

| Cause | Failed assertions |
| --- | --- |
| Role checks (RBAC) in `aircraft-service`: the endpoint needs `ATCC`, the collection sends `backoffice` | 14 |
| A request needs an id that an earlier, failed request should have stored | 9 |
| Behaviour of the current code (below) | 66 |

| Area | Behaviour at `1935a82` | Cause | At `8fbc480` |
| --- | --- | --- | --- |
| `POST /api/hangar/aircraft` | Always `404 Aircraft not found in database` | The controller changed the availability before creating the aircraft | Fixed in code |
| `POST /api/flights/scheduled` | `409 Aircraft range information is unavailable` | The model projection in Redis lost range, fuel and speed | Fixed in code |
| `POST /api/maintenance/templates` | `201` with no body, so the template id is not stored | Response changed from `200` with the template | Returns `201` **with** the template again |
| `GET /api/maintenance/records` | `500 No property 'findAll' found` | JPA `Specification` used on a Redis repository | Fixed in code (filtering in memory) |
| `/api/maintenance/parts/*` | `500` | `NullPointerException` on records without used parts | Fixed in code |
| Maintenance calls to `aircraft-service` | `500` (from a `403`) | The caller's token is forwarded and its role was not accepted by `aircraft-service` | Fixed for `GET /api/hangar/aircraft/{reg}`; the aircraft list used by alerts still needs `ATCC` |
| Journeys 1 and 2 | Fail in chain | No aircraft could be created and no flight could be scheduled | Should pass once the two fixes above are in place |

The `Bootstrap` failure on an empty maintenance database was also fixed (see [`../data-architecture-cqrs.md`](../data-architecture-cqrs.md), section 7).

The last column is read from the code: the global collection was not run again against `8fbc480`. It still sends every request as `backoffice`, so the 14 role failures remain until each request logs in with the role it needs (the collection script supports per-role tokens, see `01 - Auth Service`). `09 - Known Issues` also needs checking: `GET /api/maintenance/records` without `aircraft` should now answer `200`, so that pinned request is expected to fail and move to its service folder.
