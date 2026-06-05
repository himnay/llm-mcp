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

## Security & Operations

### Authentication

Set the `MCP_AUTH_TOKEN` environment variable (shared secret) to enable auth enforcement.  When
the variable is absent or blank the service starts in **insecure dev mode** and logs a single
`WARN` at startup:

```
MCP auth token not configured – running in INSECURE dev mode
```

Every inbound request (except `/actuator/health` and `/actuator/info`) must carry:

| Header | Value |
|---|---|
| `Authorization` | `Bearer <token>` — must match `MCP_AUTH_TOKEN` |
| `X-Acting-User` | Identity of the human/service initiating the call (optional; falls back to `system`) |

### Configuration Properties (`mcp.security.*`)

| Property | Default | Description |
|---|---|---|
| `mcp.security.token` | `${MCP_AUTH_TOKEN:}` (blank) | Shared secret; blank = auth disabled |
| `mcp.security.default-user` | `system` | Fallback acting-user when header absent |
| `mcp.security.require-user-for-writes` | `false` | When `true`, write tools reject requests whose acting user is the default/fallback |
| `mcp.security.rate-limit-per-minute` | `120` | Fixed-window per-user request cap |
| `mcp.output.max-chars` | `8000` | Maximum characters in a single tool response (excess truncated with `…[truncated]`) |

### Rate Limiting

Requests are rate-limited per acting user using a fixed 60-second window.  Exceeding the limit
returns **HTTP 429** with a JSON error body.  Health and info endpoints are exempt.

### Actuator Endpoints

Only `health`, `info`, `metrics`, and `prometheus` are exposed.  `loggers` and `env` have been
removed to avoid leaking sensitive runtime information.  Liveness/readiness probes are enabled via
`management.endpoint.health.probes.enabled: true`.

### Audit Logging

Every MCP tool invocation logs at `INFO`:

```
[AUDIT] tool=applyLeave actingUser=alice username=bob date=2025-06-01 outcome=success latencyMs=12
```

Fields: `tool`, `actingUser`, `username`, `date`, `outcome` (`success` or `failure:<ExceptionType>`),
`latencyMs`.

---

## Best Practices Applied

- **Centralised error handling** — `GlobalExceptionHandler` returns a consistent JSON envelope `{status, error, message, details, timestamp}`. Missing employees / no replacement raise `ResourceNotFoundException` → HTTP 404.
- **Input validation** — request parameters validated with Jakarta constraints (`@NotBlank`, `@NotNull`); violations → HTTP 400.
- **Observability** — Actuator endpoints (`health`, `info`, `metrics`, `prometheus`).
- **Externalised config** — datasource and port driven by environment variables with local defaults.
- **Structured logging** — `logback-spring.xml` with application-tagged console pattern.

## Health & Metrics

- `GET /actuator/health`
- `GET /actuator/prometheus`
