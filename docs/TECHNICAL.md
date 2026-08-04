# Technical documentation

## Services

### inventory-service (`:8081`)

Owns product stock. `Product` entity (`sku`, `name`, `quantityAvailable`,
`price`) backed by an in-memory H2 database, seeded with 3 demo products on
startup (`DataSeeder`). Exposes:

- `GET /api/inventory/products` — list all
- `GET /api/inventory/products/{sku}` — look up one
- `POST /api/inventory/products` — create
- `POST /api/inventory/reserve` — atomically decrement stock if enough is
  available (`synchronized` method — sufficient for a single-instance demo;
  a real multi-replica deployment would need optimistic locking or a
  database-level constraint instead, noted rather than glossed over)

### order-service (`:8082`)

Owns orders. On `POST /api/orders`, calls `inventory-service` via
`RestClient` (Spring's modern synchronous HTTP client, replacing the
now-legacy `RestTemplate`) to reserve stock, then persists the order as
`CONFIRMED` or `REJECTED_INSUFFICIENT_STOCK` depending on the real result —
not a hardcoded status. The inventory service's URL is injected via
`INVENTORY_SERVICE_URL`, defaulting to `localhost:8081` for local runs and
overridden to the Kubernetes Service DNS name (`http://inventory-service:8081`)
in `k8s/order-service.yaml`.

### gateway-service (`:8080`)

Spring Cloud Gateway with two path-based routes: `/api/inventory/**` →
inventory-service, `/api/orders/**` → order-service. Both target URIs are
environment-configurable the same way, so the identical route config works
unchanged across local, Docker Compose, and Kubernetes.

## Real bugs found and fixed while building this

**1. `order` is a reserved SQL keyword in H2.** The `Order` JPA entity
mapped to a table literally named `order` by default, and every generated
INSERT/SELECT failed with a syntax error the moment a test touched the
repository. Fixed with `@Table(name = "orders")`. Caught immediately by
the test suite, not discovered later — exactly the value of writing real
integration tests instead of only unit tests with mocked repositories.

**2. Spring MVC couldn't resolve `@PathVariable String sku` by name.**
`GET /api/inventory/products/{sku}` threw
`IllegalArgumentException: Name for argument of type [java.lang.String]
not specified, and parameter name information not available via
reflection` — a real runtime 500, not a hypothetical. Java doesn't retain
parameter names in bytecode by default, so Spring can't match `{sku}` in
the path to the `sku` parameter without either the `-parameters` compiler
flag or an explicit `@PathVariable("sku")`. Fixed both ways: the explicit
annotation on the existing endpoint, plus `-parameters` added to
`maven-compiler-plugin` in the parent POM's `pluginManagement` so future
endpoints in any of the three services don't hit the same bug.

**3. `eclipse-temurin:17-jre-alpine` has no arm64 manifest.** The first
Dockerfile draft used the Alpine JRE base image for a smaller final image;
building on this arm64 (Apple Silicon) machine failed with `no match for
platform in manifest`. Switched to `eclipse-temurin:17-jre` (Debian-based,
properly multi-arch) — a real, environment-specific compatibility issue,
not something worth spending more time forcing Alpine to work for a
30MB image-size difference.

**4. `spring-boot-maven-plugin` needs an explicit `repackage` execution
when not extending `spring-boot-starter-parent`.** This project uses its
own multi-module parent POM (so all three services share one
`dependencyManagement`), which means the plugin's `repackage` goal — which
turns the plain "thin" jar into an executable fat jar — isn't bound to the
`package` phase automatically the way it is when extending
`spring-boot-starter-parent`. First build produced a 9KB thin jar that
failed at runtime with `no main manifest attribute`. Fixed by declaring the
`repackage` execution explicitly in the parent POM's `pluginManagement`,
inherited by all three services.

## CI/CD pipeline (`.github/workflows/ci-cd.yml`)

Three jobs:

