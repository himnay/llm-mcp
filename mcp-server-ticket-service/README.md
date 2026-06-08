# Ticket Service — `mcp-server-ticket-service`

An MCP server that manages support tickets, backed by PostgreSQL (Flyway-migrated). Runs on **`:8081`**, MCP
protocol **STATELESS**.

> **Note on MCP surface:** unlike the other servers in this repo, this module does **not** currently register
> any `@Tool`-annotated methods. It exposes a plain REST API (`TicketController`) plus one **MCP prompt**
> (`analyze-tickets`). The `llm-mcp-client` therefore cannot call ticket operations as MCP tools yet — only the
> prompt is reachable over MCP. If tool exposure is desired, wrap `TicketService` (or the controller methods)
> in `@Tool`-annotated methods and register them via a `ToolCallbackProvider`, mirroring `GitHubMcpTools` /
> `HrMcpTools` in the sibling services.

---

## REST API

| Method | Path                   | Params                                          | Description              |
|--------|------------------------|-------------------------------------------------|--------------------------|
| `POST` | `/tickets`             | `title, description, priority, assignee`        | Create a ticket (WRITE)  |
| `GET`  | `/tickets`             | —                                                | List all tickets         |
| `GET`  | `/tickets/{id}`        | —                                                | Get a ticket by id       |
| `PUT`  | `/tickets/{id}/status` | `status`                                         | Update a ticket's status (WRITE) |
| `PUT`  | `/tickets/{id}/assign` | `assignee`                                       | Assign a ticket (WRITE)  |

`priority` ∈ `TicketPriority`, `status` ∈ `TicketStatus` (see `model/`).

## MCP Prompts

| Name              | Description                                                                 |
|-------------------|-----------------------------------------------------------------------------|
| `analyze-tickets` | Builds an LLM prompt summarising the current ticket backlog (id, title, priority, status — capped via `OutputSizeCapUtil`) and asks for situation analysis, high-priority call-outs, operational risks and recommended actions |

---

## Best Practices Applied

| Practice                       | Status | Notes                                                                                                          |
|--------------------------------|--------|----------------------------------------------------------------------------------------------------------------|
| Centralised error handling     | ✅      | `GlobalExceptionHandler` (`@RestControllerAdvice`) — uniform `{status, error, message, details, timestamp}` body |
| Meaningful 404s                | ✅      | `ResourceNotFoundException` → HTTP 404 for unknown ticket ids                                                  |
| Input validation               | ✅      | Jakarta Bean Validation (`@NotBlank`, `@NotNull`, `@Positive`, `@Validated`) on every controller param → HTTP 400 |
| Bearer token auth              | ✅      | `McpAuthFilter` validates `Authorization: Bearer <mcp.security.token>`; logs `WARN` and runs in insecure dev mode if unset |
| Acting-user propagation        | ✅      | `X-Acting-User` header → `ActingUserContext` thread-local, defaults to `mcp.security.default-user`             |
| Write-operation gating         | ✅      | `enforceWriteGate` in `TicketController` rejects `createTicket` / `updateStatus` / `assignTicket` from the default user when `mcp.security.require-user-for-writes=true` |
| Rate limiting                  | ✅      | In-memory per-user fixed-window limiter (`RateLimiter`, default 120 req/min) → HTTP 429                        |
| Audit logging                  | ✅      | `AUDIT <op> | user=… …args… newId=… outcome=SUCCESS|ERROR latencyMs=…` on every mutating endpoint               |
| Output truncation              | ✅      | `OutputSizeCapUtil.cap` bounds the ticket summary embedded in the `analyze-tickets` prompt so a large backlog can't flood the LLM context |
| Database migrations            | ✅      | Flyway with a dedicated history table (`flyway_schema_history_ticket`) so multiple services can share one DB safely |
| MCP prompts                    | ✅      | `analyze-tickets` registered via `@McpPrompt` (`TicketPromptProvider`)                                          |
| Externalised config            | ✅      | `SecurityProperties` (`@ConfigurationProperties`) — DB creds, tokens, limits all env-overridable               |
| Structured logging             | ✅      | SLF4J/Lombok `@Slf4j`, application-tagged via `spring.application.name`                                        |
| Distributed tracing            | ✅      | Micrometer Tracing → OTLP (`OTEL_EXPORTER_OTLP_ENDPOINT`) → Grafana Tempo                                       |
| Prometheus metrics             | ✅      | `micrometer-registry-prometheus`, scraped at `/actuator/prometheus`                                            |
| Health/auth allow-list         | ✅      | `/actuator/health` and `/actuator/info` are exempt from auth + rate limiting                                   |
| Non-root container             | ✅      | Multi-stage Dockerfile runs as a dedicated system user on a `jre`-only runtime image                           |
| MCP tool exposure              | ❌      | No `@Tool`-annotated methods registered — only reachable via REST and the `analyze-tickets` prompt (see note above) |
| Liveness/readiness probes      | ❌      | `management.endpoint.health.probes.enabled` not set (unlike HR/Gmail/GitHub)                                   |
| Circuit breaker / resilience   | ❌      | No Resilience4j — DB failures surface directly as REST/tool errors                                             |

