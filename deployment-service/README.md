# Org Deployment Service

A Spring Boot **MCP server** that manages deployment scheduling for Org. Exposes both a REST API and MCP tools (`spring-ai-starter-mcp-server-webmvc`) consumed by the `ai-assistant-mcp` client.

## Tech Stack

| Layer        | Technology                          |
|--------------|-------------------------------------|
| Runtime      | Java 17, Spring Boot 4.0.3          |
| Web          | Spring MVC                          |
| Persistence  | Spring Data JPA + PostgreSQL        |
| Migrations   | Flyway                              |
| AI / MCP     | Spring AI MCP Server (Streamable)   |
| Validation   | Jakarta Bean Validation             |
| Observability| Spring Boot Actuator + Micrometer/Prometheus |

## Running

```bash
./mvnw spring-boot:run
```

Configuration is environment-overridable (defaults shown):

| Variable      | Default                                           |
|---------------|---------------------------------------------------|
| `SERVER_PORT` | `8082`                                            |
| `DB_URL`      | `jdbc:postgresql://localhost:5432/deployment_db`  |
| `DB_USERNAME` | `postgres`                                         |
| `DB_PASSWORD` | `postgres`                                         |

## REST API

| Method | Path                          | Description              |
|--------|-------------------------------|--------------------------|
| GET    | `/deployments`                | List all deployments     |
| GET    | `/deployments/{id}`           | Get a deployment         |
| POST   | `/deployments`                | Schedule a deployment    |
| PUT    | `/deployments/{id}/assign`    | Reassign owner           |
| PUT    | `/deployments/{id}/reschedule`| Reschedule               |
| PUT    | `/deployments/{id}/cancel`    | Cancel                   |

## Best Practices Applied

- **Centralised error handling** — `GlobalExceptionHandler` returns a consistent JSON envelope `{status, error, message, details, timestamp}`. Missing entities raise `ResourceNotFoundException` → HTTP 404 (no more leaked `RuntimeException` → 500).
- **Input validation** — request parameters validated with Jakarta constraints (`@NotBlank`, `@NotNull`, `@Positive`); violations → HTTP 400.
- **Observability** — Actuator endpoints (`health`, `info`, `metrics`, `prometheus`) exposed for scraping.
- **Externalised config** — datasource and port driven by environment variables with sane local defaults.
- **Structured logging** — `logback-spring.xml` with application-tagged console pattern.

## Health & Metrics

- `GET /actuator/health`
- `GET /actuator/prometheus`
