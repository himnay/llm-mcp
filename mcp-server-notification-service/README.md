# Org Notification Service

An MCP (Model Context Protocol) server for sending and listing notifications across
channels (e.g. EMAIL, SMS, SLACK). It exposes both a REST API and MCP tools backed by a
PostgreSQL store.

## Tech Stack

| Concern            | Technology                          |
|--------------------|-------------------------------------|
| Language           | Java 17                             |
| Framework          | Spring Boot 4.0.3                   |
| Web                | Spring MVC                          |
| Persistence        | Spring Data JPA + PostgreSQL        |
| Migrations         | Flyway                              |
| MCP                | Spring AI MCP Server (WebMVC)       |
| Validation         | Jakarta Validation                  |
| Observability      | Actuator + Micrometer + Prometheus  |

## Running

Build and run from the module directory:

```bash
./mvnw spring-boot:run
```

Build a jar:

```bash
./mvnw clean package
```

A PostgreSQL instance must be reachable at the configured datasource URL. Flyway applies
the schema and seed data on startup.

## Configuration

All settings are externalised and can be overridden via environment variables:

| Variable      | Description               | Default                                            |
|---------------|---------------------------|----------------------------------------------------|
| `SERVER_PORT` | HTTP listen port          | `8083`                                             |
| `DB_URL`      | JDBC datasource URL       | `jdbc:postgresql://localhost:5432/notification_db` |
| `DB_USERNAME` | Datasource username       | `postgres`                                         |
| `DB_PASSWORD` | Datasource password       | `postgres`                                         |

## REST API

| Method | Path             | Description                                              |
|--------|------------------|---------------------------------------------------------|
| `POST` | `/notifications` | Send a notification (`channel`, `recipient`, `message`) |
| `GET`  | `/notifications` | List all notifications                                  |

## Best Practices Applied

- **Global exception handling** — `GlobalExceptionHandler` (`@RestControllerAdvice`)
  returns a consistent JSON envelope (`status`, `error`, `message`, `details`,
  `timestamp`) for all error responses.
- **Input validation** — Jakarta Validation constraints on request parameters; violations
  are mapped to HTTP 400 with field-level details.
- **Observability** — Actuator endpoints (`health`, `info`, `metrics`, `prometheus`)
  exposed for monitoring and Prometheus scraping via Micrometer.
- **Externalised configuration** — port and datasource settings are driven by environment
  variables with sensible defaults.
- **Structured logging** — `logback-spring.xml` provides a consistent, application-tagged
  console log pattern.
