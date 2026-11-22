# AISafe Flight Management System — CI/CD Pipelines

The repository has two GitHub Actions workflows in `.github/workflows/`: **CI** (`ci.yml`) checks that the whole system builds and passes its tests, and **CD** (`cd.yml`) turns a successful CI run into Docker images published on the project's GitHub page (GitHub Container Registry).

---

## 1. CI — `SIDIS Monorepo CI` (`ci.yml`)

### 1.1. Triggers

* `push` to `main` or to branch `1241008`.
* `pull_request` targeting `main`.
* A pull request with the label `documentation` skips `compile`, and therefore every job that depends on it.

### 1.2. Jobs

The workflow has three jobs on `ubuntu-latest` with JDK 21 (Temurin, Maven cache).

| Job | What it does |
| :--- | :--- |
| `compile` — Compilação Preliminar | `./mvnw clean package -DskipTests`. If any module does not compile, the workflow stops here (fail-fast) and the slower jobs never start. |
| `java-tests` — Build e Testes Java E2E | Needs `compile`. Runs the system on the runner and then `./mvnw verify` (see 1.3). Prints the last 100 lines of every service log if a step fails. |
| `postman-tests` — Testes de Integração Postman | Needs `compile`, runs in parallel with `java-tests`. Same service containers and databases; builds the jars and starts them with `java -Xmx256m -Dspring.profiles.active=test -jar ...` (discovery with `test,node1`), waits 120 s, installs Newman and runs `newman run "./docs/postman/SIDIS HTTP Tests.postman_collection.json" --insecure`. The global collection (`AISafe-Global-System`) is not run in CI. |

Both test jobs start the same service containers:

| Container | Host port | Used by |
| :--- | :--- | :--- |
| `postgres:15-alpine` (user `sidis`, health check) | `5432` | All five business services; the job creates the five databases with `psql` |
| `rabbitmq:3.12-management-alpine` | `5672` (and `15672` in `java-tests`) | All five business services |
| `redis:7-alpine` × 5 (`redis-aircraft`, `-maintenance`, `-airport`, `-flight`, `-route`, with health checks) | `6379`, `6382`, `6385`, `6388`, `6391` | One standalone Redis per service |

### 1.3. The `test` profile

Every module started in CI uses the Spring profile `test` (`application-test.properties`), which replaces the Docker addresses with `localhost`:

| Module | What the `test` profile sets |
| :--- | :--- |
| `discovery-service` | Has no `test` file; it runs with `node1` (`spring-boot:run -Dspring-boot.run.profiles=node1` in `java-tests`, `test,node1` in `postman-tests`). |
| `APIgateway` | Eureka on `http://localhost:8761/eureka/` and the six routes again. |
| `auth-service` | SQLite file `auth.db` in the working directory (instead of `/data/auth.db`) and Eureka on `localhost`. |
| Business services | PostgreSQL `localhost:5432/<service>_db`, Eureka on `localhost`, one RabbitMQ on `localhost:5672`, and a standalone Redis on the service's port (above). |

The profile also changes behaviour: `aircraft-service` and `maintenance-service` accept requests without `X-Gateway-Secret` when `test` is active (see `global-network-and-ports.md`, section 7.4).

In `java-tests` each module is started with `SPRING_PROFILES_ACTIVE=test ./mvnw spring-boot:run` from its folder, so the job compiles each module again instead of running the jars from `compile`.

### 1.4. Why the services are started

Two kinds of tests run in `./mvnw verify`:

* **End-to-end HTTP tests** (`support/GatewayHttpTest` in each module): they log in through `https://localhost:7979/api/auth/login` and call the real gateway, which needs Eureka and the target services to be up. The default login is `backoffice`, except in `APIgateway`, whose tests now log in as `atcc` by default.
* **Controller tests** with `MockMvc` (added in iteration 2; `spring-security-test` is now a test dependency of `airport-service`, `flight-service` and `route-service` too): they run without the rest of the system and enable method security in their own test configuration, so they check the `@PreAuthorize` roles even in services that do not enforce them at runtime.

`discovery-service` has a self-contained test (it starts its own server on a random port).

Consequences:

