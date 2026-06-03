# Org Ticket Service

An MCP (Model Context Protocol) server for ticket management. It exposes ticket
operations both as a REST API and as MCP server tools, backed by PostgreSQL.

## Tech Stack

| Concern            | Technology                              |
|--------------------|-----------------------------------------|
| Language           | Java 17                                 |
| Framework          | Spring Boot 4.0.3                        |
| Web                | Spring MVC (`spring-boot-starter-webmvc`) |
| Persistence        | Spring Data JPA + PostgreSQL            |
| Migrations         | Flyway                                   |
| AI / MCP           | Spring AI MCP Server (WebMVC)           |
| Validation         | Jakarta Bean Validation                 |
| Observability      | Actuator + Micrometer + Prometheus      |

## Running

```bash
./mvnw spring-boot:run
```

The service starts on port `8081` by default.

## Environment Variables

| Variable      | Description                  | Default                                          |
|---------------|------------------------------|--------------------------------------------------|
| `SERVER_PORT` | HTTP port                    | `8081`                                           |
| `DB_URL`      | JDBC URL for PostgreSQL      | `jdbc:postgresql://localhost:5432/ticket_db`     |
| `DB_USERNAME` | Database username            | `postgres`                                       |
| `DB_PASSWORD` | Database password            | `postgres`                                       |

## REST API

| Method | Path                    | Description                          |
|--------|-------------------------|--------------------------------------|
| POST   | `/tickets`              | Create a ticket (`title`, `description`, `priority`, `assignee`) |
| GET    | `/tickets`              | List all tickets                     |
| GET    | `/tickets/{id}`         | Get a ticket by id                   |
| PUT    | `/tickets/{id}/status`  | Update a ticket's status (`status`)  |
| PUT    | `/tickets/{id}/assign`  | Assign a ticket (`assignee`)         |

## Best Practices Applied

- **Centralised error handling** — a `GlobalExceptionHandler` (`@RestControllerAdvice`)
  returns a consistent JSON envelope: `{status, error, message, details, timestamp}`.
- **Meaningful 404s** — `ResourceNotFoundException` is mapped to HTTP `404 Not Found`.
- **Input validation** — Jakarta Bean Validation constraints (`@NotBlank`, `@NotNull`,
  `@Positive`) on controller params; violations map to HTTP `400 Bad Request`.
- **Observability** — Spring Boot Actuator exposes `health`, `info`, `metrics`, and
  `prometheus` endpoints, with Micrometer/Prometheus metrics.
- **Externalised configuration** — server port and datasource settings are overridable
  via environment variables with sensible local defaults.
- **Structured logging** — `logback-spring.xml` provides a consistent, application-tagged
  console log pattern.
