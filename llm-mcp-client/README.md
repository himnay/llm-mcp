# AI MCP Client — `llm-mcp-client`

The **MCP client / chat orchestrator** of the stack. Exposes a single `POST /chat` endpoint backed by an
OpenAI-powered `ChatClient` that dispatches tool calls to downstream MCP servers (ticket, deployment, notification,
hr, github, gmail are all pre-wired as named connections — see the table below for which are actually enabled by
default), persists conversation history in PostgreSQL, and wraps every outbound call with resilience, rate-limiting,
truncation and observability controls. Beyond plain tool calling, it also plays the *client* side of three optional
MCP capabilities that a connected STREAMABLE server can invoke mid-call: `McpSamplingHandler` (lets a server ask this
client's LLM to run a completion on its behalf — used by `mcp-server-github-service`'s `summarizeRepositoryHealth`),
`McpElicitationHandler` (answers a server's request for structured human confirmation by policy, since `/chat` has no
human attached mid-call — used by `mcp-server-deployment-service`'s `executeDeployment`), and `McpProgressHandler`
(logs `notifications/progress` events from a long-running tool call, also used by `executeDeployment`). Every inbound
message additionally passes through `PromptInjectionGuard` before the LLM or any tool is touched — see the root
[README's Prompt Injection Security section](../README.md#prompt-injection-security). Spring app name
`ai-mcp-server`, runs on **`:8080`** (default Spring Boot port — not overridden in `application.yaml`).

---

## MCP Server Connections

Configured under `spring.ai.mcp.client.streamable-http.connections` in `application.yaml` — one `McpSyncClient` per
downstream server, each secured and load-balanced through `ResilientToolCallbackProvider`. `ResilienceConfig`
pre-creates a circuit breaker for all six named downstream servers regardless of which are actually wired up:

| Connection name | URL                     | Downstream service                | Circuit breaker    | Wired up in `application.yaml`? |
|-----------------|-------------------------|-----------------------------------|--------------------|-----------------------------------|
| `deployment`    | `http://localhost:8082` | `mcp-server-deployment-service`   | `mcp-deployment`   | ✅ active                         |
| `github`        | `http://localhost:8085` | `mcp-server-github-service`       | `mcp-github`       | ✅ active                         |
| `ticket`        | `http://localhost:8081` | `mcp-server-ticket-service`       | `mcp-ticket`       | ⏸ commented out                  |
| `notification`  | `http://localhost:8083` | `mcp-server-notification-service` | `mcp-notification` | ⏸ commented out                  |
| `hr`            | `http://localhost:8084` | `mcp-server-hr-service`           | `mcp-hr`           | ⏸ commented out                  |
| `gmail`         | `http://localhost:8086` | `mcp-server-gmail-service`        | `mcp-gmail`        | ⏸ commented out                  |

As currently checked in, only `deployment` and `github` are uncommented, so only those two servers' tools are
actually reachable from a running `/chat` call — the other four blocks exist in the same YAML file, ready to
uncomment. `AppConfig` builds the aggregated `ToolCallbackProvider` from whichever `List<McpSyncClient>` Spring AI
auto-configures from that file — the model only ever sees tools from servers that are both configured *and*
reachable at startup. The `ticket` connection, even if enabled, currently exposes no `@McpTool`s (see
[ticket-service README](../mcp-server-ticket-service/README.md)) — only its `analyze-tickets` MCP prompt is
reachable via `PromptLoader`. `travel` is not present in this file at all.

---

## Chat API

| Method | Path    | Body                         | Description                                                                                                                                                                          |
|--------|---------|------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `POST` | `/chat` | `{"message": "<user text>"}` | Send a message; returns `{"reply": "<assistant text>"}`. Supports `/promptName arg1 arg2` shorthand to expand a downstream MCP prompt before sending to the LLM (see `PromptLoader`) |

Headers: `X-User-Id` resolves the acting/conversation user (defaults to `assistant.default-user`); forwarded
downstream as `X-Acting-User` by `McpClientSecurityConfig`.

---

## Best Practices Applied

| Practice                        | Status | Notes                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
|---------------------------------|--------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Centralised error handling      | ✅      | `GlobalExceptionHandler` (`@RestControllerAdvice`) — uniform `{status, error, message, details, timestamp}` body                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| Input validation                | ✅      | `@Valid` on `ChatRequest` (Jakarta Bean Validation) → HTTP 400 with field-level details                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| Bearer token auth (outbound)    | ✅      | `McpClientSecurityConfig` installs an `McpSyncHttpClientRequestCustomizer` that attaches `Authorization: Bearer ${assistant.mcp-auth-token}` to every downstream MCP call                                                                                                                                                                                                                                                                                                                                                                             |
| Acting-user propagation         | ✅      | `RequestContextFilter` resolves the user from `X-User-Id` (or `assistant.default-user`) into `RequestContext` (thread-local), forwarded downstream as `X-Acting-User`                                                                                                                                                                                                                                                                                                                                                                                 |
| Rate limiting                   | ✅      | `RequestContextFilter` + `RateLimiter` (in-memory per-user fixed-window, `assistant.rate-limit-per-minute=30`) → HTTP 429 on `/chat*`                                                                                                                                                                                                                                                                                                                                                                                                                 |
| Bounded tool-calling loops      | ✅      | `BoundedToolCallingManager` caps tool-execution rounds per request at `assistant.max-tool-iterations=5`, throwing `IllegalStateException` beyond that — guards against runaway agent loops                                                                                                                                                                                                                                                                                                                                                            |
| Output / context-stuffing guard | ✅      | `TruncatingToolCallback` caps each tool result at `assistant.max-tool-result-chars=8000` chars with a `…[truncated]` marker before it re-enters the model context                                                                                                                                                                                                                                                                                                                                                                                     |
| Circuit breaker / resilience    | ✅      | `ResilienceConfig` registers six named Resilience4j circuit breakers (`mcp-hr`, `mcp-ticket`, `mcp-deployment`, `mcp-notification`, `mcp-github`, `mcp-gmail`) — count-based sliding window (10 calls), 50% failure-rate / 80% slow-call-rate thresholds, 5 s slow-call duration, 30 s open-state wait, 3 permitted half-open calls, auto-transition; `ResilientToolCallbackProvider` wraps every tool callback per its owning server and returns a structured `{"error": "<server> is temporarily unavailable …"}` fallback when the circuit is open |
| Conversation memory             | ✅      | `PostgresConversationStore` persists each user/assistant exchange to `chat_message` (Flyway-migrated) and replays the last `assistant.memory-window=20` messages as history on each turn                                                                                                                                                                                                                                                                                                                                                              |
| Prompt templating               | ✅      | System prompt rendered from `prompts/system.st` via `PromptTemplate` with `{assistantName}`, `{currentUser}`, `{currentTime}` placeholders                                                                                                                                                                                                                                                                                                                                                                                                            |
| MCP prompt expansion            | ✅      | `PromptLoader` expands `/promptName arg1 arg2…` shorthand into a rendered downstream MCP prompt (via `McpSyncClient.getPrompt`) before the LLM call                                                                                                                                                                                                                                                                                                                                                                                                   |
| Token usage metering            | ✅      | `ChatService.recordTokenUsage` records `ai.tokens{type=prompt                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |completion, user=…}` Micrometer counters from `ChatResponse` usage metadata, scraped by Prometheus |
| Database migrations             | ✅      | Flyway with a dedicated history table (`flyway_schema_history_chat`) — `V1__create_chat_history.sql` creates `chat_message` with a `(conversation_id, created_at)` index                                                                                                                                                                                                                                                                                                                                                                              |
| Query timeouts / pool tuning    | ✅      | HikariCP `connection-timeout: 10000`; `spring.jpa.hibernate.ddl-auto: validate` + `open-in-view: false`                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| Downstream health aggregation   | ✅      | `McpClientHealthIndicator` pings every configured `McpSyncClient` and surfaces per-server reachability through `/actuator/health` (DOWN if any server is unreachable)                                                                                                                                                                                                                                                                                                                                                                                 |
| Structured logging              | ✅      | SLF4J/Lombok `@Slf4j`; `BoundedToolCallingManager` logs every tool-call round (`tool-call iteration=… user=… tool=…`) and its latency                                                                                                                                                                                                                                                                                                                                                                                                                 |
| Distributed tracing             | ✅      | Micrometer Tracing → OTLP (`OTEL_EXPORTER_OTLP_ENDPOINT`) → Grafana Tempo                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| Prometheus metrics              | ✅      | `micrometer-registry-prometheus`, scraped at `/actuator/prometheus`, tagged with `application: ${spring.application.name}`                                                                                                                                                                                                                                                                                                                                                                                                                            |
| Externalised config             | ✅      | `AssistantProperties` (`@ConfigurationProperties(prefix = "assistant")`) — name, default user, auth token, memory window, iteration/result caps, rate limit, sensitive words, write-tool keywords all env/property overridable                                                                                                                                                                                                                                                                                                                        |
| Write-action awareness          | ⚠️     | `assistant.write-tool-keywords` (apply/create/update/delete/send/deploy/…) and `assistant.sensitive-words` are bound and available on `RequestContext.allowWriteTools()`, but enforcement of write-gating ultimately lives server-side (`enforceWriteGate` in each MCP server) — the client surfaces the signal rather than blocking centrally                                                                                                                                                                                                        |
| Bearer token auth (inbound)     | ❌      | `/chat` itself has no inbound authentication/authorization — only `X-User-Id` for identity resolution; add a `SecurityFilterChain` if exposing beyond a trusted network                                                                                                                                                                                                                                                                                                                                                                               |
| Non-root container / Dockerfile | ✅      | Multi-stage `Dockerfile`, runs as a non-root `spring` user — matches the MCP server modules                                                                                                                                                                                                                                                                                                                                                                                                                                                           |

---

## Design Patterns (GoF)

| Pattern                     | Where                                                                                           | Role                                                                                                                                    |
|-----------------------------|-------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| **Decorator**               | `TruncatingToolCallback`, `CircuitBreakerToolCallback` (inside `ResilientToolCallbackProvider`) | Wrap a `ToolCallback` to add output truncation / circuit breaking without changing the wrapped tool                                     |
| **Proxy (protection)**      | `ResilientToolCallbackProvider`                                                                 | Stands in for downstream MCP servers; when a circuit is OPEN it short-circuits with a structured error instead of a doomed network call |
| **Mediator**                | `ChatService` (with `BoundedToolCallingManager`)                                                | Coordinates prompt template, conversation memory, OpenAI model and MCP tools so none of them reference each other                       |
| **Memento**                 | `PostgresConversationStore` + `ChatMessageEntity`                                               | Conversation state is externalised, persisted and restored per turn (capped by `assistant.memory-window`)                               |
| **Adapter**                 | `PostgresConversationStore`                                                                     | Adapts JPA rows to Spring AI `Message` objects; `SyncMcpToolCallbackProvider` adapts MCP clients to `ToolCallback`s                     |
| **Facade**                  | `ChatService.handleMessage`                                                                     | One call hides prompt rendering, history load, model call, persistence and token metering                                               |
| **Factory Method**          | `@Bean` methods in `AppConfig`, `ResilienceConfig`                                              | Container builds `ChatClient`, circuit-breaker registry, resilient tool provider                                                        |
| **Builder**                 | `ChatClient.builder(...)`, `SyncMcpToolCallbackProvider.builder()`                              | Stepwise construction of the chat pipeline                                                                                              |
| **Chain of Responsibility** | `RequestContextFilter` → rate limiter → controller                                              | Each link authenticates/limits or passes the request on                                                                                 |
| **Interpreter** (framework) | `PromptTemplate` rendering `prompts/system.st`                                                  | StringTemplate grammar is parsed and evaluated to produce the system prompt                                                             |
| **Singleton**               | All Spring beans                                                                                | One shared, stateless instance per container                                                                                            |

## Semantic Tool Selection (Redis)

With many MCP servers each exposing dozens of tools, stuffing every tool definition into every LLM
call would blow past the context window and degrade response quality. Instead, `llm-mcp-client` uses
a **vector-similarity search** to select only the tools most relevant to the user's current query:

```
Startup                                  Per request
───────────────────────────────────────  ───────────────────────────────────────────────────────
ToolCallbackProvider                     user query
        │                                       │
        │  all tool definitions                 │ embed (text-embedding-3-small)
        ▼                                       ▼
  ToolVectorIndex ──→ Redis vector store    Redis similarity search (HNSW / cosine, in-memory)
   (tool name + description + schema           │
    stored as documents, index tool_embeddings)│ top-K matching tool names
                                               ▼
                                       SemanticToolSelector
                                               │ filter full ToolCallback[] down to matched subset
                                               ▼
                                       ChatClient.toolCallbacks(selected)
                                               │
                                               ▼
                                           LLM call (only K tools in context)
```

> **Backend history:** this index was originally backed by pgvector (Postgres + the `vector`
> extension). It was switched to Redis so tool retrieval — which runs on *every* chat turn — is an
> in-memory lookup instead of a round-trip to Postgres. Postgres is still used by `llm-mcp-client`,
> but only for `PostgresConversationStore` (chat-memory persistence), which is unrelated to tool
> selection. `ToolVectorIndex` and `SemanticToolSelector` depend only on the generic Spring AI
> `VectorStore`/`Document`/`SearchRequest` API, so swapping the backend required no code changes —
> only the Maven dependency and `application.yaml` config changed.

> **Java 25 / Spring Boot 4.1 boot fix:** this module pinned `springdoc-openapi-starter-webmvc-ui`
> at `2.8.9`, which predates Spring Boot 4.1's Spring Data repackaging (`TypeInformation` moved from
> `...data.util` to `...data.core` in `spring-data-commons:4.1.0`). Because this is the only `llm-mcp`
> module combining JPA + springdoc, it was the only one to hit `NoClassDefFoundError` at startup —
> before ever touching Redis or Postgres. Bumped to `springdoc-openapi-starter-webmvc-ui:3.0.3`
> (current release, built against Spring Boot 4.x) and confirmed the service now boots past that
> point cleanly: Tomcat starts, Hibernate/JPA and Spring Data Redis repository scanning succeed, and
> it only fails on `Connection to localhost:5432 refused` — the expected "good failure" when no
> Postgres is running locally.

### Key components

| Class                                                                          | Role                                                                                                                                                                                       |
|--------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ToolVectorIndex`                                                              | `@EventListener(ApplicationReadyEvent)` — on startup, embeds every `ToolCallback`'s name + description + input schema as a `Document` in the `tool_embeddings` Redis index                 |
| `SemanticToolSelector`                                                         | Per-request: embeds the user query, calls `VectorStore.similaritySearch(topK)`, maps hit names back to `ToolCallback` objects; falls back to all tools if the vector store returns nothing |
| `RedisVectorStore` (auto-configured by `spring-ai-starter-vector-store-redis`) | HNSW index (RediSearch), 1536-dimension OpenAI `text-embedding-3-small` embeddings, index name `tool_embeddings`                                                                           |

### Configuration

| Property                                        | Default                  | Description                                                          |
|-------------------------------------------------|--------------------------|----------------------------------------------------------------------|
| `assistant.tool-selector.top-k`                 | `10`                     | Number of tools retrieved per query                                  |
| `spring.ai.openai.embedding.options.model`      | `text-embedding-3-small` | Embedding model for tool + query vectors                             |
| `spring.ai.vectorstore.redis.initialize-schema` | `true`                   | Auto-creates the RediSearch index at startup                         |
| `spring.ai.vectorstore.redis.index-name`        | `tool_embeddings`        | Name of the RediSearch index (equivalent to pgvector's `table-name`) |
| `spring.ai.vectorstore.redis.prefix`            | `tool_embeddings:`       | Key prefix for documents stored in the index                         |
| `spring.data.redis.host` / `REDIS_HOST`         | `localhost`              | Redis connection host                                                |
| `spring.data.redis.port` / `REDIS_PORT`         | `6379`                   | Redis connection port                                                |

The Redis store's HNSW graph parameters (`spring.ai.vectorstore.redis.hnsw.m` /
`ef-construction` / `ef-runtime`) are available but left at their library defaults; there is no
direct Redis equivalent of pgvector's `dimensions`/`distance-type` properties — the embedding
dimension is inferred from the embedding model, and RediSearch's vector field defaults to cosine
distance, matching the previous pgvector configuration. The root `docker-compose.yml` provides a
`redis/redis-stack-server` service (`redis`, port 6379, AOF persistence, RediSearch included) for this purpose. Postgres
(`pgvector/pgvector:pg18`) is still used for chat-memory persistence. Its `vector` extension is no
longer needed by the (now Redis-backed) tool index; the existing
`V2__enable_pgvector_extension.sql` Flyway migration is a harmless no-op
(`CREATE EXTENSION IF NOT EXISTS vector`) against the chat-memory database and was not touched,
since rewriting applied Flyway history is out of scope for this change.

---

## Configuration

| Property / Env Var                             | Default                                                                                                         | Description                                                             |
|------------------------------------------------|-----------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------|
| `DB_URL`                                       | `jdbc:postgresql://localhost:5432/spring_ai`                                                                    | PostgreSQL JDBC URL for conversation memory                             |
| `DB_USERNAME`                                  | `postgres`                                                                                                      | DB username                                                             |
| `DB_PASSWORD`                                  | `postgres`                                                                                                      | DB password                                                             |
| `OPENAI_API_KEY` (`spring.ai.openai.api-key`)  | *(required)*                                                                                                    | OpenAI API key for the chat model                                       |
| `MCP_AUTH_TOKEN` (`assistant.mcp-auth-token`)  | *(empty)*                                                                                                       | Shared bearer token attached to every outbound MCP call                 |
| `assistant.name`                               | `Enterprise AI Assistant`                                                                                       | Assistant persona name, rendered into the system prompt                 |
| `assistant.default-user`                       | `himansu.nayak`                                                                                                 | Fallback acting/conversation user when `X-User-Id` is absent            |
| `assistant.memory-window`                      | `20`                                                                                                            | Number of past messages replayed as conversation history per turn       |
| `assistant.max-tool-iterations`                | `5`                                                                                                             | Hard cap on tool-execution rounds per chat request                      |
| `assistant.max-tool-result-chars`              | `8000`                                                                                                          | Max characters of a single tool result fed back into the model          |
| `assistant.rate-limit-per-minute`              | `30`                                                                                                            | Per-user `/chat` requests allowed per minute                            |
| `assistant.sensitive-words`                    | `[]`                                                                                                            | Words that, if present in a prompt, should be blocked                   |
| `assistant.write-tool-keywords`                | `[apply, create, update, delete, send, deploy, trigger, rollback, cancel, remove, approve, assign, reschedule]` | Tool-name substrings treated as write/destructive                       |
| `assistant.tool-selector.top-k`                | `10`                                                                                                            | Number of semantically relevant tools retrieved from pgvector per query |
| `mcp.client.streamable-http.connections.*.url` | *(see table above)*                                                                                             | Per-server downstream MCP base URLs                                     |
| `OTEL_EXPORTER_OTLP_ENDPOINT`                  | `http://localhost:4318`                                                                                         | OTLP traces endpoint (Tempo)                                            |
| `TRACING_SAMPLING`                             | `1.0`                                                                                                           | Trace sampling probability                                              |

---

## Running in Isolation

This module depends only on **PostgreSQL** (for conversation memory) and the **OpenAI API** — none of the
downstream MCP servers are required to start the app, though tool calls will fail/circuit-break if they're
unreachable.

The root `docker-compose.yml` brings up the module's dependencies — **PostgreSQL**, **Redis**, **Prometheus**, and
**Grafana** (provisioned with the dashboards under `observability/`) — so you can run the app itself on the
host with hot reload (`./mvnw spring-boot:run`) while everything it needs lives in containers. Name the
services explicitly — a bare `docker compose up -d` resolves the root compose file and starts the whole stack:

```bash
cd llm-mcp-client
docker compose up -d postgres redis prometheus grafana   # :5432, :6379, :9090, :3000 (admin/admin)
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
