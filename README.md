# TwinTraffic

A **production-grade Shadow Traffic Mirroring System** built with Java Spring Boot, PostgreSQL, and Redis.

## What It Does

This platform acts as an API gateway/middleware that:
1. **Accepts** incoming requests
2. **Forwards** them synchronously to the primary service (v1) → returns v1 response to the client immediately
3. **Mirrors** the same request asynchronously to the shadow service (v2) — completely non-blocking
4. **Compares** responses (status code, JSON body, latency) between v1 and v2
5. **Logs** all data and mismatches to PostgreSQL
6. **Exposes** APIs for querying logs, metrics, and replaying requests

---

## Architecture

```
Client
  │
  ▼
POST /proxy  (shadow-platform :8080)
  │
  ├─── sync ──► v1-mock-service :8081  ──► response to client
  │
  └─── async (thread pool) ──► v2-mock-service :8082
                                     │
                                     ▼
                             ComparisonEngine
                                     │
                                     ▼
                              PostgreSQL DB
                         (requests, responses, comparisons)
                                     │
                                     ▼
                             Redis (metrics counters)
```

### Layers

| Layer | Classes |
|---|---|
| **Controller** | `ProxyController`, `RequestLogController`, `ReplayController`, `MetricsController` |
| **Service** | `ProxyService`, `ShadowService`, `ComparisonEngine`, `LoggingService`, `ReplayService`, `MetricsService`, `RequestLogService` |
| **Repository** | `RequestRepository`, `ResponseRepository`, `ComparisonRepository` |
| **Config** | `ShadowConfig`, `AsyncConfig`, `WebClientConfig`, `RedisConfig`, `OpenApiConfig` |

---

## Request Flow

```
1. Client → POST /proxy  {endpoint, method, payload, headers}
2. ProxyService.proxy()
   ├── LoggingService.saveRequest()   → persists to `requests` table
   ├── v1WebClient.call() [blocking]  → gets v1 response
   ├── LoggingService.saveResponse("v1")
   ├── ShadowConfig.shouldMirror()?   → checks enabled + sampling
   │   └── ShadowService.mirrorAsync() [@Async, thread pool]
   │       ├── v2WebClient.call() [timeout: 5s]
   │       ├── LoggingService.saveResponse("v2")
   │       └── ComparisonEngine.compare()
   │           └── persist comparison (MATCH/MISMATCH/ERROR/TIMEOUT)
   └── return ProxyResponseDTO (v1 response) to client
```

---

## Tech Stack

| Component | Technology |
|---|---|
| Backend | Java 21 + Spring Boot 3.2 |
| Database | PostgreSQL 15 (Flyway migrations) |
| Cache / Metrics | Redis 7 |
| HTTP Client | Spring WebClient (WebFlux) |
| Async | `@Async` + `ThreadPoolTaskExecutor` (10 core / 50 max) |
| Docs | SpringDoc OpenAPI 3 (Swagger UI) |
| Containerization | Docker + Docker Compose |

---

## API Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/proxy` | Forward request to v1, mirror to v2 |
| `GET` | `/requests?page=0&size=20` | List all logged requests (paginated) |
| `GET` | `/requests/{id}` | Get request detail: request + v1/v2 responses + comparison |
| `POST` | `/replay/{id}` | Replay a stored request through the full flow |
| `GET` | `/metrics` | Platform stats: totals, mismatch rate, avg latency diff |
| `GET` | `/actuator/health` | Spring Boot health check |
| `GET` | `/swagger-ui.html` | Interactive API documentation |

---

## Quick Start

### Prerequisites
- Docker + Docker Compose installed

### Run Everything

```bash
git clone <repo>
cd ShadowTool

# Build and start all 5 services
docker-compose up --build

# Or in background
docker-compose up --build -d
```

Services started:
- **shadow-platform** → http://localhost:8080
- **v1-mock-service** → http://localhost:8081
- **v2-mock-service** → http://localhost:8082
- **PostgreSQL** → localhost:5432
- **Redis** → localhost:6379

