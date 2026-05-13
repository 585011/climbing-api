# Climbing API

REST API for managing climbing walls and routes.

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
- IntelliJ IDEA

## Quick start

1. Copy `.env.example` to `.env` and fill in your local values
2. Start the database: `docker-compose up -d`
3. Configure datasource env vars in your IntelliJ Run Configuration (see [Environment variables](#environment-variables) below)
4. Run `ClimbingProjectApplication` from IntelliJ

The app runs at `http://localhost:8080`.  
Swagger UI is available at `http://localhost:8080/swagger-ui.html`.

## Environment variables

### Spring Boot

The app reads its datasource configuration from environment variables. Set these in your IntelliJ **Run → Edit Configurations… → Environment variables**:

| Variable | Example value |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/app` |
| `SPRING_DATASOURCE_USERNAME` | `user` |
| `SPRING_DATASOURCE_PASSWORD` | `password` |

Spring Boot does **not** load `.env` automatically — the variables must be present in the process environment (i.e. set in the Run Configuration when running from IntelliJ).

### Docker Compose

`docker-compose.yml` reads a `.env` file from the project root to initialise the Postgres container. Copy `.env.example` to `.env` and set:

| Variable | Description |
|---|---|
| `POSTGRES_DB` | Database name |
| `POSTGRES_USER` | Postgres username |
| `POSTGRES_PASSWORD` | Postgres password |

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
- All 404s are raised as `NotFoundException` and caught by `GlobalExceptionHandler` (`@RestControllerAdvice`)
- Mappers convert model → response DTO only (no reverse direction)
- Services trim `name` (and `grade` for routes) before persisting

## Domain model

```
climbing_areas
    └── walls          (FK: area_id → climbing_areas.id)
            └── routes (FK: wall_id → walls.id)

users
    └── user_route_ticks (FK: user_id → users.id, route_id → routes.id)
```

`Wall` and `Route` are fully implemented. `ClimbingArea`, `User`, and `UserRoute` have models and DB tables but no controllers or services yet.

## API endpoints

| Method | Path | Description | Success |
|---|---|---|---|
| GET | `/api/walls` | List all walls | 200 |
| GET | `/api/walls/{id}` | Get wall by ID | 200 / 404 |
| POST | `/api/walls` | Create wall | 201 |
| PUT | `/api/walls/{id}` | Full replace | 200 / 404 |
| DELETE | `/api/walls/{id}` | Delete wall | 204 / 404 |
| GET | `/api/walls/{wallId}/routes` | List routes for a wall | 200 / 404 |
| GET | `/api/routes` | List all routes | 200 |
| GET | `/api/routes/{id}` | Get route by ID | 200 / 404 |
| POST | `/api/routes` | Create route | 201 |
| PUT | `/api/routes/{id}` | Full replace | 200 / 404 |
| DELETE | `/api/routes/{id}` | Delete route | 204 / 404 |

All error responses share the shape: `{ timestamp, status, error, message, path }`.

## Database migrations

Flyway migrations live in `src/main/resources/db/migration/` and run automatically on startup.

| File | Purpose |
|---|---|
| `V1__create_tables.sql` | Full schema (all 5 tables) |
| `V2__seed_test_data.sql` | Sample data: 1 wall, 3 routes in Bergen |

New migrations must follow the `V{n}__{description}.sql` naming convention.

## Running tests

```bash
./gradlew test
```

Currently only a Spring context-load smoke test exists.

## Building

```bash
./gradlew build
```

A `Dockerfile` is included for building a container image.
