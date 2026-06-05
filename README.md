# Spring AI MCP — Org Enterprise Assistant

A multi-module Spring AI **Model Context Protocol (MCP)** demo. A central chat assistant (the MCP *client*) orchestrates
four domain MCP *servers* (HR, Ticketing, Deployment, Notification), each backed by PostgreSQL and exposing both a REST
API and MCP tools/prompts.

```
                    ┌─────────────────────────────────┐
                    │         llm-mcp-client           │   POST /chat
                    │  Spring AI ChatClient (OpenAI)   │
                    └──────────────┬──────────────────┘
                                   │ MCP (Streamable HTTP)
        ┌──────────────┬───────────┼───────────────┬──────────────┐
        ▼              ▼           ▼                ▼
┌──────────────┐ ┌───────────┐ ┌────────────────┐ ┌──────────────────┐
│  hr-service  │ │  ticket-  │ │  deployment-   │ │  notification-   │
│   :8084      │ │  service  │ │   service      │ │   service        │
│              │ │   :8081   │ │    :8082       │ │    :8083         │
└──────┬───────┘ └─────┬─────┘ └───────┬────────┘ └────────┬─────────┘
       │               │               │                   │
       └───────────────┴───────────────┴───────────────────┘
                               ▼
                     PostgreSQL (spring_ai DB)
                  (separate Flyway schema per service)
```

---

## Modules

| Directory                         | Port | Role       | Spring App Name        |
|-----------------------------------|------|------------|------------------------|
| `llm-mcp-client`                  | 8080 | MCP client | `ai-mcp-server`        |
| `mcp-server-ticket-service`       | 8081 | MCP server | `ticket-service`       |
| `mcp-server-deployment-service`   | 8082 | MCP server | `deployment-service`   |
| `mcp-server-notification-service` | 8083 | MCP server | `notification-service` |
| `mcp-server-hr-service`           | 8084 | MCP server | `mcp-hr-service`       |

---

## Tech Stack

All modules share the same stack:

| Concern       | Technology                                                                    |
|---------------|-------------------------------------------------------------------------------|
| Language      | Java 21                                                                       |
| Framework     | Spring Boot 4.0.3                                                             |
| Web           | Spring MVC                                                                    |
| AI / MCP      | Spring AI 2.0.0-M8 (MCP server + client)                                      |
| Persistence   | Spring Data JPA + PostgreSQL + Flyway                                         |
| Validation    | Jakarta Bean Validation                                                       |
| Observability | Spring Boot Actuator + Micrometer + Prometheus + OTLP Tracing → Grafana Tempo |
| Build         | Maven (each module has its own `./mvnw` wrapper)                              |

---

## Shared Database

All services connect to the **same** PostgreSQL instance (`spring_ai` database by default). Isolation is achieved via
separate Flyway schema-history tables per service:

| Service              | Flyway history table                 |
|----------------------|--------------------------------------|
| hr-service           | `flyway_schema_history_hr`           |
| ticket-service       | `flyway_schema_history_ticket`       |
| deployment-service   | `flyway_schema_history_deployment`   |
| notification-service | `flyway_schema_history_notification` |

---

## Running

### 1. Start infrastructure

```bash
# PostgreSQL + Prometheus + Tempo + Grafana
docker compose up -d
```

### 2. Start MCP servers (separate terminals)

```bash
cd mcp-server-hr-service && ./mvnw spring-boot:run           # :8084
cd mcp-server-ticket-service && ./mvnw spring-boot:run       # :8081
cd mcp-server-deployment-service && ./mvnw spring-boot:run   # :8082
cd mcp-server-notification-service && ./mvnw spring-boot:run # :8083
```

### 3. Start MCP client

```bash
export OPENAI_API_KEY=sk-...
cd llm-mcp-client && ./mvnw spring-boot:run                  # :8080
```

### 4. Send a chat request

```bash
curl -s localhost:8080/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"Who can cover for john.doe tomorrow?"}'
```

A message beginning with `/` (e.g. `/analyze-tickets`) is treated as an MCP prompt name and expanded into the
corresponding pre-defined prompt before being sent to the model.

---

## MCP Client — `llm-mcp-client`

The orchestrating chat assistant. It has **no datasource** — it proxies user messages to OpenAI and dispatches tool
calls to the downstream MCP servers over Streamable HTTP.

### REST API

| Method | Path    | Body                 | Description                     |
|--------|---------|----------------------|---------------------------------|
| `POST` | `/chat` | `{"message": "..."}` | Send a message to the assistant |

