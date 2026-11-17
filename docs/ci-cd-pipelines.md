# AISafe Flight Management System — CI/CD Pipelines

The repository has two GitHub Actions workflows in `.github/workflows/`: **CI** (`ci.yml`) checks that the whole system builds and passes its tests, and **CD** (`cd.yml`) turns a successful CI run into Docker images published on the project's GitHub page (GitHub Container Registry).

---

## 1. CI — `SIDIS Monorepo CI` (`ci.yml`)

### 1.1. Triggers

* `push` to `main`.
* `pull_request` targeting `main`.

### 1.2. Jobs

The workflow has three jobs on `ubuntu-latest` with JDK 21 (Temurin, Maven cache). `java-tests` and `postman-tests` wait for `compile` and then run in parallel.

| Job | What it does |
| :--- | :--- |
| `compile` — Compilação Preliminar | `./mvnw clean package -DskipTests`. If any module does not compile, the workflow stops here (fail-fast) and the slower jobs never start. |
| `java-tests` — Build e Testes Java E2E | Starts a `redis:7-alpine` service container on `6379`, builds, starts `discovery-service` (profile `node1`), waits 15 s, starts `APIgateway` and the six business services in the background (`-Xmx256m`, logs to `<service>.log`), waits 150 s for Eureka, then runs `./mvnw verify`. |
| `postman-tests` — Testes de Integração Postman | Same Redis container and same start-up as `java-tests`, then installs Newman (`npm install -g newman`) and runs `docs/postman/SIDIS HTTP Tests.postman_collection.json`. |

### 1.3. Why the services are started

Most tests are end-to-end HTTP tests (`support/GatewayHttpTest` in each module): they log in through `https://localhost:7979/api/auth/login` and call the real gateway, which needs Eureka and the target services to be up. Only `discovery-service` has a self-contained test (it starts its own server on a random port).

Consequences:

* Running `./mvnw verify` locally fails unless the whole system is running first (start it with `docker compose up` or each service by hand).
* The base URL and login can be changed with `SIDIS_BASE_URL`, `SIDIS_BACKOFFICE_USERNAME` and `SIDIS_BACKOFFICE_PASSWORD`.
* The 150-second wait is fixed: if a service takes longer to register, the first tests can fail with `503` from the gateway.
* The service logs are written to files on the runner but are not uploaded, so they are not visible after a failure.

### 1.4. Gaps after iteration 2

The runner still starts the system the way iteration 1 did, while the services now expect more infrastructure:

* **Infrastructure.** Only one standalone Redis is started. There is no PostgreSQL and no RabbitMQ, which `aircraft-service` and `maintenance-service` need, and that Redis is not a cluster, while both services use the cluster client.
* **Variables.** The workflow does not set `EUREKA_URL_1` and `EUREKA_URL_2`, which every `application.properties` now uses for the Eureka address.
* **Postman collection.** `postman-tests` runs the previous collection (`SIDIS HTTP Tests`), not `AISafe-Global-System`, and without an environment file.

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
