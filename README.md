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

