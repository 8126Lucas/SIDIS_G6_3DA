# SIDIS_G6_3DA (AISafeFMS)

Microservices architecture monorepo for the _Sistemas Distribuídos_ (SIDIS) course project.

## Service Ports
- Discovery Service (Eureka Server): 8761 - 8762
- API Gateway: 7979 - 7980
- Aircraft Service: 8081 - 8090
- Airport Service: 8091 - 8100
- Authentication Service: 8101 - 8110
- Flight Service: 8111 - 8120
- Maintenance Service: 8121 - 8130
- Route Service: 8131 - 8140

## Instructions
- **Start the cluster:** `docker compose up -d --build`
- **Access Eureka (Discovery):** http://localhost:8761 or http://localhost:8762
- **Access API (Gateway):** http://localhost:7979

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
├── aircraft-service
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