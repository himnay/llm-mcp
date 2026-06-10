# AI MCP Client — `llm-mcp-client`

The **MCP client / chat orchestrator** of the stack. Exposes a single `POST /chat` endpoint backed by an
OpenAI-powered `ChatClient` that calls out to six downstream MCP servers (ticket, deployment, notification,
hr, github, gmail) as tools, persists conversation history in PostgreSQL, and wraps every outbound call with
resilience, rate-limiting, truncation and observability controls. Spring app name `ai-mcp-server`, runs on
**`:8080`** (default Spring Boot port — not overridden in `application.yaml`).

---

## MCP Server Connections

Configured under `mcp.client.streamable-http.connections` in `application.yaml` — one `McpSyncClient` per
downstream server, each secured and load-balanced through `ResilientToolCallbackProvider`:

| Connection name | URL                     | Downstream service           | Circuit breaker |
|-----------------|-------------------------|-------------------------------|-----------------|
| `ticket`        | `http://localhost:8081` | `mcp-server-ticket-service`       | `mcp-ticket`       |
| `deployment`    | `http://localhost:8082` | `mcp-server-deployment-service`   | `mcp-deployment`   |
| `notification`  | `http://localhost:8083` | `mcp-server-notification-service` | `mcp-notification` |
| `hr`            | `http://localhost:8084` | `mcp-server-hr-service`           | `mcp-hr`           |
| `github`        | `http://localhost:8085` | `mcp-server-github-service`       | `mcp-github`       |
| `gmail`         | `http://localhost:8086` | `mcp-server-gmail-service`        | `mcp-gmail`        |

Each connection contributes its tools to a single aggregated `ToolCallbackProvider` — the model sees all
~25 downstream tools (e.g. `applyLeave`, `getDeployments`, `sendNotification`, `getRepository`, `sendEmail`, …)
as one flat catalogue and picks whichever fits the user's request. The `ticket` connection currently exposes
no `@Tool`s (see [ticket-service README](../mcp-server-ticket-service/README.md)) — only its `analyze-tickets`
MCP prompt is reachable via `PromptLoader`.

---

## Chat API

| Method | Path    | Body                          | Description                                               |
|--------|---------|-------------------------------|-----------------------------------------------------------|
| `POST` | `/chat` | `{"message": "<user text>"}`  | Send a message; returns `{"reply": "<assistant text>"}`. Supports `/promptName arg1 arg2` shorthand to expand a downstream MCP prompt before sending to the LLM (see `PromptLoader`) |

Headers: `X-User-Id` resolves the acting/conversation user (defaults to `assistant.default-user`); forwarded
downstream as `X-Acting-User` by `McpClientSecurityConfig`.

---

## Best Practices Applied

