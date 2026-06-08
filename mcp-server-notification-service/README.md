# Notification Service — `mcp-server-notification-service`

An MCP server that sends and lists notifications across channels (INTERNAL, EMAIL, SLACK), backed by PostgreSQL
(Flyway-migrated). Runs on **`:8083`**, MCP protocol **STATELESS**.

---

## MCP Tools

Defined in `NotificationTools` (registered via `MethodToolCallbackProvider` in `McpToolConfig`):

| Tool name          | Type  | Description                                                                |
|--------------------|-------|-----------------------------------------------------------------------------|
| `getNotifications` | READ  | Get all notifications                                                       |
| `sendNotification` | WRITE | Send a notification — `channel` (`INTERNAL`/`EMAIL`/`SLACK`), `recipient` team, `message` |

---

## Best Practices Applied

| Practice                       | Status | Notes                                                                                                          |
|--------------------------------|--------|----------------------------------------------------------------------------------------------------------------|
| Centralised error handling     | ✅      | `GlobalExceptionHandler` (`@RestControllerAdvice`) — uniform `{status, error, message, details, timestamp}` body |
| Meaningful 404s                | ✅      | `ResourceNotFoundException` → HTTP 404 for unknown notification ids                                            |
| Input validation               | ✅      | `@ToolParam` typed enum (`NotificationChannel`) + blank/null guards → `IllegalArgumentException` → HTTP 400    |
| Bearer token auth              | ✅      | `McpAuthFilter` validates `Authorization: Bearer <mcp.security.token>`; logs `WARN` and runs in insecure dev mode if unset |
| Acting-user propagation        | ✅      | `X-Acting-User` header → `ActingUserContext` thread-local, defaults to `mcp.security.default-user`             |
| Write-operation gating         | ✅      | `enforceWriteGate` rejects `sendNotification` from the default user when `mcp.security.require-user-for-writes=true` |
| Rate limiting                  | ✅      | In-memory per-user fixed-window limiter (`RateLimiter`, default 120 req/min) → HTTP 429                        |
| Audit logging                  | ✅      | Every tool call logs `TOOL <name> | user=… channel=… recipient=… outcome=… latencyMs=…`                         |
| Output truncation              | ✅      | `OutputSizeCapUtil.cap` truncates tool responses                                                               |
| Database migrations            | ✅      | Flyway with a dedicated history table (`flyway_schema_history_notification`) so multiple services can share one DB safely |
| Externalised config            | ✅      | `SecurityProperties` (`@ConfigurationProperties`) — DB creds, tokens, limits all env-overridable               |
| Structured logging             | ✅      | SLF4J/Lombok `@Slf4j`, application-tagged via `spring.application.name`                                        |
| Distributed tracing            | ✅      | Micrometer Tracing → OTLP (`OTEL_EXPORTER_OTLP_ENDPOINT`) → Grafana Tempo                                       |
| Prometheus metrics             | ✅      | `micrometer-registry-prometheus`, scraped at `/actuator/prometheus`                                            |
| Health/auth allow-list         | ✅      | `/actuator/health` and `/actuator/info` are exempt from auth + rate limiting                                   |
| Non-root container             | ✅      | Multi-stage Dockerfile runs as a dedicated system user on a `jre`-only runtime image                           |
| Auth wiring in `application.yaml` | ⚠️   | `SecurityProperties` documents `mcp.security.*`, but **no `mcp:` block is present in `application.yaml`** — the filter still binds via `@ConfigurationProperties` defaults, so set `MCP_AUTH_TOKEN` to enable auth (see root [README](../README.md#notification-service--mcp-server-notification-service-8083)) |
| Liveness/readiness probes      | ❌      | `management.endpoint.health.probes.enabled` not set (unlike HR/Gmail/GitHub/Deployment)                        |
| Circuit breaker / resilience   | ❌      | No Resilience4j — DB failures surface directly as tool errors                                                  |

---

## Configuration

| Property / Env Var                       | Default                                      | Description                                              |
|--------------------------------------------|----------------------------------------------|----------------------------------------------------------|
| `SERVER_PORT`                               | `8083`                                       | HTTP port                                                |
| `DB_URL`                                    | `jdbc:postgresql://localhost:5432/spring_ai` | PostgreSQL JDBC URL                                      |
| `DB_USERNAME`                               | `postgres`                                   | DB username                                              |
| `DB_PASSWORD`                               | `postgres`                                   | DB password                                              |
| `MCP_AUTH_TOKEN` (`mcp.security.token`)     | *(empty → insecure dev mode)*                | Shared bearer token required from MCP clients            |
| `mcp.security.default-user`                 | `system`                                     | Fallback acting user when `X-Acting-User` is absent      |
| `mcp.security.require-user-for-writes`      | `false`                                      | Reject `sendNotification` from the default user when `true` |
| `mcp.security.rate-limit-per-minute`        | `120`                                        | Per-user fixed-window request cap                        |
| `OTEL_EXPORTER_OTLP_ENDPOINT`               | `http://localhost:4318`                      | OTLP traces endpoint (Tempo)                             |
| `TRACING_SAMPLING`                          | `1.0`                                        | Trace sampling probability                               |

---

## Running in Isolation

```bash
cd mcp-server-notification-service
docker compose up -d         # PostgreSQL only — no other MCP-server dependency
export DB_URL=jdbc:postgresql://localhost:5432/spring_ai
export MCP_AUTH_TOKEN=$(uuidgen)
./mvnw spring-boot:run       # :8083
```

---

## curl Commands

> MCP requests are JSON-RPC 2.0 over the streamable-HTTP endpoint `/mcp`. Replace `$TOKEN` with your
> `MCP_AUTH_TOKEN`.

### List available tools

```bash
curl -s http://localhost:8083/mcp \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'
```

### List notifications

```bash
curl -s http://localhost:8083/mcp \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"getNotifications","arguments":{}}}'
```

### Send a notification (write — pass `X-Acting-User` if `require-user-for-writes` is enabled)

```bash
curl -s http://localhost:8083/mcp \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'X-Acting-User: jane.doe' \
  -d '{
        "jsonrpc":"2.0","id":3,"method":"tools/call",
        "params":{"name":"sendNotification","arguments":{
          "channel":"SLACK","recipient":"dev-team",
          "message":"Deployment 4 has been cancelled — see #incidents for details"
        }}
      }'
```

### Actuator

```bash
curl -s http://localhost:8083/actuator/health | jq
curl -s http://localhost:8083/actuator/prometheus | head -40
```
