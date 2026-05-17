# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A social media + chat platform (Blessing Software) with media sharing, real-time messaging, and community features. Monorepo with a Spring Boot 3.2 backend and Vue 3 frontend.

## Build & Run

```bash
# Backend (Spring Boot 3.2.7, Java 17, Maven)
mvn spring-boot:run                        # Start backend on :8080 (dev profile)
mvn spring-boot:run -Dspring-boot.run.profiles=prod
mvn test                                   # Run backend tests
./mvnw spring-boot:run                     # Use Maven wrapper if available

# Frontend (cd frontend/) — Vue 3 + Vite 6
npm run dev                                # Dev server on :5173, proxies /api → :8080
npm run build                              # Production build
```

The Vite dev server proxies `/api/*` → `http://localhost:8080` (strips `/api` prefix).

## Architecture

### Backend (`src/main/java/com/itheima/`)

Standard Spring Boot layered architecture:

| Layer | Package | Role |
|-------|---------|------|
| Controller | `controller/` | REST endpoints (28 controllers) |
| Service | `service/` + `service/impl/` | Business logic (~46 impls) |
| Mapper | `mapper/` | MyBatis data access (~39 mappers) |
| Entity | `pojo/` | Domain objects |
| DTO | `dto/` | Request/response objects |

**Authentication flow:** Custom JWT via `LoginInterceptor` (not Spring Security). Spring Security is configured to permit all — auth is done entirely in the interceptor. JWT tokens are issued by `AuthService`, validated in `LoginInterceptor` (checks blacklist, status, expiration), and user claims are stored in `ThreadLocalUtil`.

**Authorization:** AOP-based via `@RequirePermission` and `@RequireAdmin` annotations on controller methods, intercepted by `PermissionAspect` which delegates to `PermissionService`.

**Real-time communication:** WebSocket at `/ws/chat`. `ChatWebSocketHandler` handles message routing (private, group, world chat). Connection auth via `ChatWebSocketInterceptor` (validates JWT token from query params).

**File storage:** Aliyun OSS for production (configured in `application-dev.yml`). `OssUploadService` handles upload logic. Local storage also supported via `upload.storage.type` config.

**Key dependencies:** MyBatis with XML mappers (`src/main/resources/com/itheima/mapper/*.xml`), Redis (Lettuce), PageHelper for pagination, Hutool, Lombok, sensitive-word filtering, SpringDoc OpenAPI (Swagger at `/swagger-ui.html`).

### Frontend (`frontend/src/`)

Vue 3 Composition API + Pinia stores:

| Directory | Purpose |
|-----------|---------|
| `api/` | Axios modules per domain (user.js, socialApi.js, groupChatApi.js, etc.) |
| `stores/` | Pinia: `userInfo` (auth + social + WebSocket orchestration), `token` (JWT), `room` (group chat) |
| `utils/` | `requests.js` (axios instance with interceptor chain), `websocket.js` (WebSocket singleton) |
| `views/` | Page components |
| `components/` | Reusable components |

**HTTP client (`requests.js`):** All API calls use a shared axios instance. Request interceptor attaches `Bearer` token. Response interceptor handles code-based error toasts (`code === 0` = success) and automatic 401 token refresh (queues concurrent requests during refresh). `X-Refresh-Token` header triggers token renewal.

**WebSocket:** `WebSocketService` singleton connects to `ws://localhost:8080/ws/chat`. Supports message types: `message`, `typing`, `read`, `online/offline`, `group_message`, `group_event`, `group_read`, `world_message`, `ping/pong` heartbeat. Falls back to HTTP for message sending when WebSocket is disconnected.

### Docker

`docker-compose.yml` runs MySQL 9, Redis 7, and the Spring Boot app. `Dockerfile` uses `openjdk:17-jre-slim` with external config mount at `/app/config/`.

## Configuration

- `application.yml` — shared config (MyBatis, multipart, rate limits, VIP pricing, upload limits)
- `application-dev.yml` — local dev (MySQL connection, Redis, OSS creds, JWT secret, sensitive-word)
- `application-prod.yml` — production overrides
- `.env` at project root for Docker environment variables
- Sensitive words file: `src/main/resources/sensitive-words.txt`

## Response Style

Keep answers short and direct. No repetitive explanations, no restating the same point in different words. Answer the question and stop.

## API Conventions

All responses use `Result<T>` envelope: `{ code: 0, msg: "操作成功", data: T }`. Code 0 = success, 1 = failure. Auth endpoints excluded from login interceptor: `/user/register`, `/user/login`, `/user/loginByCode`, `/user/captcha`, `/user/sendSMSCode`, `/user/refreshToken`.
