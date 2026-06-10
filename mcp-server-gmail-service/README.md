# Gmail Service — `mcp-server-gmail-service`

An MCP server that exposes Gmail inbox operations (search, read, label, draft, send, delete) as tools for the
`llm-mcp-client` assistant, backed by the Gmail REST API. Runs on **`:8086`**, MCP protocol **STREAMABLE**, no
datasource — it is a thin, stateless proxy over `gmail.googleapis.com`.

---

## MCP Tools

Defined in `GmailMcpTools` (registered via `MethodToolCallbackProvider` in `McpToolConfig`):

| Tool name         | Type  | Description                                                                |
|-------------------|-------|----------------------------------------------------------------------------|
| `listEmails`      | READ  | List emails, optionally filtered by `labelIds` (INBOX, SENT, SPAM, …) and `maxResults` |
| `getEmail`        | READ  | Full content of an email by `messageId`, with optional `format`            |
| `searchEmails`    | READ  | Search using Gmail query syntax (`from:`, `subject:`, `is:unread`, …)      |
| `getEmailThread`  | READ  | Full conversation thread by `threadId`                                     |
| `getGmailProfile` | READ  | Authenticated user's profile — email address, total message/thread counts  |
| `listLabels`      | READ  | All labels (system + custom) for the authenticated user                    |
| `getEmailsByLabel`| READ  | Emails filtered by a specific label id (`listLabels` first to discover ids)|
| `markAsRead`      | WRITE | Remove the `UNREAD` label from a message                                   |
| `markAsUnread`    | WRITE | Add the `UNREAD` label to a message                                        |
| `createDraft`     | WRITE | Create a draft (`to`, `subject`, `body`)                                   |
| `sendEmail`       | WRITE | Send an email (`to`, `subject`, `body`)                                    |
| `deleteEmail`     | WRITE | Move a message to Trash by `messageId`                                     |

---

## Best Practices Applied

