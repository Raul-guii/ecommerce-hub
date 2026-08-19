# E-commerce Hub

A multi-tenant SaaS platform that synchronizes inventory and pricing between merchant catalogs and marketplaces (Mercado Livre, Amazon) — asynchronously, resiliently, and with proof it survives real load.

![CI](https://github.com/Raul-guii/ecommerce-hub/actions/workflows/ci.yml/badge.svg)

> This is a portfolio/demonstration project. It's built to prove specific distributed-systems concepts under real, observable conditions — not to be a one-command deploy. See ["Running it locally"](#running-it-locally) for the reduced-scope setup that actually applies.

---

## The problem

A merchant selling on their own storefront, Mercado Livre, and Amazon shares one inventory pool across all three channels. If one unit sells on Amazon, the other channels need to know within seconds — otherwise the same item gets sold twice ("oversell"). During peak events like Black Friday, a merchant may need to push 10,000 price updates at once without taking the system down.

E-commerce Hub solves this by decoupling the fast path (receiving updates) from the slow path (calling flaky external marketplace APIs), with full tenant isolation so one merchant's data is never visible to another — on the same database, same API, same cluster.

---

## Architecture

![Architecture diagram](docs/architecture.png)

A batch upload returns `202 Accepted` immediately — the caller never waits for marketplace calls to complete. Each `BatchItem` moves through `PENDING → SUCCESS | FAILED → DEAD_LETTER`, tracked independently. Both API and Worker run as Kubernetes Deployments; the Worker autoscales via KEDA based on queue depth, not CPU/memory.

---

## The proof

Talk is cheap — this section is the evidence, not the pitch.

### Autoscaling under real load (KEDA)

The Worker scales from 1 to 10 replicas in response to actual RabbitMQ queue depth — not CPU/memory, the default Kubernetes metric, which wouldn't reflect backlog on an I/O-bound consumer like this one.

![HPA scaling from 1 to 10 replicas](docs/images/keda-hpa-scaling.png)
*`kubectl get hpa -w` — replica count climbing as the metric crosses the threshold: 1 → 4 → 8 → 10.*

![Kubernetes scaling events](docs/images/keda-scaling-events.png)
*Native Kubernetes events confirming each step: `Scaled up replica set ... from 1 to 4`, `from 4 to 8`, `from 8 to 10`.*

![RabbitMQ queue under load](docs/images/rabbitmq-unacked-load.png)
*RabbitMQ management UI mid-test: 118 messages unacked, all 10 replicas consuming in parallel.*

### Tests run against real infrastructure, on every push

No mocked databases in integration tests — Testcontainers spins up real MySQL instances. The pipeline fails the build if any of it breaks.

![GitHub Actions CI passing](docs/images/ci-green.png)
*Both pipeline jobs green: test suite (Testcontainers-backed) and Docker image build/publish to GHCR.*

### Observability under load

![Grafana dashboard with live metrics](docs/images/grafana-dashboard.png)
*Prometheus + Grafana: sync throughput, worker pod count, and success/failure rate rendering real data during a load test.*

---

## Stack — and why

| Technology | Why |
|---|---|
| **Java 21 + Spring Boot 4.1** | LTS baseline, Virtual Threads, mature enterprise ecosystem |
| **MySQL 8** | ACID guarantees for inventory data — silent lost updates aren't acceptable here |
| **RabbitMQ** | Decouples the fast HTTP path from slow external calls; built-in DLQ support without extra setup (vs. Kafka, which fits event streaming/replay use cases this project doesn't need) |
| **Redis** | Caches marketplace credentials per tenant — without it, every queued message would hit MySQL for a token lookup |
| **Flyway** | Schema changes only happen through versioned, reviewable migrations — never auto-generated `ddl-auto` |
| **Resilience4j** | Circuit breaker isolated in its own bean (`MarketplaceSyncExecutor`) so retry/backoff logic stays testable independent of message consumption |
| **nimbus-jose-jwt** | Full control over accepted algorithms — explicitly rejects `alg: none` and unexpected algorithms, a common JWT attack vector |
| **Argon2id** | Memory-hard password hashing, resistant to GPU/ASIC attacks (chosen over bcrypt for that reason) |
| **Kubernetes + KEDA** | Worker autoscales on RabbitMQ queue depth via the RabbitMQ management HTTP API (not raw AMQP polling — more resilient to the kind of local-network flakiness a Docker Desktop bridge can produce) |
| **Prometheus + Grafana** | Metrics-based proof the system holds up under load, not just a claim |
| **GitHub Actions** | Every push is tested (Testcontainers-backed integration tests) before an image is published |

Multi-tenancy is enforced via a Hibernate filter activated per-request from the validated JWT — application code never has to remember to add `WHERE tenant_id = ?` manually.

---

## Running it locally

This covers local development only — API and Worker running outside Docker, talking to containerized infrastructure. It does **not** cover the Kubernetes/KEDA/Helm setup used to produce the autoscaling evidence above; that stack exists to demonstrate the concept, not to be spun up casually.

```bash
docker compose up -d          # MySQL, RabbitMQ, Redis, WireMock, Prometheus, Grafana
./mvnw clean install -DskipTests
./mvnw spring-boot:run -pl ecommerce-hub-api      # in one terminal
./mvnw spring-boot:run -pl ecommerce-hub-worker   # in another
```

API: `http://localhost:8080` · RabbitMQ management: `http://localhost:15672` (guest/guest) · Grafana: `http://localhost:3000`

Requires a `.env` file (see `.env.example`) with `JWT_SECRET`, `DISCORD_WEBHOOK_URL`, and database credentials.

---

## What this isn't

- Not real microservices — API and Worker share one database; this is a deliberate tradeoff for a solo portfolio project, not a production recommendation at scale
- Not connected to real Amazon/Mercado Livre APIs — WireMock simulates realistic marketplace failure patterns (state-based scenarios: healthy → degrading → recovered) so retry and circuit-breaker behavior is provable without vendor approval delays
- No frontend — this is an API-first, backend-focused project; Swagger and Grafana serve as the visible surface

---

## Recruiter pitch

"I built a multi-tenant e-commerce integration SaaS that synchronizes merchant inventory and pricing with major marketplaces like Mercado Livre and Amazon, asynchronously and in near real time. The platform is designed as a distributed, multi-tenant architecture, ensuring full data isolation per merchant and infrastructure elasticity under traffic spikes like Black Friday."