### MCP server connections

| Server         | URL                     |
|----------------|-------------------------|
| `hr`           | `http://localhost:8084` |
| `ticket`       | `http://localhost:8081` |
| `deployment`   | `http://localhost:8082` |
| `notification` | `http://localhost:8083` |

### `assistant.*` configuration properties

| Property                          | Default                   | Description                                               |
|-----------------------------------|---------------------------|-----------------------------------------------------------|
| `assistant.name`                  | `Enterprise AI Assistant` | Display name / persona                                    |
| `assistant.default-user`          | `john.doe`                | Acting user when no authenticated principal is present    |
| `assistant.mcp-auth-token`        | *(env: MCP_AUTH_TOKEN)*   | Token forwarded to MCP servers as `Bearer` header         |
| `assistant.memory-window`         | `20`                      | Number of conversation turns kept in context              |
| `assistant.max-tool-iterations`   | `5`                       | Max tool-call rounds per chat turn before forcing a reply |
| `assistant.max-tool-result-chars` | `8000`                    | Tool result strings are truncated beyond this length      |
| `assistant.rate-limit-per-minute` | `30`                      | Per-user request cap                                      |
| `assistant.write-tool-keywords`   | apply, create, update, …  | Keywords that classify a tool call as a write operation   |
| `assistant.sensitive-words`       | *(empty)*                 | Words that are masked before being sent to the model      |

---

## HR Service — `mcp-server-hr-service` (:8084)

Manages employee leave and replacement lookups.

### MCP Tools

| Tool name         | Description                                                     |
|-------------------|-----------------------------------------------------------------|
| `applyLeave`      | Apply leave for a user on a specific ISO-8601 date (yyyy-MM-dd) |
| `findReplacement` | Find a replacement employee for a user on a specific date       |

### REST API

| Method | Path                         | Description                      |
|--------|------------------------------|----------------------------------|
| `POST` | `/hr/leave`                  | Apply for leave                  |
| `GET`  | `/hr/leave/{username}`       | Check whether a user is on leave |
| `GET`  | `/hr/replacement/{username}` | Find an available replacement    |

### Environment Variables

| Variable         | Default                                      |
|------------------|----------------------------------------------|
| `SERVER_PORT`    | `8084`                                       |
| `DB_URL`         | `jdbc:postgresql://localhost:5432/spring_ai` |
| `DB_USERNAME`    | `postgres`                                   |
| `DB_PASSWORD`    | `postgres`                                   |
| `MCP_AUTH_TOKEN` | *(empty → insecure dev mode)*                |

### `mcp.security.*` properties

| Property                               | Default   | Description                                              |
|----------------------------------------|-----------|----------------------------------------------------------|
| `mcp.security.token`                   | *(empty)* | Shared bearer token; blank = auth disabled (WARN logged) |
| `mcp.security.default-user`            | `system`  | Fallback acting-user when `X-Acting-User` is absent      |
| `mcp.security.require-user-for-writes` | `false`   | Reject write tools if acting user is the default         |
| `mcp.security.rate-limit-per-minute`   | `120`     | Fixed-window per-user request cap                        |
| `mcp.output.max-chars`                 | `8000`    | Tool response max chars (excess truncated)               |

---

## Ticket Service — `mcp-server-ticket-service` (:8081)

Manages support tickets. Exposes an `analyze-tickets` MCP prompt in addition to tools.

### MCP Tools & Prompts

| Name                 | Type   | Description                                              |
|----------------------|--------|----------------------------------------------------------|
| `createTicket`       | Tool   | Create a ticket (title, description, priority, assignee) |
| `getTickets`         | Tool   | List all tickets                                         |
| `getTicket`          | Tool   | Get a ticket by id                                       |
| `updateTicketStatus` | Tool   | Update a ticket's status                                 |
| `assignTicket`       | Tool   | Assign a ticket to an employee                           |
| `analyze-tickets`    | Prompt | Returns a pre-built prompt summarising open ticket load  |

### REST API

| Method | Path                   | Description              |
|--------|------------------------|--------------------------|
| `POST` | `/tickets`             | Create a ticket          |
| `GET`  | `/tickets`             | List all tickets         |
| `GET`  | `/tickets/{id}`        | Get a ticket by id       |
| `PUT`  | `/tickets/{id}/status` | Update a ticket's status |
| `PUT`  | `/tickets/{id}/assign` | Assign a ticket          |

### Environment Variables