* Running `./mvnw verify` locally fails unless the whole system is running first (start it with `docker compose up` or each service by hand).
* The base URL and login can be changed with `SIDIS_BASE_URL`, `SIDIS_BACKOFFICE_USERNAME` and `SIDIS_BACKOFFICE_PASSWORD`.
* Locally the services need the `test` profile (or the Docker variables) and the same containers as CI.
* The 120-second wait is fixed: if a service takes longer to register, the first tests can fail with `503` from the gateway.
* The service logs are not uploaded as artifacts; `java-tests` prints their last 100 lines on failure, `postman-tests` does not.

### 1.5. Local run and remaining points

The system was started locally the same way as `postman-tests` (same containers, jars with `-Dspring.profiles.active=test`): every module registered in Eureka and `SIDIS HTTP Tests` passed (65 requests, 42 assertions, 0 failures). The gateway needs up to 30 seconds after the services register before it routes to them (`503` until its Eureka cache refreshes); the fixed 120-second wait covers this.

Remaining points:

* **Test-only bypass in the code.** The gateway secret check is skipped in `aircraft-service` and `maintenance-service` whenever the `test` profile is active, so CI does not check that filter in those two services.
* **Branch trigger.** The `push` trigger includes a personal branch (`1241008`).

---

## 2. CD — `SIDIS Monorepo CD` (`cd.yml`)

### 2.1. Trigger

* `workflow_run` of `SIDIS Monorepo CI`, type `completed`; the jobs run only if the CI conclusion is `success`.

### 2.2. Jobs

| Job | What it does |
| :--- | :--- |
| `build-services` | Checks out the code, builds every module with the Maven wrapper (`chmod +x mvnw && ./mvnw clean package -DskipTests`), lowercases the repository owner name, and uploads all `**/target/*.jar` files as the `maven-artifacts` artifact (kept 1 day). |
| `push-images` | Matrix over the 8 modules (`fail-fast: false`): downloads the jars, logs in to `ghcr.io` with `GITHUB_TOKEN`, builds `<module>/Dockerfile`, and pushes `ghcr.io/<owner>/<module>:latest`. |

The workflow declares `permissions: contents: read, packages: write`, which `GITHUB_TOKEN` needs to push to `ghcr.io`.

Every `Dockerfile` uses `eclipse-temurin:21-jre-alpine`, copies `target/*.jar` and runs it with `-Xmx256m`.

### 2.3. Published images

Images appear under **Packages** on the repository owner's GitHub page:

* `ghcr.io/<owner>/aircraft-service:latest`, `airport-service`, `auth-service`, `discovery-service`, `flight-service`, `maintenance-service`, `route-service`.

`docker-compose.yml` uses the same names (with `REPO_OWNER`) and a `build:` section, so `docker compose up` can pull the published images or build them locally.

### 2.4. Known limitations

* **Gateway image name.** The matrix entry is `APIgateway`, so the tag is `ghcr.io/<owner>/APIgateway:latest`. Docker image names must be lowercase, so this build is expected to fail; `docker-compose.yml` also expects `api-gateway`, not `APIgateway`.
* **Code that is built.** For `workflow_run` events, `actions/checkout` checks out the latest commit of the default branch (`main`), not the commit that the CI tested. A successful CI on a pull request or on branch `1241008` therefore republishes the current `main`.
* **Single tag.** Only `latest` is pushed, so previous images are overwritten and there is no version history.
* **Tests are not repeated.** CD builds with `-DskipTests` and relies on the CI result.

---

## 3. Using the Images in a Presentation

```bash
export REPO_OWNER=<github-owner-in-lowercase>
docker compose pull --ignore-pull-failures   # images that exist in GHCR
./mvnw clean package -DskipTests             # only needed for images that must be built locally
docker compose --profile pc<n> up -d         # on each PC, with IP_PC1..5 and EUREKA_URL_1/2 set
```

Images that were not pulled are built from the local `Dockerfile`s, which copy `target/*.jar`, so the Maven build must run first. This is currently the case for the gateway (see section 2.4).

The system is then available through HAProxy at `https://192.168.137.1`, the Eureka dashboards at `http://<IP_PC1>:8761` and `http://<IP_PC3>:8762`, and the HAProxy dashboard at `http://192.168.137.1:8404/stats`. Pulling from GHCR may require `docker login ghcr.io` if the packages are private.
