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

## Security & Operations

### Authentication

The service enforces a shared bearer token when the `MCP_AUTH_TOKEN` environment variable is set.

| Environment Variable | Property             | Default   | Notes                                                   |
|----------------------|----------------------|-----------|---------------------------------------------------------|
| `MCP_AUTH_TOKEN`     | `mcp.security.token` | *(empty)* | Empty = insecure dev mode — a WARN is logged at startup |

**Request headers:**

| Header                          | Required                    | Purpose                                                                                     |
|---------------------------------|-----------------------------|---------------------------------------------------------------------------------------------|
| `Authorization: Bearer <token>` | Yes (when token configured) | Authenticates the caller                                                                    |
| `X-Acting-User`                 | Optional                    | Identifies the human/agent performing the action. Falls back to `mcp.security.default-user` |

**Error responses** (JSON `{status, error, message, timestamp}`):
- `401 Unauthorized` — missing or wrong bearer token
- `429 Too Many Requests` — rate limit exceeded

### Security Properties (`mcp.security.*`)

| Property                               | Default              | Description                                                                          |
|----------------------------------------|----------------------|--------------------------------------------------------------------------------------|
| `mcp.security.token`                   | `${MCP_AUTH_TOKEN:}` | Shared bearer token; blank disables auth                                             |
| `mcp.security.default-user`            | `system`             | Fallback acting-user name                                                            |
| `mcp.security.require-user-for-writes` | `false`              | When `true`, write tools require an explicit `X-Acting-User` that is not the default |
| `mcp.security.rate-limit-per-minute`   | `120`                | Max requests per acting-user per minute                                              |

### Rate Limiting

In-memory per-user sliding-window rate limiter. Exceeding the limit returns `429 Too Many Requests`.  
`/actuator/health` and `/actuator/info` are always exempt from auth and rate limiting.

### Actuator Endpoints

| Endpoint                   | Purpose                                 |
|----------------------------|-----------------------------------------|
| `GET /actuator/health`     | Liveness + readiness (always permitted) |
| `GET /actuator/info`       | App metadata (always permitted)         |
| `GET /actuator/metrics`    | JVM/app metrics                         |
| `GET /actuator/prometheus` | Prometheus scrape endpoint              |

### Audit Logging

Every MCP tool call emits a structured INFO log line with:
- Tool name
- Acting user (`X-Acting-User` or default)
- Sanitised argument summary
- Outcome (SUCCESS / ERROR)
- Latency in milliseconds

Write/destructive tool calls are additionally tagged with `AUDIT` prefix.

Example:
```
INFO  AUDIT createDeployment | user=jane serviceName=payments environment=PROD scheduledTime=2025-06-01T14:00:00 owner=jane newId=42 outcome=SUCCESS latencyMs=23
```

## Best Practices Applied

- **Centralised error handling** — `GlobalExceptionHandler` returns a consistent JSON envelope `{status, error, message, details, timestamp}`. Missing entities raise `ResourceNotFoundException` → HTTP 404 (no more leaked `RuntimeException` → 500).
- **Input validation** — request parameters validated with Jakarta constraints (`@NotBlank`, `@NotNull`, `@Positive`); violations → HTTP 400.
- **Observability** — Actuator endpoints (`health`, `info`, `metrics`, `prometheus`) exposed for scraping.
- **Externalised config** — datasource and port driven by environment variables with sane local defaults.
- **Structured logging** — `logback-spring.xml` with application-tagged console pattern.

## Health & Metrics

- `GET /actuator/health`
- `GET /actuator/prometheus`