| Variable         | Default                                      |
|------------------|----------------------------------------------|
| `SERVER_PORT`    | `8081`                                       |
| `DB_URL`         | `jdbc:postgresql://localhost:5432/spring_ai` |
| `DB_USERNAME`    | `postgres`                                   |
| `DB_PASSWORD`    | `postgres`                                   |
| `MCP_AUTH_TOKEN` | *(empty → insecure dev mode)*                |

`mcp.security.*` properties are the same as HR Service (no `mcp.output.max-chars`).

---

## Deployment Service — `mcp-server-deployment-service` (:8082)

Manages deployment scheduling. Uses **STREAMABLE** MCP protocol (others use STATELESS).

### MCP Tools

| Tool name              | Description                                                       |
|------------------------|-------------------------------------------------------------------|
| `getDeployments`       | Get all deployments                                               |
| `getDeployment`        | Get a deployment by its id                                        |
| `createDeployment`     | Schedule a new deployment (service, environment, datetime, owner) |
| `assignOwner`          | Assign a new owner to an existing deployment                      |
| `rescheduleDeployment` | Reschedule a deployment to a new ISO datetime                     |
| `cancelDeployment`     | Cancel a deployment by id                                         |

### REST API

| Method | Path                           | Description           |
|--------|--------------------------------|-----------------------|
| `GET`  | `/deployments`                 | List all deployments  |
| `GET`  | `/deployments/{id}`            | Get a deployment      |
| `POST` | `/deployments`                 | Schedule a deployment |
| `PUT`  | `/deployments/{id}/assign`     | Reassign owner        |
| `PUT`  | `/deployments/{id}/reschedule` | Reschedule            |
| `PUT`  | `/deployments/{id}/cancel`     | Cancel                |

### Environment Variables

| Variable         | Default                                      |
|------------------|----------------------------------------------|
| `SERVER_PORT`    | `8082`                                       |
| `DB_URL`         | `jdbc:postgresql://localhost:5432/spring_ai` |
| `DB_USERNAME`    | `postgres`                                   |
| `DB_PASSWORD`    | `postgres`                                   |
| `MCP_AUTH_TOKEN` | *(empty → insecure dev mode)*                |

`mcp.security.*` properties are the same as HR Service.

---

## Notification Service — `mcp-server-notification-service` (:8083)

Sends and lists notifications across channels (INTERNAL, EMAIL, SLACK).

### MCP Tools

| Tool name          | Description                                       |
|--------------------|---------------------------------------------------|
| `getNotifications` | Get all notifications                             |
| `sendNotification` | Send a notification (channel, recipient, message) |

### REST API

| Method | Path             | Description                                             |
|--------|------------------|---------------------------------------------------------|
| `POST` | `/notifications` | Send a notification (`channel`, `recipient`, `message`) |
| `GET`  | `/notifications` | List all notifications                                  |

### Environment Variables

| Variable         | Default                                      |
|------------------|----------------------------------------------|
| `SERVER_PORT`    | `8083`                                       |
| `DB_URL`         | `jdbc:postgresql://localhost:5432/spring_ai` |
| `DB_USERNAME`    | `postgres`                                   |
| `DB_PASSWORD`    | `postgres`                                   |
| `MCP_AUTH_TOKEN` | *(empty → insecure dev mode)*                |

`mcp.security.*` properties are the same as HR Service (auth implementation present but not yet wired into
`application.yaml` — set `MCP_AUTH_TOKEN` env var to enable).

---

## Security & Operations (MCP Servers)

All four servers share the same security model:

### Bearer Token Authentication

Set `MCP_AUTH_TOKEN` to enable. Without it the service logs a `WARN` at startup and runs in **insecure dev mode**.

| Header                          | Required when token set | Purpose                                                                 |
|---------------------------------|-------------------------|-------------------------------------------------------------------------|
| `Authorization: Bearer <token>` | Yes                     | Authenticates the caller (client or human)                              |
| `X-Acting-User`                 | Optional                | Identity of the acting human. Falls back to `mcp.security.default-user` |

Error responses: `401 Unauthorized`, `429 Too Many Requests`.

`/actuator/health` and `/actuator/info` are always exempt from auth and rate limiting.

### Rate Limiting

In-memory per-user fixed-window rate limiter (default 120 req/min). Returns `429` when exceeded.

### Audit Logging

Every MCP tool invocation emits a structured `INFO` log line:

```
INFO  AUDIT createDeployment | user=jane serviceName=payments environment=PROD scheduledTime=2025-06-01T14:00:00 outcome=SUCCESS latencyMs=23
```