| Practice                       | Status | Notes                                                                                                          |
|--------------------------------|--------|----------------------------------------------------------------------------------------------------------------|
| Centralised error handling     | ✅      | `GlobalExceptionHandler` (`@RestControllerAdvice`) — uniform `{status, error, message, details, timestamp}` body |
| Meaningful 404s                | ✅      | `ResourceNotFoundException` thrown from `GmailService` on `HttpClientErrorException.NotFound` from the Gmail API |
| Input validation               | ✅      | Blank/null guards on every tool argument (`messageId`, `to`, `subject`, `query`, …) → `IllegalArgumentException` → HTTP 400 |
| Bearer token auth              | ✅      | `McpAuthFilter` validates `Authorization: Bearer <mcp.security.token>`; logs a `WARN` and runs in insecure dev mode if unset |
| Acting-user propagation        | ✅      | `X-Acting-User` header → `ActingUserContext` thread-local, defaults to `mcp.security.default-user` |
| Write-operation gating         | ✅      | `enforceWriteGate` rejects mutating tools (`sendEmail`, `deleteEmail`, `createDraft`, `markAsRead/Unread`) from the default user when `mcp.security.require-user-for-writes=true` |
| Rate limiting                  | ✅      | In-memory per-user fixed-window limiter (`RateLimiter`, default 120 req/min) → HTTP 429 |
| Audit logging                  | ✅      | Every tool call logs `TOOL <name> | user=… …args… latencyMs=…` with outcome on success/error |
| Output truncation              | ✅      | `OutputSizeCapUtil.cap` truncates Gmail API responses beyond `mcp.output.max-chars` (default 8 000) — important here since raw message bodies can be large |
| Externalised config            | ✅      | `GmailProperties` / `SecurityProperties` (`@ConfigurationProperties`) — token, base URL, user id, page size all env-overridable |
| Structured logging             | ✅      | SLF4J/Lombok `@Slf4j`, application-tagged via `spring.application.name` |
| Distributed tracing            | ✅      | Micrometer Tracing → OTLP (`OTEL_EXPORTER_OTLP_ENDPOINT`) → Grafana Tempo |
| Prometheus metrics             | ✅      | `micrometer-registry-prometheus`, scraped at `/actuator/prometheus` |
| Liveness/readiness probes      | ✅      | `management.endpoint.health.probes.enabled: true` |
| Health/auth allow-list         | ✅      | `/actuator/health` and `/actuator/info` are exempt from auth + rate limiting |
| Non-root container             | ✅      | Multi-stage Dockerfile runs as a dedicated `spring:spring` system user on a `jre`-only runtime image |
| Destructive-action awareness   | ⚠️      | `deleteEmail` only moves to Trash (Gmail's reversible delete) rather than permanent erasure — but still gated behind write-tool classification on the client and `require-user-for-writes` here |
| Circuit breaker / resilience   | ❌      | No Resilience4j — Gmail API failures surface directly to the caller as tool errors |
| Token refresh                  | ❌      | `gmail.access-token` is a static OAuth2 token (env var); no refresh-token flow — must be rotated manually when it expires |

---

## Design Patterns (GoF)

| Pattern | Where | Role |
|---------|-------|------|
| **Facade** | `GmailService` | Hides Gmail REST API details (URIs, query params, error translation) behind simple methods |
| **Builder** | `RestClient.builder()` in `GmailClientConfig` | Stepwise construction of the configured Gmail client |
| **Factory Method** | `@Bean` methods in `GmailClientConfig`, `McpToolConfig` | Container builds and wires the REST client and tool provider |
| **Observer** | `@EventListener(ContextRefreshedEvent)` (`warnIfNoToken`) | Startup event subscription warns when no Gmail access token is configured |
| **Singleton** | All Spring beans | One shared, stateless instance per container |
| **Template Method** | `McpAuthFilter extends OncePerRequestFilter` | Framework skeleton calls `doFilterInternal` / `shouldNotFilter` hooks |
| **Chain of Responsibility** | Servlet `FilterChain` | Auth → rate-limit → tools, each link handles or passes on |
| **Command** | `@Tool` methods (`listEmails`, `sendEmail`, …) wrapped as `ToolCallback` objects | Tool invocations reified for the MCP runtime |

## Configuration

| Property / Env Var                          | Default                                  | Description                                              |
|-----------------------------------------------|------------------------------------------|----------------------------------------------------------|
| `SERVER_PORT`                                  | `8086`                                   | HTTP port                                                |
| `GMAIL_ACCESS_TOKEN` (`gmail.access-token`)    | *(empty → calls fail with 401)*          | OAuth2 access token, obtained via Google OAuth2 / service account |
| `gmail.api-base-url`                           | `https://gmail.googleapis.com/gmail/v1`  | Gmail REST API base URL                                  |
| `gmail.user-id`                                | `me`                                     | Gmail user id (`me` = authenticated user)                |
| `gmail.default-page-size`                      | `20`                                     | Default `maxResults` for list/search operations          |
| `MCP_AUTH_TOKEN` (`mcp.security.token`)        | *(empty → insecure dev mode)*            | Shared bearer token required from MCP clients            |
| `mcp.security.default-user`                    | `system`                                 | Fallback acting user when `X-Acting-User` is absent      |
| `mcp.security.require-user-for-writes`         | `false`                                  | Reject write tools from the default user when `true`     |
| `mcp.security.rate-limit-per-minute`           | `120`                                    | Per-user fixed-window request cap                        |
| `mcp.output.max-chars`                         | `8000`                                   | Max characters returned per tool before truncation       |
| `OTEL_EXPORTER_OTLP_ENDPOINT`                  | `http://localhost:4318`                  | OTLP traces endpoint (Tempo)                             |
| `TRACING_SAMPLING`                             | `1.0`                                    | Trace sampling probability                               |

---

## Running in Isolation

```bash
cd mcp-server-gmail-service
export GMAIL_ACCESS_TOKEN=ya29.xxxx        # short-lived OAuth2 access token
export MCP_AUTH_TOKEN=$(uuidgen)
./mvnw spring-boot:run                     # :8086
```

Or via the root compose stack (brings up Tempo/Prometheus/Grafana alongside it):

```bash
docker compose up gmail-service
```

---

## curl Commands

> MCP requests are JSON-RPC 2.0 over the streamable-HTTP endpoint `/mcp`. Replace `$TOKEN` with your
> `MCP_AUTH_TOKEN`.

### List available tools

```bash
curl -s http://localhost:8086/mcp \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'
```

### List recent inbox emails

```bash
curl -s http://localhost:8086/mcp \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
        "jsonrpc":"2.0","id":2,"method":"tools/call",
        "params":{"name":"listEmails","arguments":{"labelIds":"INBOX","maxResults":10}}
      }'
```

### Get a single email

```bash
curl -s http://localhost:8086/mcp \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
        "jsonrpc":"2.0","id":3,"method":"tools/call",
        "params":{"name":"getEmail","arguments":{"messageId":"18d4f2a9b7c3e1f0","format":"full"}}
      }'
```

### Search using Gmail query syntax

```bash
curl -s http://localhost:8086/mcp \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
        "jsonrpc":"2.0","id":4,"method":"tools/call",
        "params":{"name":"searchEmails","arguments":{"query":"is:unread from:boss@example.com","maxResults":10}}
      }'
```

### Get a thread

```bash
curl -s http://localhost:8086/mcp \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
        "jsonrpc":"2.0","id":5,"method":"tools/call",
        "params":{"name":"getEmailThread","arguments":{"threadId":"18d4f2a9b7c3e1f0"}}
      }'
```

### Profile / labels

```bash
curl -s http://localhost:8086/mcp \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"jsonrpc":"2.0","id":6,"method":"tools/call","params":{"name":"getGmailProfile","arguments":{}}}'

curl -s http://localhost:8086/mcp \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"jsonrpc":"2.0","id":7,"method":"tools/call","params":{"name":"listLabels","arguments":{}}}'
```

### Emails by label

```bash
curl -s http://localhost:8086/mcp \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
        "jsonrpc":"2.0","id":8,"method":"tools/call",
        "params":{"name":"getEmailsByLabel","arguments":{"labelId":"IMPORTANT","maxResults":10}}
      }'
```

### Mark read / unread (write — pass `X-Acting-User` if `require-user-for-writes` is enabled)

```bash
curl -s http://localhost:8086/mcp \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'X-Acting-User: jane.doe' \
  -d '{"jsonrpc":"2.0","id":9,"method":"tools/call","params":{"name":"markAsRead","arguments":{"messageId":"18d4f2a9b7c3e1f0"}}}'

curl -s http://localhost:8086/mcp \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'X-Acting-User: jane.doe' \
  -d '{"jsonrpc":"2.0","id":10,"method":"tools/call","params":{"name":"markAsUnread","arguments":{"messageId":"18d4f2a9b7c3e1f0"}}}'
```

### Create a draft (write)

```bash
curl -s http://localhost:8086/mcp \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'X-Acting-User: jane.doe' \
  -d '{
        "jsonrpc":"2.0","id":11,"method":"tools/call",
        "params":{"name":"createDraft","arguments":{
          "to":"alice@example.com","subject":"Deployment update",
          "body":"The billing-api deployment has been rescheduled to tomorrow at 2pm."
        }}
      }'
```

### Send an email (write)

```bash
curl -s http://localhost:8086/mcp \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'X-Acting-User: jane.doe' \
  -d '{
        "jsonrpc":"2.0","id":12,"method":"tools/call",
        "params":{"name":"sendEmail","arguments":{
          "to":"alice@example.com","subject":"Deployment update",
          "body":"The billing-api deployment has been rescheduled to tomorrow at 2pm."
        }}
      }'
```

### Delete (move to Trash) — write

```bash
curl -s http://localhost:8086/mcp \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'X-Acting-User: jane.doe' \
  -d '{"jsonrpc":"2.0","id":13,"method":"tools/call","params":{"name":"deleteEmail","arguments":{"messageId":"18d4f2a9b7c3e1f0"}}}'
```

### Actuator

```bash
curl -s http://localhost:8086/actuator/health | jq
curl -s http://localhost:8086/actuator/prometheus | head -40
```
