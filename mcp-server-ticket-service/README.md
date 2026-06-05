# Org Ticket Service

An MCP (Model Context Protocol) server for ticket management. It exposes ticket
operations as a REST API and an MCP prompt (`analyze-tickets`), backed by PostgreSQL.

## Tech Stack

| Concern            | Technology                              |
|--------------------|-----------------------------------------|
| Language           | Java 21                                 |
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
| `DB_URL`      | JDBC URL for PostgreSQL      | `jdbc:postgresql://localhost:5432/spring_ai`     |
| `DB_USERNAME` | Database username            | `postgres`                                       |
| `DB_PASSWORD` | Database password            | `postgres`                                       |
| `MCP_AUTH_TOKEN` | Shared bearer token; blank = insecure dev mode | *(empty)*                       |

## REST API

| Method | Path                    | Description                          |
|--------|-------------------------|--------------------------------------|
| POST   | `/tickets`              | Create a ticket (`title`, `description`, `priority`, `assignee`) |
| GET    | `/tickets`              | List all tickets                     |
| GET    | `/tickets/{id}`         | Get a ticket by id                   |
| PUT    | `/tickets/{id}/status`  | Update a ticket's status (`status`)  |
| PUT    | `/tickets/{id}/assign`  | Assign a ticket (`assignee`)         |

## Security & Operations

### Authentication

The service enforces a shared bearer token when the `MCP_AUTH_TOKEN` environment variable is set.
Authentication, acting-user propagation and rate limiting are applied by a single servlet
filter (`McpAuthFilter`) that covers both the REST API and the MCP endpoint.

**Request headers:**

| Header | Required | Purpose |
|---|---|---|
| `Authorization: Bearer <token>` | Yes (when token configured) | Authenticates the caller |
| `X-Acting-User` | Optional | Identifies the human/agent performing the action. Falls back to `mcp.security.default-user` |

**Error responses** (JSON `{status, error, message, timestamp}`):
- `401 Unauthorized` — missing or wrong bearer token
- `429 Too Many Requests` — rate limit exceeded

### Security Properties (`mcp.security.*`)

| Property | Default | Description |
|---|---|---|
| `mcp.security.token` | `${MCP_AUTH_TOKEN:}` | Shared bearer token; blank disables auth |
| `mcp.security.default-user` | `system` | Fallback acting-user name |
| `mcp.security.require-user-for-writes` | `false` | When `true`, write endpoints require an explicit `X-Acting-User` that is not the default |
| `mcp.security.rate-limit-per-minute` | `120` | Max requests per acting-user per minute |

### Rate Limiting

In-memory per-user sliding-window rate limiter. Exceeding the limit returns `429 Too Many Requests`.
`/actuator/health` and `/actuator/info` are always exempt from auth and rate limiting.

### Audit Logging

Every mutating endpoint (create / update-status / assign) emits a structured `AUDIT` INFO log
line with the acting user, sanitised arguments, outcome (SUCCESS / ERROR) and latency in
milliseconds. Reads emit a `TOOL` log line. The `analyze-tickets` prompt logs the acting user
and ticket count, and caps its ticket summary to protect the LLM context window.

Example:
```
INFO  AUDIT createTicket | user=jane priority=HIGH assignee=ops newId=42 outcome=SUCCESS latencyMs=23
```

## Best Practices Applied

- **Centralised error handling** — a `GlobalExceptionHandler` (`@RestControllerAdvice`)
  returns a consistent JSON envelope: `{status, error, message, details, timestamp}`.
- **Meaningful 404s** — `ResourceNotFoundException` is mapped to HTTP `404 Not Found`.
- **Input validation** — Jakarta Bean Validation constraints (`@NotBlank`, `@NotNull`,
  `@Positive`) on controller params; violations map to HTTP `400 Bad Request`.
- **Observability** — Spring Boot Actuator exposes `health`, `info`, `metrics`, and
  `prometheus` endpoints, with Micrometer/Prometheus metrics. The sensitive `loggers`
  and `env` endpoints are deliberately not exposed.
- **Security hardening** — bearer-token auth, acting-user propagation (`X-Acting-User`),
  per-user rate limiting, a write-gate option and audit logging (see *Security & Operations*).
- **Externalised configuration** — server port and datasource settings are overridable
  via environment variables with sensible local defaults.
- **Structured logging** — `logback-spring.xml` provides a consistent, application-tagged
  console log pattern.