Fields: tool name, acting user, sanitised arguments, outcome (SUCCESS / ERROR), latency in ms.

### Actuator Endpoints

MCP servers expose: `health`, `info`, `metrics`, `prometheus`  
The client additionally exposes: `loggers`, `env`

Liveness/readiness probes are enabled on HR and Deployment services (`management.endpoint.health.probes.enabled: true`).

---

## Observability

Full setup in [OBSERVABILITY.md](OBSERVABILITY.md).

`docker compose up -d` starts:

- **Prometheus** — `:9090`
- **Grafana Tempo** — `:4318` (OTLP in), `:3200` (query)
- **Grafana** — `:3000` (admin/admin)

**Grafana dashboard:** *"Org MCP — Client & Servers Overview"* auto-loads with a `Service` dropdown (multi-select +
*All*). Panels: up status, HTTP rate/p95 latency/5xx, JVM heap, CPU, threads, DB connections, GC pause.

**Distributed tracing:** Trace IDs propagate from the client through each MCP server tool call and are viewable in
Grafana → Explore → Tempo.

**Token usage:** OpenAI token consumption is **not yet tracked** as a Prometheus metric. To add it, wire a
`ChatResponseMetadata` listener in `llm-mcp-client` that reads `response.getMetadata().getUsage()` and records
prompt/completion tokens via a `MeterRegistry` counter tagged with the acting user. Per-server attribution is not
directly available (tokens are consumed in a single OpenAI call at the client); you can approximate it by tracking which
tools were called in each turn.

---

## Best Practices Applied

| Practice                        | Status | Notes                                                                                                                                   |
|---------------------------------|--------|-----------------------------------------------------------------------------------------------------------------------------------------|
| Centralised error handling      | ✅      | `GlobalExceptionHandler` (`@RestControllerAdvice`) in every module — consistent `{status, error, message, details, timestamp}` envelope |
| Meaningful 404s                 | ✅      | `ResourceNotFoundException` → HTTP 404 on all domain lookups                                                                            |
| Input validation                | ✅      | Jakarta constraints (`@NotBlank`, `@NotNull`, `@Positive`, `@Valid`) on all controllers; violations → HTTP 400                          |
| Bearer token auth               | ✅      | `McpAuthFilter` in all servers; insecure-dev-mode warning when unset                                                                    |
| Acting-user propagation         | ✅      | `X-Acting-User` header forwarded from client to server; stored in `ActingUserContext` thread-local                                      |
| Rate limiting                   | ✅      | Per-user fixed-window (120 req/min on servers, 30 req/min on client)                                                                    |
| Audit logging                   | ✅      | Structured `AUDIT` log lines on every tool invocation (hr, ticket, deployment; notification has filter, verify logging)                 |
| Output truncation               | ✅      | `mcp.output.max-chars: 8000` on servers; `assistant.max-tool-result-chars: 8000` on client                                              |
| Memory window                   | ✅      | `assistant.memory-window: 20` — caps conversation context sent to OpenAI                                                                |
| Max tool iterations             | ✅      | `assistant.max-tool-iterations: 5` — prevents infinite tool-call loops                                                                  |
| Write-tool classification       | ✅      | `assistant.write-tool-keywords` list allows the client to identify and guard write operations                                           |
| Externalised config             | ✅      | All ports, credentials, and tokens are environment-variable-overridable with safe local defaults                                        |
| Typed configuration             | ✅      | `@ConfigurationProperties` beans (`AssistantProperties`, `SecurityProperties`) — no hardcoded values                                    |
| Structured logging              | ✅      | `logback-spring.xml` with application-tagged console pattern in every module                                                            |
| Distributed tracing             | ✅      | Micrometer Tracing → OTLP → Grafana Tempo; trace IDs propagate client → server                                                          |
| Prometheus metrics              | ✅      | Micrometer + Prometheus registry; application-tagged; scraped by Grafana dashboard                                                      |
| Liveness/readiness probes       | ✅      | Enabled on HR and Deployment services; all services expose `/actuator/health`                                                           |
| MCP prompts                     | ✅      | Ticket service exposes `analyze-tickets` prompt; client expands `/promptName` shorthand                                                 |
| Token usage metering            | ❌      | Not yet implemented — see Observability section for how to add it                                                                       |
| Circuit breaker / resilience    | ❌      | No Resilience4j or similar; client will surface MCP server failures directly to the caller                                              |
| Persistent conversation storage | ❌      | Conversation history is in-memory only; restarts lose context                                                                           |