---

## Configuration

| Property / Env Var                       | Default                                      | Description                                              |
|--------------------------------------------|----------------------------------------------|----------------------------------------------------------|
| `SERVER_PORT`                               | `8081`                                       | HTTP port                                                |
| `DB_URL`                                    | `jdbc:postgresql://localhost:5432/spring_ai` | PostgreSQL JDBC URL                                      |
| `DB_USERNAME`                               | `postgres`                                   | DB username                                              |
| `DB_PASSWORD`                               | `postgres`                                   | DB password                                              |
| `MCP_AUTH_TOKEN` (`mcp.security.token`)     | *(empty → insecure dev mode)*                | Shared bearer token required from MCP/REST clients       |
| `mcp.security.default-user`                 | `system`                                     | Fallback acting user when `X-Acting-User` is absent      |
| `mcp.security.require-user-for-writes`      | `false`                                      | Reject mutating endpoints from the default user when `true` |
| `mcp.security.rate-limit-per-minute`        | `120`                                        | Per-user fixed-window request cap                        |
| `OTEL_EXPORTER_OTLP_ENDPOINT`               | `http://localhost:4318`                      | OTLP traces endpoint (Tempo)                             |
| `TRACING_SAMPLING`                          | `1.0`                                        | Trace sampling probability                               |

---

## Running in Isolation

```bash
cd mcp-server-ticket-service
docker compose up -d         # PostgreSQL only — ticket-service has no MCP-server dependency
export DB_URL=jdbc:postgresql://localhost:5432/spring_ai
export MCP_AUTH_TOKEN=$(uuidgen)
./mvnw spring-boot:run       # :8081
```

---

## curl Commands

### REST API

```bash
TOKEN=<your MCP_AUTH_TOKEN>

# Create a ticket
curl -s -X POST "http://localhost:8081/tickets" \
  -H "Authorization: Bearer $TOKEN" -H 'X-Acting-User: jane.doe' \
  --data-urlencode "title=Redis cluster not responding" \
  --data-urlencode "description=Cluster nodes flapping since 14:00 UTC" \
  --data-urlencode "priority=HIGH" \
  --data-urlencode "assignee=sarah.dev"

# List all tickets
curl -s "http://localhost:8081/tickets" -H "Authorization: Bearer $TOKEN"

# Get a ticket by id
curl -s "http://localhost:8081/tickets/1" -H "Authorization: Bearer $TOKEN"

# Update status
curl -s -X PUT "http://localhost:8081/tickets/1/status?status=RESOLVED" \
  -H "Authorization: Bearer $TOKEN" -H 'X-Acting-User: jane.doe'

# Reassign
curl -s -X PUT "http://localhost:8081/tickets/1/assign?assignee=mark.ops" \
  -H "Authorization: Bearer $TOKEN" -H 'X-Acting-User: jane.doe'
```

### MCP — discovery & prompt

```bash
# List tools (expect an empty/near-empty list — see note above)
curl -s http://localhost:8081/mcp \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'

# List prompts
curl -s http://localhost:8081/mcp \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"jsonrpc":"2.0","id":2,"method":"prompts/list"}'

# Fetch the analyze-tickets prompt
curl -s http://localhost:8081/mcp \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"jsonrpc":"2.0","id":3,"method":"prompts/get","params":{"name":"analyze-tickets"}}'
```

### Actuator

```bash
curl -s http://localhost:8081/actuator/health | jq
curl -s http://localhost:8081/actuator/prometheus | head -40
```
