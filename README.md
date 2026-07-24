# UFS — Unified File Service

[![CI](https://github.com/yesingle10/ufs/actions/workflows/ci.yml/badge.svg)](https://github.com/yesingle10/ufs/actions/workflows/ci.yml)
[![CodeQL](https://github.com/yesingle10/ufs/actions/workflows/codeql.yml/badge.svg)](https://github.com/yesingle10/ufs/actions/workflows/codeql.yml)
[![Release](https://img.shields.io/github/v/release/yesingle10/ufs?color=blue)](https://github.com/yesingle10/ufs/releases)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

Multi-tenant, multi-cloud unified file service. Abstracts COS / OSS / Kodo / TOS behind a single REST API with built-in presigned upload/download, policy routing, audit trail, and recycle bin.

## Repositories

| Repo | Stack | Coverage | CI |
|------|-------|----------|----|
| **[ufs-core](https://github.com/yesingle10/ufs)** | Spring Boot 3.2 / Java 17 / MyBatis | 65 tests | [![CI](https://github.com/yesingle10/ufs/actions/workflows/ci.yml/badge.svg)](https://github.com/yesingle10/ufs/actions/workflows/ci.yml) |
| **[ufs-client](https://github.com/yesingle10/ufs-client)** | OkHttp 4.12 / Jackson / JWT | 23 tests | [![CI](https://github.com/yesingle10/ufs-client/actions/workflows/ci.yml/badge.svg)](https://github.com/yesingle10/ufs-client/actions/workflows/ci.yml) |
| **[ufs-console](https://github.com/yesingle10/ufs-console)** | Vue 3 / Vite 5 / Tailwind 3 | Build ✅ | [![CI](https://github.com/yesingle10/ufs-console/actions/workflows/ci.yml/badge.svg)](https://github.com/yesingle10/ufs-console/actions/workflows/ci.yml) |
| **[ufs-sdk-example](https://github.com/yesingle10/ufs-sdk-example)** | Maven exec demo | — | — |

## Quick Start

```bash
git clone https://github.com/yesingle10/ufs.git
cd ufs-core
cp .env.example .env
docker compose up -d
```

Wait 30s, then: `curl http://localhost:8080/api/v1/health` → `{"status":"UP"}`

**Swagger UI**: http://localhost:8080/swagger-ui.html

## Architecture

See **[ARCHITECTURE.md](ARCHITECTURE.md)** for full details.

```
Browser / Java App → Vue 3 Console (:5173) → ufs-core (:8080) → MySQL 8 + Redis 7
                                                    ↓
                                    COS / OSS / Kodo / TOS
```

## Core Capabilities

| Module | Description |
|--------|-------------|
| **Auth** | HMAC-SHA256 (server-to-server) + JWT Bearer (web console), both with replay protection |
| **Buckets** | Multi-cloud bucket CRUD with ACL (private / public-read) |
| **Objects** | Presigned upload (simple + multipart), presigned download, list with cursor pagination, head, delete |
| **Policies** | BizType → vendor + bucket routing engine, TTL-based lifecycle |
| **Audit** | MySQL-backed operation trail (RocketMQ ready for prod) |
| **Recycle** | Soft-delete with restore and permanent purge |
| **Monitor** | `/actuator/health` + Prometheus metrics + dashboard API |
| **Tenants** | ThreadLocal context holder + MyBatis SQL interceptor for row-level isolation |

## SDK Usage

```java
UfsClient client = new UfsClient(new UfsClientConfig()
    .setBaseUrl("http://localhost:8080")
    .setAppId("app_demo")
    .setAppSecret("secret_demo")
    .setTenantId("t_demo"));

// Buckets
client.bucket().list().forEach(b -> System.out.println(b.getName()));

// Presigned upload
PresignResult presign = client.object()
    .presignUpload("my-bucket", "photo.jpg", "photo.jpg", "image/jpeg", 1024);

// Health
Map<String, Object> status = client.health().check();
```

Full SDK example: [ufs-sdk-example](https://github.com/yesingle10/ufs-sdk-example)

## API Quick Reference

| Endpoint | Method | Auth |
|----------|--------|------|
| `/api/v1/health` | GET | None |
| `/api/v1/buckets` | GET / POST | JWT |
| `/api/v1/buckets/{name}` | GET / PUT / DELETE | JWT |
| `/api/v1/policies/active` | GET | JWT |
| `/api/v1/policies` | POST | JWT |
| `/api/v1/policies/{bizType}` | GET / PUT / DELETE | JWT |
| `/api/v1/objects/presign-upload` | POST | JWT |
| `/api/v1/objects/presign-download` | POST | JWT |
| `/api/v1/objects/presign-multipart` | POST | JWT |
| `/api/v1/objects/complete-upload` | POST | JWT |
| `/api/v1/objects` | GET | JWT |
| `/api/v1/objects/head` | GET | JWT |
| `/api/v1/objects` | DELETE | JWT |
| `/api/v1/monitor/stats` | GET | JWT |
| `/api/v1/monitor/audit` | GET | JWT |
| `/api/v1/monitor/recycle` | GET / POST | JWT |

Full interactive docs: `/swagger-ui.html`

## Deployment

- **Docker Compose**: `docker compose up -d` (MySQL 8 + Redis 7 + ufs-core)
- **CI/CD**: GitHub Actions test + JaCoCo coverage on push/PR, Docker publish on main, CodeQL security scan weekly
- **Monitoring**: Prometheus + Grafana dashboard (see [monitoring/](monitoring/))
- **Kubernetes**: Helm chart (see [helm/ufs/](helm/ufs/))

## Docs

- [ARCHITECTURE.md](ARCHITECTURE.md) — Architecture design, component map, data flow
- [INTEGRATION.md](INTEGRATION.md) — End-to-end verification guide, Docker quick start
- [Swagger UI](http://localhost:8080/swagger-ui.html) — Interactive API documentation (requires running instance)