### Test It

```bash
# 1. Proxy a request
curl -X POST http://localhost:8080/proxy \
  -H "Content-Type: application/json" \
  -d '{
    "endpoint": "/api/data",
    "method": "POST",
    "payload": {"userId": 42, "action": "test"},
    "headers": {}
  }'

# 2. View all logged requests
curl http://localhost:8080/requests | jq

# 3. View detail (replace <uuid> from step 2)
curl http://localhost:8080/requests/<uuid> | jq

# 4. Replay a request
curl -X POST http://localhost:8080/replay/<uuid> | jq

# 5. Platform metrics
curl http://localhost:8080/metrics | jq

# 6. Swagger UI
open http://localhost:8080/swagger-ui.html
```

### Verify in DB

```bash
docker exec -it shadow-postgres psql -U shadow -d shadowdb -c \
  "SELECT match_status, count(*) FROM comparisons GROUP BY match_status;"
```

---

## Configuration

All settings are environment-variable driven:

| Variable | Default | Description |
|---|---|---|
| `SHADOW_ENABLED` | `true` | Toggle shadow mirroring on/off |
| `SHADOW_SAMPLING_RATE` | `1.0` | Fraction of requests to mirror (0.0–1.0) |
| `V1_BASE_URL` | `http://v1-mock-service:8081` | Primary service URL |
| `V2_BASE_URL` | `http://v2-mock-service:8082` | Shadow service URL |
| `V2_TIMEOUT_MS` | `5000` | v2 request timeout (ms) |
| `DB_URL` | `jdbc:postgresql://postgres:5432/shadowdb` | Database URL |
| `REDIS_HOST` | `redis` | Redis hostname |

---

## Mock Services

| Service | Port | Behavior |
|---|---|---|
| **v1-mock-service** | 8081 | Always returns `200 OK` with correct data |
| **v2-mock-service** | 8082 | 40% correct · 30% error (500/503) · 30% delayed (2–6s) |

---

## Database Schema

```sql
requests     (id UUID, endpoint, method, payload JSONB, headers JSONB, created_at)
responses    (id UUID, request_id FK, source [v1/v2], status_code, response_body JSONB, latency_ms, error_message)
comparisons  (id UUID, request_id FK, match_status [MATCH/MISMATCH/ERROR/TIMEOUT], latency_diff, v1_status_code, v2_status_code, diff_details JSONB)
```

---

## Design Decisions & Trade-offs

### `@Async` vs Kafka
- Chose `@Async` + `ThreadPoolTaskExecutor` for simplicity and zero extra infrastructure.
- For true production at massive scale, Kafka would provide durability, backpressure, and replay semantics.

### Fail-Safe Shadow
- v2 errors are fully isolated — caught in `ShadowService`, never propagated to the client.
- v1 response is always returned regardless of v2 outcome.

### JSON Diff
- Uses Jackson `JsonNode` for recursive field-level diff. Only differing fields are stored in `diff_details`.
- Dynamic fields (timestamps, request IDs) can be ignored by extending `ComparisonEngine`.

### Sampling
- `ShadowConfig.shouldMirror()` uses `Math.random() < samplingRate` — stateless per-request sampling.
- For consistent sampling (e.g., by user ID), replace with a hash-based approach.

### Idempotency
- All shadow requests include `X-Shadow-Request: true` header, allowing downstream services to opt out of side effects.

### Redis
- Used for fast atomic metric counters (`INCR`). Falls back gracefully if Redis is unavailable.

---

## Production Enhancements

- Add **Kafka** for durable async shadow queue
- Add **rate limiting** (Redis token bucket) on `/proxy`
- Add **authentication** (JWT) on management endpoints  
- Add **Prometheus + Grafana** dashboards via `/actuator/prometheus`
- Add **field exclusion list** for ignoring dynamic JSON fields in comparison
