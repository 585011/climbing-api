# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Full-stack (Dockerized) — builds and runs both postgres and the API
docker-compose up -d

# Hybrid (local dev) — starts only the database; run the API separately
docker-compose up -d postgres
./gradlew bootRun

# Build
./gradlew build

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.example.climbingapi.ClimbingProjectApplicationTests"
```

The app runs on `http://localhost:8080`. Swagger UI is available at `http://localhost:8080/swagger-ui.html`.

## Integration Tests

Integration tests use [Testcontainers](https://testcontainers.com/) to spin up a real PostgreSQL 16
instance. No external database or env vars are required.

```bash
# Run all tests (unit + integration)
./gradlew test

# Run only integration tests
./gradlew test --tests "com.example.climbingapi.integration.*"
```

**Structure:** `src/test/kotlin/.../integration/IntegrationTestBase.kt` is the abstract base class
that starts a shared Postgres container (once per JVM) and truncates all tables before each test.
One `*ControllerIT.kt` file per controller covers happy-path CRUD, 404, 400, 409, and cascade behaviour.

**Design decisions:**
- `companion object @Container` + `@ServiceConnection` (Spring Boot 3.1+) — single container per JVM,
  datasource wired automatically, no `@DynamicPropertySource` needed.
- Explicit `TRUNCATE … RESTART IDENTITY CASCADE` in `@BeforeEach` instead of `@Transactional` rollback —
  MockMvc dispatches through `DispatcherServlet` which opens its own transactions.
- Tests seed their own minimal fixtures; Flyway seed data (V2, V3) is wiped by the truncation.
- CI no longer has a `services.postgres` block — Testcontainers pulls `postgres:16` from Docker Hub
  on the GitHub Actions runner (Docker is pre-installed on `ubuntu-latest`).

## Environment

Copy `.env` values as environment variables before running. The app reads the following from the environment (see `application.yaml`):

| Variable | Where to find it |
|----------|-----------------|
| `POSTGRES_DB` | `.env` |
| `SPRING_DATASOURCE_USERNAME` | `.env` |
| `SPRING_DATASOURCE_PASSWORD` | `.env` |
| `AUTH0_ISSUER_URI` | Auth0 dashboard → Applications → APIs → your API → Settings (e.g. `https://YOUR_TENANT.auth0.com/`) |
| `AUTH0_AUDIENCE` | Auth0 dashboard → Applications → APIs → your API → API Audience |
| `ALLOW_ORIGINS` | Production frontend URL for CORS (optional; localhost:5173 and localhost:8000 are always allowed) |

```bash
export $(grep -v '^#' .env | xargs)
export AUTH0_ISSUER_URI=https://YOUR_TENANT.auth0.com/
export AUTH0_AUDIENCE=YOUR_API_IDENTIFIER
```

Requires **Java 21** (the Gradle toolchain will download it automatically if not present).

## Architecture

Kotlin + Spring Boot 3.5 REST API backed by **PostgreSQL 16**. Uses **Spring Data JDBC** (`JdbcTemplate`) with hand-written SQL — there is no JPA/ORM.

**Layer flow:** `Controller → Service → Repository`

- `controller/` — REST handlers. Delegates all logic to services and mappers; no business logic here.
- `service/` — Business logic. Throws `NotFoundException` when entities are missing.
- `repository/` — Raw SQL via `JdbcTemplate` with inline `RowMapper` lambdas. Uses `RETURNING` clauses on inserts to avoid a second query.
- `model/` — Plain Kotlin data classes (no annotations). `id` and `createdAt` are nullable since they are DB-assigned.
- `dto/` — Request/response classes. Requests use Bean Validation annotations (`@NotBlank`, `@Min`, `@Email`, etc.). Required fields are non-nullable; optional fields are `String?`/`Int?`. Response DTOs use non-nullable types for fields guaranteed by the DB schema.
- `mapper/` — Spring `@Component` classes that convert between `model` and `dto` types.
- `exception/` — `GlobalExceptionHandler` (`@RestControllerAdvice`) centralises error responses. All 404s come from `NotFoundException`.

## Implemented endpoints

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/climbing-areas` | List all areas; supports `?page=0&size=20` (max 100) |
| GET | `/api/walls` | List all walls; supports `?page=0&size=20` (max 100) |
| GET | `/api/walls/{id}` | 404 if not found |
| POST | `/api/walls` | Returns 201; `areaId` required |
| GET | `/api/walls/{wallId}/routes` | 404 if wall not found |
| PUT | `/api/walls/{id}` | Full replace; 404 if not found |
| GET | `/api/routes` | List all routes; supports `?page=0&size=20` (max 100) |
| GET | `/api/routes/{id}` | 404 if not found |
| POST | `/api/routes` | Returns 201; validates wall exists |
| PUT | `/api/routes/{id}` | Full replace; validates wall exists; 404 if not found |
| GET | `/api/users` | List all users; supports `?page=0&size=20` (max 100) |
| GET | `/api/users/{id}` | 404 if not found |
| GET | `/api/users/me` | Returns the authenticated user's own profile; 404 if not yet registered |
| POST | `/api/users/me` | Self-register: creates user from JWT claims (`sub` → auth0Id, `email` claim); body: `{displayName}`; returns 201 |
| PUT | `/api/users/{id}` | Full replace; 403 if not owner; 404 if not found |
| DELETE | `/api/users/{id}` | 204; 403 if not owner; 404 if not found |
| GET | `/api/users/{userId}/ticks` | List user's ticked routes; supports `?page=0&size=20`; 403 if not owner; 404 if user not found |
| POST | `/api/users/{userId}/ticks` | Returns 201; 403 if not owner; validates user + route exist |
| GET | `/api/users/{userId}/ticks/{tickId}` | 403 if not owner; 404 if user or tick not found |
| PUT | `/api/users/{userId}/ticks/{tickId}` | Update style/rating/personalNote; 403 if not owner |
| DELETE | `/api/users/{userId}/ticks/{tickId}` | 204; 403 if not owner; 404 if not found |

All error responses share the shape: `{ timestamp, status, error, errorCode, message, path }`.

## Domain model

```
climbing_areas
    └── walls (FK: area_id, ON DELETE CASCADE)
            └── routes (FK: wall_id, ON DELETE CASCADE)

users
    └── user_route_ticks (FK: user_id + route_id, ON DELETE CASCADE, UNIQUE per pair)
```

All five domain types have a complete Controller/Service/Repository stack.

## Conventions

- Services trim `name` (and `grade` for routes) before persisting.
- Mappers only implement `toResponse()` — there is no `toModel()` direction.
- Unit tests for all 5 services use Mockito (`@ExtendWith(MockitoExtension::class)`) — no Spring context needed.
- All REST endpoints are prefixed with `/api`.
- All `/api/**` endpoints require a valid Auth0 JWT (`Authorization: Bearer <token>`). Returns 401 if missing, 403 if the caller is not the resource owner.
- Tick and user-write endpoints enforce ownership: the JWT `sub` claim must match `auth0_id` on the user record.
- Multi-step service methods (e.g. validate → insert) are annotated `@Transactional`.
- List endpoints return `PagedResponse<T>` with `data`, `page`, `pageSize`, `total` fields. Size is clamped to max 100 in the service layer.

## Database migrations

Flyway migrations live in `src/main/resources/db/migration/`. New migrations must follow the `V{n}__{description}.sql` naming convention and are applied automatically on startup.

| File | Purpose |
|------|---------|
| `V1__create_tables.sql` | Full schema (all 5 tables) |
| `V2__schema_improvements.sql` | NOT NULL on FK cols, UNIQUE on email + ticks, CASCADE deletes, FK indexes, lat/lng precision |
| `V3__add_auth0_id.sql` | Add nullable `auth0_id VARCHAR(128)` + unique constraint + index to `users` table |

## Related projects

- **climbing-web** — companion frontend at https://github.com/585011/climbing-web.
  CORS is configured in `SecurityConfig.kt` for this frontend. API changes that affect the contract should be coordinated with that repo.

## Code guidelines
Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

### 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

### 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

### 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

### 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.
