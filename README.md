# Climbing API

REST API for managing climbing walls, routes, and user ticks.

## Tech stack

| Layer | Technology |
|---|---|
| Language | Kotlin (JVM 21) |
| Framework | Spring Boot 3.5 |
| Database | PostgreSQL 16 |
| Data access | Spring Data JDBC (`JdbcTemplate`, no ORM) |
| Migrations | Flyway |
| API docs | OpenAPI 3 / Swagger UI (springdoc) |
| Build | Gradle |

## Prerequisites

- JDK 21
- Docker and Docker Compose

## Quick start

```bash
# Copy and fill in your local values
cp .env.example .env

# Full-stack: start postgres + build + run API
docker-compose up -d

# Or hybrid: start only the DB, run the API locally
docker-compose up -d postgres
export $(grep -v '^#' .env | xargs)
./gradlew bootRun
```

The app runs at `http://localhost:8080`.  
Swagger UI is available at `http://localhost:8080/swagger-ui.html`.

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

## Architecture

```
HTTP Request
    ↓
Controller   — REST handlers; delegates all logic to services
    ↓
Service      — business logic; throws NotFoundException when entities are missing
    ↓
Repository   — hand-written SQL via JdbcTemplate with inline RowMapper lambdas
    ↓
PostgreSQL
```

**Key design decisions:**
- No ORM — raw SQL with `RETURNING` clauses on INSERT/UPDATE to avoid a second query
- All 404s raised as `NotFoundException`, caught by `GlobalExceptionHandler` (`@RestControllerAdvice`)
- Mappers convert model → response DTO only (no reverse direction)
- Services trim `name` (and `grade` for routes) before persisting
- Multi-step service methods (validate → insert) are wrapped in `@Transactional`

## Domain model

```
climbing_areas
    └── walls          (FK: area_id → climbing_areas.id, CASCADE)
            └── routes (FK: wall_id → walls.id, CASCADE)

users
    └── user_route_ticks (FK: user_id → users.id, route_id → routes.id, CASCADE; UNIQUE per pair)
```

Deleting a parent cascades to all children (area → walls → routes → ticks; user → ticks).

## API endpoints

| Method | Path | Description | Success |
|---|---|---|---|
| GET | `/api/climbing-areas` | List all areas | 200 |
| GET | `/api/climbing-areas/{id}` | Get area by ID | 200 / 404 |
| POST | `/api/climbing-areas` | Create area | 201 |
| PUT | `/api/climbing-areas/{id}` | Full replace | 200 / 404 |
| GET | `/api/walls` | List all walls | 200 |
| GET | `/api/walls/{id}` | Get wall by ID | 200 / 404 |
| POST | `/api/walls` | Create wall | 201 |
| PUT | `/api/walls/{id}` | Full replace | 200 / 404 |
| GET | `/api/walls/{wallId}/routes` | List routes for a wall | 200 / 404 |
| GET | `/api/routes` | List all routes | 200 |
| GET | `/api/routes/{id}` | Get route by ID | 200 / 404 |
| POST | `/api/routes` | Create route | 201 |
| PUT | `/api/routes/{id}` | Full replace | 200 / 404 |
| GET | `/api/users` | List all users | 200 |
| GET | `/api/users/{id}` | Get user by ID | 200 / 404 |
| POST | `/api/users` | Create user | 201 |
| PUT | `/api/users/{id}` | Full replace | 200 / 404 |
| DELETE | `/api/users/{id}` | Delete user | 204 / 404 / 409 |
| GET | `/api/users/{userId}/ticks` | List user's ticks | 200 / 404 |
| POST | `/api/users/{userId}/ticks` | Tick a route | 201 / 404 / 409 |
| GET | `/api/users/{userId}/ticks/{tickId}` | Get tick | 200 / 404 |
| PUT | `/api/users/{userId}/ticks/{tickId}` | Update tick | 200 / 404 |
| DELETE | `/api/users/{userId}/ticks/{tickId}` | Delete tick | 204 / 404 |

All error responses share the shape: `{ timestamp, status, error, errorCode, message, path }`.

## Database migrations

Flyway migrations live in `src/main/resources/db/migration/` and run automatically on startup.

| File | Purpose |
|---|---|
| `V1__create_tables.sql` | Full schema (all 5 tables) |
| `V2__seed_test_data.sql` | Sample wall + 3 routes in Bergen |
| `V3__seed_climbing_areas.sql` | Sample climbing areas + more walls/routes |
| `V4__schema_improvements.sql` | NOT NULL on FK cols, UNIQUE constraints, CASCADE deletes, FK indexes, lat/lng precision |

New migrations must follow the `V{n}__{description}.sql` naming convention.

## Running tests

```bash
./gradlew test
```

Unit tests for `UserService` and `TickService` use Mockito (no Spring context). The context-load smoke test requires the database to be running.

## Building

```bash
./gradlew build
```

A `Dockerfile` and `docker-compose.yml` are included for containerised builds and local development.
