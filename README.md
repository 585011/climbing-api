# Climbing API

REST API for logging climbs at crags and climbing gyms: climbing areas, walls, routes, and user ticks.

## Clients

| Client | Platform | Repo |
|---|---|---|
| climbing-web | Web | [585011/climbing-web](https://github.com/585011/climbing-web) |
| climbing-mobile | iOS only (for now) | `585011/climbing-mobile` (private) |

Both clients log in through the same Auth0 tenant and call this API with a Bearer JWT. API contract changes need to be coordinated with both. CORS only matters for the web client, since the iOS app makes native requests.

## Domain docs

- [`CONTEXT.md`](CONTEXT.md): glossary of domain terms (Tick, Session, Follow, Kudos, …)
- [`docs/adr/`](docs/adr/): architecture decision records, including the planned following feed

## Tech stack

| Layer | Technology |
|---|---|
| Language | Kotlin (JVM 21) |
| Framework | Spring Boot 3.5 |
| Database | PostgreSQL 16 |
| Data access | Spring Data JDBC (`JdbcTemplate`, no ORM) |
| Migrations | Flyway |
| Auth | Auth0 (JWT / OAuth2 Resource Server) |
| Image storage | Cloudflare R2 (S3-compatible, AWS SDK v2) |
| API docs | OpenAPI 3 / Swagger UI (springdoc) |
| Build | Gradle |
| Tests | JUnit 5, Mockito, Testcontainers |

## Prerequisites

- JDK 21 (the Gradle toolchain downloads it automatically if missing)
- Docker and Docker Compose

## Quick start

```bash
# Create .env in the project root with the variables listed below

# Full-stack: start postgres + build + run API
docker-compose up -d

# Or hybrid: start only the DB, run the API locally
docker-compose up -d postgres
export $(grep -v '^#' .env | xargs)
./gradlew bootRun
```

The app runs at `http://localhost:8080`.
Swagger UI is available at `http://localhost:8080/swagger-ui.html`.
`GET /health` is an unauthenticated liveness probe.

## Environment variables

`docker-compose.yml` reads `.env` from the project root automatically. For local `./gradlew bootRun`, export the variables into your shell first.

| Variable | Description |
|---|---|
| `POSTGRES_DB` | Database name |
| `POSTGRES_USER` | Postgres superuser (used by the postgres container init) |
| `POSTGRES_PASSWORD` | Postgres superuser password |
| `SPRING_DATASOURCE_USERNAME` | Username Spring Boot uses to connect |
| `SPRING_DATASOURCE_PASSWORD` | Password Spring Boot uses to connect |
| `SPRING_DATASOURCE_URL` | JDBC URL (only needed for local `bootRun`; compose overrides it) |
| `AUTH0_ISSUER_URI` | Auth0 issuer URI: Auth0 dashboard → Applications → APIs → your API → Issuer (e.g. `https://YOUR_TENANT.auth0.com/`) |
| `AUTH0_AUDIENCE` | Auth0 API audience: Auth0 dashboard → Applications → APIs → your API → API Audience |
| `ALLOW_ORIGINS` | Optional: production frontend URL added to the CORS allowlist (localhost:5173 and localhost:8000 are always allowed) |
| `SPRING_PROFILES_ACTIVE` | Set to `prod` in production to suppress JDBC debug logging (`application-prod.yaml`) |
| `R2_ENDPOINT` | Cloudflare R2 S3 API endpoint (`https://<ACCOUNT_ID>.r2.cloudflarestorage.com`) |
| `R2_ACCESS_KEY_ID` | R2 API token access key (Cloudflare dashboard → R2 → Manage API Tokens) |
| `R2_SECRET_ACCESS_KEY` | R2 API token secret |
| `R2_BUCKET` | R2 bucket name for wall images and tick photos |

## Authentication and authorization

All `/api/**` endpoints require a valid JWT in the `Authorization: Bearer <token>` header (401 if missing).

- The clients log users in via Auth0 and attach the JWT to every API request.
- The API validates the JWT signature against Auth0's JWK set and checks the `iss` and `aud` claims.
- **Email allowlist**: an Auth0 Action currently limits who can log in. This is temporary and signup is expected to open to anyone, so the API does not rely on it for security.
- **Ownership**: tick and user-write endpoints require the caller's JWT `sub` to match the `auth0_id` on the user record (403 if not).
- **Admin role**: catalog writes (POST/PUT/DELETE on climbing areas, walls and routes) require the `admin` role, read from the `https://climbing-api/roles` JWT claim (set by an Auth0 post-login Action). The role check runs before validation, so a non-admin write returns 403, never 400. The one exception is `PUT /api/routes/{id}/retired`, which any authenticated user may call (see [Climbing gyms](#climbing-gyms)).

### Onboarding flow

1. User logs in on a client and receives a JWT from Auth0.
2. Client calls `POST /api/users/me` with `{"displayName": "..."}` and the JWT. This creates a user row (`auth0Id` and email are taken from the JWT claims).
3. Client calls `GET /api/users/me` to get the DB `id` for subsequent requests.

## Architecture

```
HTTP Request
    ↓
Controller   — REST handlers; delegates all logic to services and mappers
    ↓
Service      — business logic; throws NotFoundException when entities are missing
    ↓
Repository   — hand-written SQL via JdbcTemplate with inline RowMapper lambdas
    ↓
PostgreSQL
```

**Key design decisions:**
- No ORM: raw SQL with `RETURNING` clauses on INSERT/UPDATE to avoid a second query
- All 404s raised as `NotFoundException`, caught by `GlobalExceptionHandler` (`@RestControllerAdvice`)
- Mappers convert model → response DTO only (no reverse direction)
- Services trim `name` (and `grade` for routes) before persisting
- Multi-step service methods (validate → insert) are wrapped in `@Transactional`
- List endpoints return `PagedResponse<T>` (`data`, `page`, `pageSize`, `total`); `size` is capped at 100

## Domain model

```
climbing_areas (type: crag | gym)
    └── walls          (FK: area_id → climbing_areas.id, CASCADE)
            └── routes (FK: wall_id → walls.id, CASCADE)

users
    └── user_route_ticks (FK: user_id → users.id, route_id → routes.id, CASCADE; UNIQUE per pair)
```

Deleting a parent cascades to all children (area → walls → routes → ticks; user → ticks). Deleting an area, wall, route or user does not remove image objects from R2; only deleting a tick, a tick photo or a wall image does.

### Climbing gyms

A gym is a climbing area with `type = 'gym'`; its walls are admin-created sectors. Because gym routes rotate often:

- Any user can create a gym route inline while ticking (`newRoute` on the tick request); `created_by` records who.
- Any user can **retire** a gym route (`retired_at`). Routes are retired, never deleted on rotation, so ticks and history survive. Retired routes are hidden from route lists and area route counts but can still be fetched by id. Un-retiring, and retiring crag routes, is admin only.
- Gyms that grade by colour ranges store the colour in `routes.grade_band` (e.g. `Yellow` = 5+ to 6). It is set only by `newRoute` on a tick; `routes.grade` always holds the exact grade.

### Images

Images are stored in Cloudflare R2, not Postgres. Rows hold only the R2 object key; responses expose short-lived (15 min) presigned GET URLs. Uploads accept JPEG, PNG or WebP up to 5 MB.

- **Wall images** (admin only, keys under `walls/`): the original plus an optimized and a thumbnail variant (`image_key`, `optimized_key`, `thumbnail_key`), exposed as `WallResponse.imageUrl` and `thumbnailUrl`.
- **Tick photos** (gym ticks only, keys under `ticks/`): only a resized JPEG is kept (`user_route_ticks.photo_key`), exposed as `TickResponse.photoUrl`. Deleting a tick deletes its photo.

## API endpoints

| Method | Path | Description | Success |
|---|---|---|---|
| GET | `/health` | Liveness probe (no auth) | 200 |
| GET | `/api/climbing-areas` | List areas (`?page=0&size=20`) | 200 |
| GET | `/api/climbing-areas/{id}` | Get area by ID | 200 / 404 |
| GET | `/api/climbing-areas/{areaId}/walls` | List walls in an area | 200 / 404 |
| POST | `/api/climbing-areas` | Create area (admin) | 201 |
| PUT | `/api/climbing-areas/{id}` | Full replace (admin) | 200 / 404 |
| DELETE | `/api/climbing-areas/{id}` | Delete area (admin) | 204 / 404 |
| GET | `/api/walls` | List walls (`?page=0&size=20`) | 200 |
| GET | `/api/walls/{id}` | Get wall by ID | 200 / 404 |
| POST | `/api/walls` | Create wall (admin). JSON, or `multipart/form-data` with a JSON part `wall` + optional `image` file | 201 |
| PUT | `/api/walls/{id}` | Full replace (admin); does not touch the image | 200 / 404 |
| PUT | `/api/walls/{id}/image` | Upload/replace image (admin, multipart `image`) | 200 / 404 |
| DELETE | `/api/walls/{id}/image` | Remove image (admin); returns the wall | 200 / 404 |
| POST | `/api/walls/backfill-images` | Generate missing image variants (admin) | 200 |
| DELETE | `/api/walls/{id}` | Delete wall (admin) | 204 / 404 |
| GET | `/api/walls/{wallId}/routes` | List routes on a wall (`?includeRetired=true` to include retired) | 200 / 404 |
| GET | `/api/routes` | List routes (`?page=0&size=20`, `?includeRetired=true`) | 200 |
| GET | `/api/routes/{id}` | Get route by ID (also retired) | 200 / 404 |
| POST | `/api/routes` | Create route (admin); validates wall exists | 201 |
| PUT | `/api/routes/{id}` | Full replace (admin); leaves `grade_band` alone | 200 / 404 |
| PUT | `/api/routes/{id}/retired` | Body `{retired}`. Any user may retire a gym route; un-retire and crag routes are admin only | 200 / 403 / 404 |
| DELETE | `/api/routes/{id}` | Delete route (admin) | 204 / 404 |
| GET | `/api/users` | List users (`?page=0&size=20`) | 200 |
| GET | `/api/users/{id}` | Get user by ID | 200 / 404 |
| GET | `/api/users/me` | Get own profile | 200 / 404 |
| POST | `/api/users/me` | Self-register (body: `{displayName}`) | 201 / 409 |
| PUT | `/api/users/{id}` | Full replace (owner only) | 200 / 403 / 404 |
| DELETE | `/api/users/{id}` | Delete user (owner only) | 204 / 403 / 404 |
| GET | `/api/users/{userId}/ticks` | List own ticks (`?page=0&size=20`, `?areaType=crag\|gym`) | 200 / 403 / 404 |
| POST | `/api/users/{userId}/ticks` | Tick a route. Exactly one of `routeId` or `newRoute {wallId, grade, style, holdColor?, name?, gradeBand?}` (gym walls only) | 201 / 400 / 403 / 404 / 409 |
| GET | `/api/users/{userId}/ticks/{tickId}` | Get own tick | 200 / 403 / 404 |
| PUT | `/api/users/{userId}/ticks/{tickId}` | Update style/rating/personalNote | 200 / 403 / 404 |
| PUT | `/api/users/{userId}/ticks/{tickId}/photo` | Add/replace photo (multipart `image`; gym ticks only) | 200 / 400 / 403 / 404 |
| DELETE | `/api/users/{userId}/ticks/{tickId}/photo` | Remove photo, keep tick (200 also when there was none) | 200 / 403 / 404 |
| DELETE | `/api/users/{userId}/ticks/{tickId}` | Delete tick and its photo | 204 / 403 / 404 |

All error responses share the shape: `{ timestamp, status, error, errorCode, message, path }`.

## Database migrations

Flyway migrations live in `src/main/resources/db/migration/` and run automatically on startup.

| File | Purpose |
|---|---|
| `V1__create_tables.sql` | Full schema (all 5 tables) |
| `V2__schema_improvements.sql` | NOT NULL on FK cols, UNIQUE on email + ticks, CASCADE deletes, FK indexes, lat/lng precision |
| `V3__add_auth0_id.sql` | Add nullable `auth0_id VARCHAR(128)` + unique constraint + index to `users` |
| `V4__widen_description_columns.sql` | Widen `walls.description` and `walls.approach_info` to `TEXT` |
| `V5__add_wall_image_key.sql` | Add nullable `image_key` to `walls` (R2 object key for the wall image) |
| `V6__add_wall_image_variants.sql` | Add `optimized_key` and `thumbnail_key` to `walls` |
| `V7__climbing_gyms.sql` | `climbing_areas.type` (crag\|gym); `routes.hold_color`, `retired_at`, `created_by`; `routes.style` constrained to sport/trad/boulder/toprope/speed |
| `V8__add_tick_photo_key.sql` | Add nullable `photo_key` to `user_route_ticks` (R2 object key for a gym tick's photo) |
| `V9__add_route_grade_band.sql` | Add nullable `grade_band VARCHAR(30)` to `routes` (gym grade colour, e.g. `Yellow`) |

New migrations must follow the `V{n}__{description}.sql` naming convention.

## Running tests

```bash
# All tests (unit + integration)
./gradlew test

# Only integration tests
./gradlew test --tests "com.example.climbingapi.integration.*"
```

- **Unit tests**: all services, using Mockito (no Spring context).
- **Integration tests**: one `*ControllerIT` per controller, covering CRUD, 404, 400, 409 and cascade behaviour. They use Testcontainers to start a real PostgreSQL 16 container (once per JVM), and truncate all tables before each test. No external database or Auth0 credentials are needed; JWTs are mocked via `SecurityMockMvcRequestPostProcessors.jwt()`. CI pulls `postgres:16` from Docker Hub on the GitHub Actions runner.

## Building

```bash
./gradlew build
```

A `Dockerfile` and `docker-compose.yml` are included for containerised builds and local development.