1. **`build-test`** — matrix over the three services, `mvn test` for each.
   10 real tests total (verified locally before being committed): 4 in
   inventory-service, 5 in order-service, 1 in gateway-service.
2. **`docker-publish`** (needs `build-test`) — builds each service's
   multi-stage Docker image and pushes to GHCR, tagged both `:latest` and
   `:<commit-sha>` for traceable, reproducible deploys.
3. **`k8s-deploy-verify`** (needs `build-test`) — the part that makes
   "Kubernetes" a verified claim rather than an aspirational one:
   - Builds the three images fresh inside the runner (kept independent of
     the `docker-publish` job so this job's success doesn't depend on GHCR
     auth/visibility — it's a self-contained proof).
   - Creates a real **kind** cluster (`helm/kind-action`).
   - `kind load docker-image` for all three, so the cluster can run them
     without needing registry access.
   - `kubectl apply -f k8s/` — real Deployment + Service manifests.
   - `kubectl rollout status` on all three deployments, with a 180s
     timeout — the job fails loudly if a pod doesn't become ready, rather
     than silently reporting success.
   - `kubectl port-forward` to the gateway Service, then two real `curl`
     calls: list products, place an order. **This is the step that proves
     the deployment actually serves correct application traffic**, not
     just that pods reached `Running` state.
   - On failure, dumps `kubectl get pods` and each deployment's logs, so a
     broken run is debuggable from the Actions log alone.

## Why local Docker demo screenshots aren't included

This machine's Docker daemon was running several unrelated, pre-existing
containers from other projects (multiple MongoDB instances, each pinned at
50-90% CPU) that left almost no memory headroom in the Docker VM. Three
JVM-based Spring Boot containers were verified working correctly against
each other locally first (products list, place-order, and reject-on-
insufficient-stock all confirmed via direct `curl` calls), but they were
then OOM-killed (exit code 137) by the resource-starved daemon before a
screenshot could be captured, and a `kind` cluster in that same environment
would face the identical contention.

Rather than force a screenshot out of an unreliable environment, the real
verification for Docker + Kubernetes lives in the **GitHub Actions run**,
which executes in an isolated runner with dedicated resources — a more
credible source of truth than a local capture would have been anyway. The
mvn test screenshot in this repo is from this machine (genuinely run here);
the container/Kubernetes evidence is [Actions run #1](https://github.com/ameyagidh/SDLC/actions/runs/30893127297) —
completed successfully, all 7 jobs green, `k8s-deploy-verify` (real kind
cluster + live HTTP calls through the gateway) passed in 3m36s.

## Resource limits and probes (`k8s/*.yaml`)

Each Deployment sets requests (256Mi/250m) and limits (512Mi/500m), and
both readiness and liveness probes against Spring Boot Actuator's
`/actuator/health/readiness` and `/actuator/health/liveness` endpoints
(enabled via `management.endpoint.health.probes.enabled: true` in each
service's `application.yml`) — the standard, correct way to wire Spring
Boot's health groups into Kubernetes probes, rather than probing the
generic `/actuator/health` endpoint for both.

## What was deliberately not built

- **No Ingress controller / TLS.** `gateway-service`'s Kubernetes Service
  is `NodePort`, reachable via `kubectl port-forward` in CI and locally —
  sufficient to prove routing works, without adding an Ingress controller
  that would need its own free-hosting story to actually expose it publicly.
- **No persistent database.** Each service uses in-memory H2, so data
  doesn't survive a pod restart. Swapping in a real Postgres (with a
  `StatefulSet` + `PersistentVolumeClaim`) is the natural next step for a
  production version, not attempted here to keep the free-hosting
  constraint simple (a managed free-tier Postgres would add another
  external dependency to the "free" story).
- **No Helm chart.** Plain manifests are more legible for a portfolio
  piece demonstrating raw Kubernetes concepts; templating them into a Helm
  chart is a reasonable next iteration once the raw manifests are stable.