| Practice                          | Status | Notes                                                                                                          |
|-----------------------------------|--------|----------------------------------------------------------------------------------------------------------------|
| Centralised error handling        | ✅      | `GlobalExceptionHandler` (`@RestControllerAdvice`) — uniform `{status, error, message, details, timestamp}` body |
| Input validation                  | ✅      | `@Valid` on `ChatRequest` (Jakarta Bean Validation) → HTTP 400 with field-level details                        |
| Bearer token auth (outbound)      | ✅      | `McpClientSecurityConfig` installs an `McpSyncHttpClientRequestCustomizer` that attaches `Authorization: Bearer ${assistant.mcp-auth-token}` to every downstream MCP call |
| Acting-user propagation           | ✅      | `RequestContextFilter` resolves the user from `X-User-Id` (or `assistant.default-user`) into `RequestContext` (thread-local), forwarded downstream as `X-Acting-User` |
| Rate limiting                     | ✅      | `RequestContextFilter` + `RateLimiter` (in-memory per-user fixed-window, `assistant.rate-limit-per-minute=30`) → HTTP 429 on `/chat*` |
| Bounded tool-calling loops        | ✅      | `BoundedToolCallingManager` caps tool-execution rounds per request at `assistant.max-tool-iterations=5`, throwing `IllegalStateException` beyond that — guards against runaway agent loops |
| Output / context-stuffing guard   | ✅      | `TruncatingToolCallback` caps each tool result at `assistant.max-tool-result-chars=8000` chars with a `…[truncated]` marker before it re-enters the model context |
| Circuit breaker / resilience      | ✅      | `ResilienceConfig` registers six named Resilience4j circuit breakers (`mcp-hr`, `mcp-ticket`, `mcp-deployment`, `mcp-notification`, `mcp-github`, `mcp-gmail`) — count-based sliding window (10 calls), 50% failure-rate / 80% slow-call-rate thresholds, 5 s slow-call duration, 30 s open-state wait, 3 permitted half-open calls, auto-transition; `ResilientToolCallbackProvider` wraps every tool callback per its owning server and returns a structured `{"error": "<server> is temporarily unavailable …"}` fallback when the circuit is open |
| Conversation memory               | ✅      | `PostgresConversationStore` persists each user/assistant exchange to `chat_message` (Flyway-migrated) and replays the last `assistant.memory-window=20` messages as history on each turn |
| Prompt templating                 | ✅      | System prompt rendered from `prompts/system.st` via `PromptTemplate` with `{assistantName}`, `{currentUser}`, `{currentTime}` placeholders |
| MCP prompt expansion              | ✅      | `PromptLoader` expands `/promptName arg1 arg2…` shorthand into a rendered downstream MCP prompt (via `McpSyncClient.getPrompt`) before the LLM call |
| Token usage metering              | ✅      | `ChatService.recordTokenUsage` records `ai.tokens{type=prompt|completion, user=…}` Micrometer counters from `ChatResponse` usage metadata, scraped by Prometheus |
| Database migrations               | ✅      | Flyway with a dedicated history table (`flyway_schema_history_chat`) — `V1__create_chat_history.sql` creates `chat_message` with a `(conversation_id, created_at)` index |
| Query timeouts / pool tuning      | ✅      | HikariCP `connection-timeout: 10000`; `spring.jpa.hibernate.ddl-auto: validate` + `open-in-view: false` |
| Downstream health aggregation     | ✅      | `McpClientHealthIndicator` pings every configured `McpSyncClient` and surfaces per-server reachability through `/actuator/health` (DOWN if any server is unreachable) |
| Structured logging                | ✅      | SLF4J/Lombok `@Slf4j`; `BoundedToolCallingManager` logs every tool-call round (`tool-call iteration=… user=… tool=…`) and its latency |
| Distributed tracing               | ✅      | Micrometer Tracing → OTLP (`OTEL_EXPORTER_OTLP_ENDPOINT`) → Grafana Tempo                                       |
| Prometheus metrics                | ✅      | `micrometer-registry-prometheus`, scraped at `/actuator/prometheus`, tagged with `application: ${spring.application.name}` |
| Externalised config               | ✅      | `AssistantProperties` (`@ConfigurationProperties(prefix = "assistant")`) — name, default user, auth token, memory window, iteration/result caps, rate limit, sensitive words, write-tool keywords all env/property overridable |
| Write-action awareness            | ⚠️      | `assistant.write-tool-keywords` (apply/create/update/delete/send/deploy/…) and `assistant.sensitive-words` are bound and available on `RequestContext.allowWriteTools()`, but enforcement of write-gating ultimately lives server-side (`enforceWriteGate` in each MCP server) — the client surfaces the signal rather than blocking centrally |
| Bearer token auth (inbound)       | ❌      | `/chat` itself has no inbound authentication/authorization — only `X-User-Id` for identity resolution; add a `SecurityFilterChain` if exposing beyond a trusted network |
| Non-root container / Dockerfile   | ✅      | Multi-stage `Dockerfile`, runs as a non-root `spring` user — matches the MCP server modules            |

---

## Configuration

