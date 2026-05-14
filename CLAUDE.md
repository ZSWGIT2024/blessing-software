# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Architecture Overview

This is a **social networking platform** with art/media sharing, real-time chat (friend/group/world), and VIP membership. It has three parts:

| Component | Stack | Port |
|-----------|-------|------|
| Backend API | Spring Boot 3.2.7 + Java 17 + MyBatis + MySQL + Redis | 8080 |
| Frontend SPA | Vue 3 + Vite + Pinia + Vue Router + Element Plus | 5173 |
| Frontend Express server | Node Express + SQLite (user profiles, image submissions) | 3001 |

The frontend Vite dev server proxies `/api` → `localhost:8080` (stripping the `/api` prefix in rewrite). The Express server is a separate supplementary service for image/avatar handling with its own SQLite database.

## Commands

```bash
# Backend (from project root)
./mvnw spring-boot:run                    # Start backend (dev profile)
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=prod"
./mvnw test                                # Run all backend tests
./mvnw test -Dtest=ClassName               # Run a single test class
./mvnw clean package -DskipTests           # Build JAR

# Frontend (from frontend/)
npm run dev                                # Start Vite dev server (port 5173)
npm run build                              # Production build

# Frontend Express server (from frontend/server/)
node index.js                              # Start Express server (port 3001)

# Docker
docker compose up -d                       # Start MySQL + Redis + app
```

## Backend Architecture

### Package Layout (standard layered architecture)

```
com.itheima
├── controller/    # REST controllers (26 controllers)
├── service/       # Service interfaces → service/impl/ (30+ implementations)
├── mapper/        # MyBatis mapper interfaces (37 mappers)
├── pojo/          # Entity classes matching DB tables
├── dto/           # Data transfer objects (ChatMessageDTO, FriendRelationDTO, etc.)
├── vo/            # View objects (FollowListVO, MediaVO, etc.)
├── config/        # Spring config (SecurityConfig, WebConfig, RedisConfig, WebSocketConfig, etc.)
├── interceptors/  # LoginInterceptor — JWT token validation on all requests except /user/register, /user/login, /user/sendSMSCode
├── aspect/        # PermissionAspect — AOP around @RequirePermission annotation
├── annotation/    # Custom annotations (RequirePermission)
├── websocket/     # ChatWebSocketHandler + ChatWebSocketInterceptor
├── handler/       # JsonTypeHandler for MyBatis JSON column mapping
├── common/        # Constants (RedisConstants, UserConstant, CacheConstant)
├── exception/     # Custom exceptions (PermissionDeniedException, etc.)
├── task/          # Scheduled cleanup tasks (friend apply, group chat, sensitive words)
├── utils/         # Utility classes
└── validation/    # Custom validators
```

### Authentication Flow

Spring Security disables form login, HTTP Basic, CSRF, and logout (SecurityConfig.java). Authentication is handled by `LoginInterceptor` which validates JWT tokens on all requests except `/user/register`, `/user/login`, `/user/sendSMSCode`, `/user/refreshToken`. Tokens use the `com.auth0:java-jwt` library. Permission checks use AOP via `@RequirePermission` annotation processed by `PermissionAspect`.

### WebSocket Chat System

`ChatWebSocketHandler` (TextWebSocketHandler) manages real-time messaging with three chat types:
- **Friend chat** (`chatType: "friend"`)
- **World chat** (`chatType: "world"`) — public chat room
- **Group chat** (`chatType: "group"`)

User sessions tracked in `ConcurrentHashMap<Integer, WebSocketSession>`. Offline messages are persisted via `OfflineMessageService`. Rate limiting enforced through `RateLimitService` (Redis-based).

### Caching Strategy

Extensive Redis caching with well-defined keys in `RedisConstants.java`. Multiple cache service implementations in `service/impl/`:
- `SocialCacheService`, `GroupCacheService`, `WorldChatCacheService`, `MediaCacheService`, `FavoriteCacheService`, `EmojiCacheService`

Key pattern: `CacheService.evict()` must be called after writes. TTL configuration is centralized in `RedisConstants`.

### Key Features

- **VIP system** — Monthly/Quarterly/Yearly/Lifetime tiers with escalating daily upload limits
- **Media upload** — Supports images and video (500MB max), OSS or local storage configurable via `STORAGE_TYPE` env var
- **Sensitive word filter** — Uses `com.github.houbb:sensitive-word`, words loaded from `sensitive-words.txt`
- **Rate limiting** — Redis-based, limits on messages (30/min), friend applies (5/hr), follows (50/hr), file uploads (20/hr)
- **Daily upload limits** — Varies by VIP tier, tracked via `UserDailyUploadMapper`
- **Swagger** — springdoc-openapi at `/swagger-ui.html`

## Frontend Architecture

### Directory Structure

```
frontend/src/
├── api/          # Axios API client modules
├── stores/       # Pinia stores (userInfo.js, room.js, token.js)
├── router/       # Vue Router with auth guards (requiresAuth, requiresAdmin, guestOnly)
├── views/        # Page-level components (Home, Gallery, UserProfile, AIGallery, etc.)
├── components/   # Reusable components + admin components
│   └── admin/    # Admin panel components (Navbar, Dashboard, Review, UserManagement, etc.)
├── utils/        # WebSocket client, request interceptors, image compression, click-outside directive
└── assets/       # Static assets
```

### Routing & Auth

Vue Router with meta-based guards:
- `requiresAuth` — redirects to `/login` if not authenticated
- `requiresAdmin` — redirects to `/` if not admin
- `guestOnly` — redirects to `/` if already logged in

Pinia `userInfo` store manages auth state. Token store persists JWT to localStorage.

### State Management

Three Pinia stores:
- `userInfo.js` — User authentication, profile, admin status
- `room.js` — Chat room state
- `token.js` — JWT token persistence with `pinia-persistedstate-plugin`

## Database

MySQL database `blessing_software` with tables for users, social features (friends, follows, friend groups), chat (world messages, group messages, group members), media (user_media, media_likes, comments, favorites), emoji, VIP, and operational data (login records, operation logs). SQL init scripts in `src/main/resources/sql/`.

## Configuration

- `src/main/resources/application.yml` — Common config (file sizes, rate limits, cache TTLs, VIP prices)
- `src/main/resources/application-dev.yml` — Dev profile (DB creds, Redis, OSS)
- `src/main/resources/application-prod.yml` — Prod profile
- `.env` — Environment variables for Docker Compose (DB_PASSWORD, OSS keys, JWT secret)
- Sensitive config files (application-dev.yml, application-prod.yml) are gitignored


## Learn And Grow
- Update your CLAUDE.md and MEMORY.md after every conversation so you don't make that mistake again.
- remember my programming habits and coding style, then record them in this file（CLAUDE.md）, so I don't need tell you every time. For example, all code must be clearly marked with comments, annotations, and the purpose of fields and logical methods, etc.
