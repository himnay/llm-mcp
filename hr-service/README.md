# Org HR Service

A Spring Boot **MCP server** managing employee leave and replacement lookups for Org. Exposes a REST API plus MCP tools consumed by the `ai-assistant-mcp` client.

## Tech Stack

| Layer        | Technology                          |
|--------------|-------------------------------------|
| Runtime      | Java 17, Spring Boot 3.5.11         |
| Web          | Spring MVC                          |
| Persistence  | Spring Data JPA + PostgreSQL        |
| Migrations   | Flyway                              |
| AI / MCP     | Spring AI MCP Server (Stateless)    |
| Validation   | Jakarta Bean Validation             |
| Observability| Spring Boot Actuator + Micrometer/Prometheus |

## Running

```bash
./mvnw spring-boot:run
```

| Variable      | Default                                   |
|---------------|-------------------------------------------|
| `SERVER_PORT` | `8084`                                    |
| `DB_URL`      | `jdbc:postgresql://localhost:5432/hr_db`  |
| `DB_USERNAME` | `postgres`                                |
| `DB_PASSWORD` | `postgres`                                |

## REST API

| Method | Path                        | Description                       |
|--------|-----------------------------|-----------------------------------|
| POST   | `/hr/leave`                 | Apply for leave                   |
| GET    | `/hr/leave/{username}`      | Check whether a user is on leave  |
| GET    | `/hr/replacement/{username}`| Find an available replacement     |

## Best Practices Applied

- **Centralised error handling** — `GlobalExceptionHandler` returns a consistent JSON envelope `{status, error, message, details, timestamp}`. Missing employees / no replacement raise `ResourceNotFoundException` → HTTP 404.
- **Input validation** — request parameters validated with Jakarta constraints (`@NotBlank`, `@NotNull`); violations → HTTP 400.
- **Observability** — Actuator endpoints (`health`, `info`, `metrics`, `prometheus`).
- **Externalised config** — datasource and port driven by environment variables with local defaults.
- **Structured logging** — `logback-spring.xml` with application-tagged console pattern.

## Health & Metrics

- `GET /actuator/health`
- `GET /actuator/prometheus`
