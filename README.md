# SIDIS_G6_3DA (AISafeFMS)

Microservices architecture monorepo for the _Sistemas Distribuídos_ (SIDIS) course project.

## Service Ports
| **Service** | **Ports** |
|-------------|-----------|
| **`discovery-service`** | `8761 - 8762` |
| **`APIgateway`** | `7979 - 7980` |
| **`aicraft-service`** | `8081 - 8090` |
| **`airport-service`** | `8091 - 8100` |
| **`auth-service`** | `8101 - 8110` |
| **`flight-service`** | `8111 - 8120` |
| **`maintenance-service`** | `8121 - 8130` |
| **`route-service`** | `8131 - 8140` |
| **`redis-database`** | `6379 - 6380` |
| **`prometheus`** | `9090` |

## Database Choices
- `aircraft-service`: PostgreSQL (Write) + Redis (Read)
- `airport-service`: PostgreSQL (Write) + Redis (Read)
- `auth-service`: SQLite
- `flight-service`: PostgreSQL (Write) + Redis (Read)
- `route-service`: PostgreSQL (Write) + Redis (Read)
- `maintenance-service`: PostgreSQL (Write) + Redis (Read)

## PC Workload Distribution
| **PC**            | **Services**                                                                                                                                                                                                                             |
|-------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **PC1 (Lucas)**   | `discovery-service-1`, `api-gateway-1`, `prometheus`, `haproxy`, `redis-aircraft-1`, `redis-maintenance-1`                                                                                                                               |
| **PC2 (Lucas)**   | `aircraft-service-1`, `maintenance-service-1`, `auth-service-1`, `redis-aircraft-2`, `redis-maintenance-2`                                                                                                                               |
| **PC3 (Gustavo)** | `api-gateway-2`, `route-service-1`, `flight-service-1`, `redis-aircraft-3`, `redis-maintenance-3`                                                                                                                                        |
| **PC4 (Tiago)**   | `discovery-service-2`, `airport-service-1`, `maintenance-service-2`, `auth-service-2`, `rabbitmq-3`, `redis-airport-1`, `redis-flight-1`, `redis-route-1`                                                                                |
| **PC5 (Martim)**  | `airport-service-2`, `route-service-2`, `flight-service-2`, `aircraft-service-2`, `rabbitmq-1`, `rabbitmq-2`, `redis-airport-2`, `redis-flight-2`, `redis-route-2`, `redis-airport-3`, `redis-flight-3`, `redis-route-3`, `postgresql-1` |

## Instructions
- **Start the cluster:** `docker compose up -d --build`
- **Access Eureka (Discovery):** http://localhost:8761 or http://localhost:8762
- **Access API (Gateway):** https://localhost:7979 (self-signed certificate; log in with `POST /api/auth/login` and send the token as `Authorization: Bearer <token>`)
- **Documentation:** [docs/README.md](docs/README.md) (global) and `docs/` inside each service; CI/CD details in [docs/ci-cd-pipelines.md](docs/ci-cd-pipelines.md)

> **Startup Note:** It is expected to see `Connection Refused` errors in the logs during the first 30-60 seconds. The microservices are configured with retry mechanisms and will automatically stabilize once the Eureka nodes finish booting and synchronize their registries. Wait until all services appear in the Eureka dashboard before issuing HTTP requests.

## CI/CD Architecture
This repository implements fully automated workflows via GitHub Actions:
- **CI (`ci.yml`):** Compiles all modules and executes exhaustive E2E integration tests in a headless environment.
- **CD (`cd.yml`):** Triggers strictly on CI success, builds individual Docker images via a matrix strategy, and pushes them to the GitHub Container Registry (GHCR).

*Note: The `docker-compose.yml` adopts a hybrid approach, capable of pulling from GHCR while falling back to local builds if registry credentials are not present.*

## Known Limitations
- **H2 Database Isolation:** E2E testing assumes 1 instance per service to overcome in-memory H2 database isolation. Eventual consistency across nodes is accepted for Assignment 1, meaning scaling replicas locally will partition the in-memory data context.

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
├── data/
│   ├── aircraft-service-1/
│   │   └── aircraft.db    
│   ├── aircraft-service-2/
│   ├── airport-service-1/
│   ├── airport-service-2/
│   ├── auth-service-1/
│   ├── auth-service-2/
│   ├── flight-service-1/
│   ├── flight-service-2/
│   ├── maintenance-service-1/
│   ├── maintenance-service-2/
│   ├── route-service-1/
│   └── route-service-2/
├── docs/
├── flight-service/
├── maintenance-service/
├── route-service/
├── .gitignore
├── docker-compose.yml
├── LICENSE
├── pom.xml
└── README.md
```

## Colaborators
- Lucas Santos
- Gustavo Abrantes
- Martim Ferreira
- Tiago Silva
