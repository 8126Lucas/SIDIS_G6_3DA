# SIDIS_G6_3DA (AISafeFMS)

Microservices architecture monorepo for the _Sistemas Distribuídos_ (SIDIS) course project.

## Service Ports
| **Service** | **Ports** |
|-------------|-----------|
| **`haproxy`** (entry point) | `443` (API), `8404` (stats) |
| **`discovery-service`** | `8761 - 8762` |
| **`APIgateway`** | `7979 - 7980` |
| **`aircraft-service`** | `8081 - 8090` |
| **`airport-service`** | `8091 - 8100` |
| **`auth-service`** | `8101 - 8110` |
| **`flight-service`** | `8111 - 8120` |
| **`maintenance-service`** | `8121 - 8130` |
| **`route-service`** | `8131 - 8140` |
| **`postgresql`** | `5432` |
| **`rabbitmq`** | `5672 - 5674` (AMQP), `15672 - 15674` (management) |
| **`redis`** | `6379 - 6393` (3 nodes per service) |
| **`prometheus`** | `9090` |

Every service runs two instances (first two ports of its range). Full matrix: [docs/global-network-and-ports.md](docs/global-network-and-ports.md).

## Database Choices
- `aircraft-service`: PostgreSQL (Write) + Redis (Read)
- `airport-service`: PostgreSQL (Write) + Redis (Read)
- `auth-service`: SQLite
- `flight-service`: PostgreSQL (Write) + Redis (Read)
- `route-service`: PostgreSQL (Write) + Redis (Read)
- `maintenance-service`: PostgreSQL (Write) + Redis (Read)

Writes go to PostgreSQL and are projected into Redis through RabbitMQ events (CQRS). Details: [docs/data-architecture-cqrs.md](docs/data-architecture-cqrs.md).

## PC Workload Distribution
Taken from the profiles in `docker-compose.yml` (each PC starts its own profile).

| **PC**            | **Services**                                                                                                                                                                        |
|-------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **PC1 (Lucas)**   | `discovery-node1`, `api-gateway-1`, `haproxy`, `prometheus`, `redis-aircraft-1`, `redis-maintenance-1`                                                                              |
| **PC2 (Lucas)**   | `auth-service-1`, `aircraft-service-1`, `maintenance-service-1`, `redis-aircraft-2`, `redis-maintenance-2`                                                                          |
| **PC3 (Gustavo)** | `discovery-node2`, `auth-service-2`, `airport-service-1`, `maintenance-service-2`, `rabbitmq-3`, `redis-aircraft-3`, `redis-maintenance-3`                                          |
| **PC4 (Tiago)**   | `api-gateway-2`, `airport-service-2`, `flight-service-1`, `route-service-1`, `redis-airport-1`, `redis-flight-1`, `redis-route-1`                                                   |
| **PC5 (Martim)**  | `aircraft-service-2`, `flight-service-2`, `route-service-2`, `postgresql-1`, `rabbitmq-1`, `rabbitmq-2`, `redis-airport-2`, `redis-airport-3`, `redis-flight-2`, `redis-flight-3`, `redis-route-2`, `redis-route-3` |

## Instructions
- **Configure each PC:** define `IP_PC1` … `IP_PC5`, `EUREKA_URL_1` (`http://<IP_PC1>:8761/eureka/`) and `EUREKA_URL_2` (`http://<IP_PC3>:8762/eureka/`). `.env` only sets `REPO_OWNER`.
- **Start a PC:** `docker compose --profile pc<n> up -d` (for example `--profile pc1` on PC1). Local images need `./mvnw clean package -DskipTests` first.
- **Access the API:** through HAProxy at https://192.168.137.1 (self-signed certificate of the gateway; log in with `POST /api/auth/login` and send the token as `Authorization: Bearer <token>`). Each gateway can also be reached directly on `7979` / `7980`.
- **Access Eureka (Discovery):** `http://<IP_PC1>:8761` or `http://<IP_PC3>:8762`
- **HAProxy dashboard:** http://192.168.137.1:8404/stats
- **Run on one machine (as CI does):** start PostgreSQL (with the five `*_db` databases), RabbitMQ and one Redis per service, then run each jar with `-Dspring.profiles.active=test` (discovery with `test,node1`). See [docs/ci-cd-pipelines.md](docs/ci-cd-pipelines.md).
- **Postman:** collections and environments in [docs/postman](docs/postman/README.md).
- **Documentation:** [docs/README.md](docs/README.md) (global) and `docs/` inside each service.

> **Startup Note:** It is expected to see `Connection Refused` errors in the logs during the first 30-60 seconds. Wait until all services appear in the Eureka dashboard before issuing HTTP requests; the gateway can still answer `503` for about 30 seconds after a service registers, until its registry cache refreshes.

## Default Users
| **Username / password** | **Role** |
|-------------------------|----------|
| `admin` / `admin123` | `ADMIN` |
| `atcc` / `atcc123` | `ATCC` |
| `backoffice` / `backoffice123` | `BACKOFFICE` |
| `technician` / `technician123` | `MAINTENANCE_TECHNICIAN` |
| `supervisor` / `supervisor123` | `MAINTENANCE_SUPERVISOR` |

## CI/CD Architecture
This repository implements automated workflows via GitHub Actions:
- **CI (`ci.yml`):** Compiles all modules, then starts the whole system on the runner (Spring profile `test`, with PostgreSQL, RabbitMQ and Redis as service containers) and runs the Java E2E tests (`./mvnw verify`) and the Postman collection (`SIDIS HTTP Tests`) with Newman.
- **CD (`cd.yml`):** Triggers on CI success, builds individual Docker images via a matrix strategy, and pushes them to the GitHub Container Registry (GHCR).

*Note: The `docker-compose.yml` adopts a hybrid approach, capable of pulling from GHCR while falling back to local builds if registry credentials are not present.*

## Known Limitations
- **Role checks:** every business controller declares `@PreAuthorize` roles, but only `aircraft-service` enforces them.
- **Projection errors:** a RabbitMQ event that cannot be projected into Redis is redelivered forever (no dead-letter queue).
- **Clustering:** the Redis containers are not started in cluster mode and the three RabbitMQ nodes are independent brokers.

Full list per service in each `docs/README.md`, and globally in [docs/data-architecture-cqrs.md](docs/data-architecture-cqrs.md) (section 7) and [docs/global-network-and-ports.md](docs/global-network-and-ports.md) (section 7).

## Project Structure
```bash
SIDIS_G6_3DA/
├── .github/
│   ├── workflows/
│   │   ├── cd.yml
│   │   └── ci.yml
│   └── CODEOWNERS
├── aircraft-service/
│   ├── docs/
│   ├── src/
│   │   ├── main/
│   │   └── test/
│   ├── Dockerfile
│   └── pom.xml
├── airport-service/
├── APIgateway/
├── auth-service/
├── discovery-service/
├── flight-service/
├── maintenance-service/
├── route-service/
├── data/                  # volumes mounted by docker-compose (e.g. auth-service SQLite files)
├── docs/
│   ├── postman/
│   └── ...                # global documentation and diagrams
├── .env
├── .gitignore
├── docker-compose.yml
├── haproxy.cfg
├── postgres-init.sql
├── prometheus.yml
├── LICENSE
├── mvnw / mvnw.cmd
├── pom.xml
└── README.md
```

## Colaborators
- Lucas Santos
- Gustavo Abrantes
- Martim Ferreira
- Tiago Silva