| Property / Env Var                              | Default                                      | Description                                                       |
|--------------------------------------------------|----------------------------------------------|-------------------------------------------------------------------|
| `DB_URL`                                          | `jdbc:postgresql://localhost:5432/spring_ai` | PostgreSQL JDBC URL for conversation memory                       |
| `DB_USERNAME`                                     | `postgres`                                   | DB username                                                       |
| `DB_PASSWORD`                                     | `postgres`                                   | DB password                                                       |
| `OPENAI_API_KEY` (`spring.ai.openai.api-key`)     | *(required)*                                 | OpenAI API key for the chat model                                 |
| `MCP_AUTH_TOKEN` (`assistant.mcp-auth-token`)     | *(empty)*                                    | Shared bearer token attached to every outbound MCP call            |
| `assistant.name`                                  | `Enterprise AI Assistant`                    | Assistant persona name, rendered into the system prompt            |
| `assistant.default-user`                          | `himansu.nayak`                                   | Fallback acting/conversation user when `X-User-Id` is absent       |
| `assistant.memory-window`                         | `20`                                         | Number of past messages replayed as conversation history per turn  |
| `assistant.max-tool-iterations`                   | `5`                                          | Hard cap on tool-execution rounds per chat request                 |
| `assistant.max-tool-result-chars`                 | `8000`                                       | Max characters of a single tool result fed back into the model     |
| `assistant.rate-limit-per-minute`                 | `30`                                         | Per-user `/chat` requests allowed per minute                       |
| `assistant.sensitive-words`                       | `[]`                                         | Words that, if present in a prompt, should be blocked              |
| `assistant.write-tool-keywords`                   | `[apply, create, update, delete, send, deploy, trigger, rollback, cancel, remove, approve, assign, reschedule]` | Tool-name substrings treated as write/destructive |
| `mcp.client.streamable-http.connections.*.url`    | *(see table above)*                          | Per-server downstream MCP base URLs                                |
| `OTEL_EXPORTER_OTLP_ENDPOINT`                     | `http://localhost:4318`                      | OTLP traces endpoint (Tempo)                                       |
| `TRACING_SAMPLING`                                | `1.0`                                        | Trace sampling probability                                         |

---

## Running in Isolation

This module depends only on **PostgreSQL** (for conversation memory) and the **OpenAI API** — none of the
downstream MCP servers are required to start the app, though tool calls will fail/circuit-break if they're
unreachable.

`docker-compose.yml` brings up just the module's dependencies — **PostgreSQL**, **Prometheus**, and **Grafana**
(provisioned with the client's own dashboards via `../observability`) — so you can run the app itself on the
host with hot reload (`./mvnw spring-boot:run`) while everything it needs lives in containers.

```bash
cd llm-mcp-client
docker compose up -d                  # postgres (llm-postgres) :5432, prometheus :9090, grafana :3000 (admin/admin)
export DB_URL=jdbc:postgresql://localhost:5432/spring_ai
export OPENAI_API_KEY=sk-xxxx
export MCP_AUTH_TOKEN=$(uuidgen)      # must match the token configured on the downstream MCP servers
./mvnw spring-boot:run                # :8080
```

A multi-stage `Dockerfile` is also provided if you'd rather build and run the client itself as a container
(e.g. to add it as a service to this or the root `docker-compose.yml`).

To exercise the full flow, also start the downstream MCP servers (see each service's own README / the root
[`docker-compose.yml`](../docker-compose.yml)) on ports 8081–8086 with the same `MCP_AUTH_TOKEN`.

---

## curl Commands

### Send a chat message

```bash
curl -s -X POST http://localhost:8080/chat \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: jane.doe' \
  -d '{"message":"What deployments are scheduled for this week?"}'
```

### Use an MCP-prompt shorthand (expanded by `PromptLoader` before the LLM call)

```bash
curl -s -X POST http://localhost:8080/chat \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: jane.doe' \
  -d '{"message":"/analyze-tickets"}'
```

### Actuator (health includes per-MCP-server reachability via `McpClientHealthIndicator`)

```bash
curl -s http://localhost:8080/actuator/health | jq
curl -s http://localhost:8080/actuator/prometheus | head -40
curl -s http://localhost:8080/actuator/prometheus | grep ai_tokens
curl -s http://localhost:8080/actuator/prometheus | grep resilience4j_circuitbreaker_state
```
