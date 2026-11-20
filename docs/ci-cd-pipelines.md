# AISafe Flight Management System — CI/CD Pipelines

The repository has two GitHub Actions workflows in `.github/workflows/`: **CI** (`ci.yml`) checks that the whole system builds and passes its tests, and **CD** (`cd.yml`) turns a successful CI run into Docker images published on the project's GitHub page (GitHub Container Registry).

---

## 1. CI — `SIDIS Monorepo CI` (`ci.yml`)

### 1.1. Triggers

* `push` to `main`.
* `pull_request` targeting `main`.
* A pull request with the label `documentation` skips `compile`, and therefore every job that depends on it.

### 1.2. Jobs

The workflow has two active jobs on `ubuntu-latest` with JDK 21 (Temurin, Maven cache). The `postman-tests` job is still in the file but commented out.

| Job | What it does |
| :--- | :--- |
| `compile` — Compilação Preliminar | `./mvnw clean package -DskipTests`. If any module does not compile, the workflow stops here (fail-fast) and the slower job never starts. |
| `java-tests` — Build e Testes Java E2E | Needs `compile`. Starts three service containers: `postgres:15-alpine` (user `sidis`, with a health check), `rabbitmq:3.12-management-alpine` (`5672`) and `redis:7-alpine` (`6379`). Creates the five databases with `psql`, builds, starts `discovery-service` (profile `node1`), waits 15 s, starts `APIgateway` and the six business services in the background (`-Xmx256m`, logs to `<service>.log`), waits 150 s for Eureka, then runs `./mvnw verify`. |

### 1.3. Why the services are started

Two kinds of tests run in `./mvnw verify`:

* **End-to-end HTTP tests** (`support/GatewayHttpTest` in each module): they log in through `https://localhost:7979/api/auth/login` and call the real gateway, which needs Eureka and the target services to be up.
* **Controller tests** with `MockMvc` in `aircraft-service` and `maintenance-service` (added in iteration 2): they run without the rest of the system and enable method security in their own test configuration, so they check the `@PreAuthorize` roles.

`discovery-service` has a self-contained test (it starts its own server on a random port).

Consequences:

* Running `./mvnw verify` locally fails unless the whole system is running first (start it with `docker compose up` or each service by hand).
* The base URL and login can be changed with `SIDIS_BASE_URL`, `SIDIS_BACKOFFICE_USERNAME` and `SIDIS_BACKOFFICE_PASSWORD`.
* The 150-second wait is fixed: if a service takes longer to register, the first tests can fail with `503` from the gateway.
* The service logs are written to files on the runner but are not uploaded, so they are not visible after a failure.

### 1.4. Gaps after iteration 2

PostgreSQL and RabbitMQ are now on the runner, but the end-to-end job still cannot pass:

* **Eureka variables.** The workflow does not set `EUREKA_URL_1` and `EUREKA_URL_2`. Started without them, a service logs `Invalid eureka server URI: ${EUREKA_URL_1}/`, never registers, and the gateway answers `503` for it (checked by starting `auth-service` without the variables).
* **Gateway.** `APIgateway` does not start with the current `application.properties` (old key on the `route-service` route, see `APIgateway/docs/routing-and-security.md`).
* **Redis.** The runner has one standalone Redis on `6379`, while the services use the cluster client and expect three nodes per service on different ports (`6379 - 6393`).
* **Maintenance startup.** On the runner's empty database, `maintenance-service` stops during `Bootstrap` (see `data-architecture-cqrs.md`, section 7).
* **SQLite path.** `auth-service` opens `/data/auth.db`, a folder the runner does not create.
* **Postman.** The Postman job is commented out, so neither collection runs in CI.

---

## 2. CD — `SIDIS Monorepo CD` (`cd.yml`)

### 2.1. Trigger

* `workflow_run` of `SIDIS Monorepo CI`, type `completed`; the jobs run only if the CI conclusion is `success`.

### 2.2. Jobs

| Job | What it does |
| :--- | :--- |
| `build-services` | Checks out the code, builds every module with `mvn clean package -DskipTests`, lowercases the repository owner name, and uploads all `**/target/*.jar` files as the `maven-artifacts` artifact (kept 1 day). |
| `push-images` | Matrix over the 8 modules (`fail-fast: false`): downloads the jars, logs in to `ghcr.io` with `GITHUB_TOKEN`, builds `<module>/Dockerfile`, and pushes `ghcr.io/<owner>/<module>:latest`. |

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
