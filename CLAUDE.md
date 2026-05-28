# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Start the PostgreSQL database (required before running the app)
docker-compose up -d

# Run the application
./gradlew bootRun

# Build
./gradlew build

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.example.climbingapi.ClimbingProjectApplicationTests"
```

The app runs on `http://localhost:8080`. Swagger UI is available at `http://localhost:8080/swagger-ui.html`.

## Environment

Copy `.env` values as environment variables before running. The app reads `POSTGRES_DB`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` from the environment (see `application.yaml`).

```bash
export $(grep -v '^#' .env | xargs)
```

## Architecture

Kotlin + Spring Boot 3.5 REST API backed by PostgreSQL. Uses **Spring Data JDBC** (`JdbcTemplate`) with hand-written SQL — there is no JPA/ORM.

**Layer flow:** `Controller → Service → Repository`

- `controller/` — REST handlers. Delegates all logic to services and mappers; no business logic here.
- `service/` — Business logic. Throws `NotFoundException` when entities are missing.
- `repository/` — Raw SQL via `JdbcTemplate` with inline `RowMapper` lambdas. Uses `RETURNING` clauses on inserts to avoid a second query.
- `model/` — Plain Kotlin data classes (no annotations). `id` and `createdAt` are nullable since they are DB-assigned.
- `dto/` — Request/response classes. Requests use Bean Validation annotations (`@NotBlank`, `@Min`, etc.).
- `mapper/` — Spring `@Component` classes that convert between `model` and `dto` types.
- `exception/` — `GlobalExceptionHandler` (`@RestControllerAdvice`) centralises error responses. All 404s come from `NotFoundException`.

## Implemented endpoints

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/walls` | List all walls |
| GET | `/api/walls/{id}` | 404 if not found |
| POST | `/api/walls` | Returns 201; `areaId` required |
| GET | `/api/walls/{wallId}/routes` | 404 if wall not found |
| PUT | `/api/walls/{id}` | Full replace; 404 if not found |
| GET | `/api/routes` | List all routes |
| GET | `/api/routes/{id}` | 404 if not found |
| POST | `/api/routes` | Returns 201; validates wall exists |
| PUT | `/api/routes/{id}` | Full replace; validates wall exists; 404 if not found |
| GET | `/api/users` | List all users |
| GET | `/api/users/{id}` | 404 if not found |
| POST | `/api/users` | Returns 201 |
| PUT | `/api/users/{id}` | Full replace; 404 if not found |
| DELETE | `/api/users/{id}` | 204; 409 if user has ticks |
| GET | `/api/users/{userId}/ticks` | List user's ticked routes; 404 if user not found |
| POST | `/api/users/{userId}/ticks` | Returns 201; validates user + route exist |
| GET | `/api/users/{userId}/ticks/{tickId}` | 404 if user or tick not found |
| PUT | `/api/users/{userId}/ticks/{tickId}` | Update style/rating/personalNote |
| DELETE | `/api/users/{userId}/ticks/{tickId}` | 204; 404 if not found |

All error responses share the shape: `{ timestamp, status, error, message, path }`.

## Domain model

```
climbing_areas
    └── walls (FK: area_id)
            └── routes (FK: wall_id)

users
    └── user_route_ticks (FK: user_id, route_id)
```

`ClimbingArea` has a controller/service/repository. `User` and `UserRoute` (ticks) are fully wired up. All five domain types now have a complete stack.

## Conventions

- Services trim `name` (and `grade` for routes) before persisting.
- Mappers only implement `toResponse()` — there is no `toModel()` direction.
- Unit tests for `UserService` and `TickService` use Mockito (`@ExtendWith(MockitoExtension::class)`) — no Spring context needed.

## Database migrations

Flyway migrations live in `src/main/resources/db/migration/`. New migrations must follow the `V{n}__{description}.sql` naming convention and are applied automatically on startup.
