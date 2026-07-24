# UFS Architecture

## System Overview

UFS (Unified File Service) is a multi-tenant, multi-cloud file service that provides a unified API for file upload, download, and management across Tencent COS, Aliyun OSS, Qiniu Kodo, and Volcengine TOS.

```
Browser / Java App
    │                  Java SDK Client
    ├─ Vue 3 Console ─┐    │
    │   (:5173)       │    │ (direct HTTP)
    │                 ▼    ▼
    │           ┌──────────────┐
    │           │   ufs-core   │
    │           │   :8080      │
    │           │              │
    │           │ JWT Auth     │
    │           │ Buckets      │
    │           │ Objects      │
    │           │ Policies     │
    │           │ Audit        │
    │           │ Recycle      │
    │           └──┬──────┬────┘
    │              │      │
    │         ┌────▼┐ ┌──▼───┐
    │         │MySQL│ │Redis │
    │         │ 8.0 │ │  7   │
    │         └─────┘ └──────┘
    │              │
    │    ┌─────────┼─────────────┐
    │    ▼         ▼      ▼      ▼
    │  COS       OSS    Kodo    TOS
```

## Repository Map

| Repo | Type | Description |
|------|------|-------------|
| [yesingle10/ufs](https://github.com/yesingle10/ufs) | Backend | Spring Boot 3.2 core service |
| [yesingle10/ufs-client](https://github.com/yesingle10/ufs-client) | SDK | Java client library (OkHttp + JWT) |
| [yesingle10/ufs-console](https://github.com/yesingle10/ufs-console) | Frontend | Vue 3 management console |
| [yesingle10/ufs-sdk-example](https://github.com/yesingle10/ufs-sdk-example) | Example | SDK usage demo |

## Component Design

### ufs-core

- **Framework**: Spring Boot 3.2 / Java 17
- **Database**: MySQL 8.0 + Flyway migrations
- **Cache**: Redis 7 (JWT blacklist, temp tokens)
- **API**: REST over HTTP, JWT Bearer auth
- **Docs**: Springdoc OpenAPI 3.0 + Swagger UI

#### Key Modules

| Module | Responsibility |
|--------|---------------|
| `auth/` | JWT issue / validate / refresh |
| `bucket/` | Storage bucket CRUD |
| `object/` | Presign upload / download / multipart |
| `policy/` | BizType → Storage routing engine |
| `audit/` | Operation audit trail |
| `recycle/` | Soft-delete + restore + purge |
| `adaptor/` | COS / OSS / Kodo / TOS adapters |

### ufs-client SDK

- **Runtime**: Java 17+, OkHttp 4.12, Jackson 2.17
- **Auth**: Auto JWT issue + cache + transparent refresh
- **Test**: 23 tests with MockWebServer

#### API Surface

| API | Methods |
|-----|---------|
| `BucketApi` | list() / get(name) / create(...) / update(...) / delete(name) |
| `ObjectApi` | presignUpload() / presignMultipartUpload() / completeUpload() / list() / head() / presignDownload() / delete() |
| `PolicyApi` | listActive() / get(bizType) / create(...) / update(...) / delete(bizType) |
| `HealthApi` | check() |

### ufs-console

- **Framework**: Vue 3 Composition API / Vite 5
- **Styling**: Tailwind CSS 3
- **Charts**: Chart.js 4
- **Routing**: Vue Router 4

#### Views

| View | Function |
|------|----------|
| Dashboard | Storage overview + charts |
| Buckets | List / create / edit / delete |
| Objects | Presign upload / browse / delete |
| Policies | Active list + CRUD |
| Audit | Query by operator/action |
| Recycle | Restore / purge |

## Data Flow

1. **Auth**: `POST /api/v1/auth/token` → JWT with tenant claims
2. **Upload**: `POST /api/v1/objects/presign-upload` → presigned URL → client uploads directly to cloud
3. **Download**: `POST /api/v1/objects/presign-download` → temp download URL
4. **Policy routing**: `POST /api/v1/policies` → define bizType → vendor + bucket mapping

## Deployment

### Docker Compose (recommended)

```bash
git clone https://github.com/yesingle10/ufs
cd ufs-core
cp .env.example .env
docker compose up -d
# MySQL 8 + Redis 7 + ufs-core, all connected
```

### Local Dev

```bash
# Backend
cd ufs-core && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Frontend
cd ufs-console && npm run dev

# SDK example
cd ufs-sdk-example && mvn compile exec:java
```

## CI/CD

| Workflow | Repo | Trigger | Action |
|----------|------|---------|--------|
| Test | ufs-core | push/PR | Maven test (65 tests) |
| Docker Publish | ufs-core | push main | Build → push ghcr.io |
| Test | ufs-client | push/PR | Maven test (23 tests) |
| SDK Publish | ufs-client | tag v* | Deploy to GitHub Packages |
| Build | ufs-console | push/PR | npm ci + build |
