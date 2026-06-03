# Spring AI MCP — Org Enterprise Assistant

A multi-module Spring AI **Model Context Protocol (MCP)** demo. A central chat assistant (the MCP *client*) orchestrates four domain MCP *servers* (HR, ticketing, deployment, notification), each backed by PostgreSQL and exposing both a REST API and MCP tools/prompts.

```
                    ┌─────────────────────────────┐
                    │      ai-assistant-mcp        │   MCP client + /chat REST
                    │  Spring AI ChatClient (OpenAI)│
                    └──────────────┬──────────────┘
                                   │ MCP (Streamable HTTP)
        ┌──────────────┬───────────┼───────────────┬──────────────┐
        ▼              ▼           ▼                ▼              
┌──────────────┐ ┌───────────┐ ┌────────────────┐ ┌──────────────────┐
│  hr-service  │ │  ticket-  │ │  deployment-   │ │  notification-   │
│   :8084      │ │  service  │ │   service      │ │   service        │
│              │ │   :8081   │ │    :8082       │ │    :8083         │
└──────┬───────┘ └─────┬─────┘ └───────┬────────┘ └────────┬─────────┘
       ▼               ▼               ▼                   ▼
   hr_db          ticket_db      deployment_db        notification_db
                         (PostgreSQL + Flyway)
```

## Modules

| Module                        | Port  | Role        | DB                |
|-------------------------------|-------|-------------|-------------------|
| `ai-assistant-mcp`            | 8080* | MCP client  | —                 |
| `ticket-service`     | 8081  | MCP server  | `ticket_db`       |
| `deployment-service` | 8082  | MCP server  | `deployment_db`   |
| `notification-service`| 8083 | MCP server  | `notification_db` |
| `hr-service`         | 8084  | MCP server  | `hr_db`           |

\* default Spring Boot port unless `SERVER_PORT` is set.

Each module has its own `README.md` with endpoint and configuration details.

## Tech Stack

- Java 17, Spring Boot (3.5.11 for the client + hr-service, 4.0.3 for the newer services)
- **Maven** build — each module has its own wrapper (`./mvnw`)
- Spring MVC, Spring Data JPA, PostgreSQL, Flyway
- Spring AI MCP (server `webmvc` + client), OpenAI chat model
- Jakarta Bean Validation
- Spring Boot Actuator + Micrometer / Prometheus (metrics) + Micrometer Tracing → OTLP → Grafana Tempo (traces)

## Best Practices Applied (this pass)

These production-grade practices — modelled on the `llm-gateway` project — were rolled out consistently across **all** modules:

| Practice | What it adds |
|----------|--------------|
| **Centralised error handling** | A `GlobalExceptionHandler` (`@RestControllerAdvice`) in every module returns a consistent JSON envelope `{status, error, message, details, timestamp}`. |
| **Meaningful 404s** | Domain lookups now raise `ResourceNotFoundException` → HTTP 404 instead of leaking a raw `RuntimeException` → 500. |
| **Input validation** | Controllers are `@Validated`; request params/bodies carry Jakarta constraints (`@NotBlank`, `@NotNull`, `@Positive`, `@Valid`). Violations → HTTP 400 with field-level details. |
| **Observability** | Actuator (`health`, `info`, `metrics`, `prometheus`, `loggers`, `env`), application-tagged Prometheus metrics, **and OTEL distributed tracing** exported over OTLP to Grafana Tempo (trace IDs propagate client → server). See [OBSERVABILITY.md](OBSERVABILITY.md). |
| **Externalised config** | Ports and datasource credentials are environment-overridable (`SERVER_PORT`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) with safe local defaults — no hardcoded secrets in YAML. |
| **Typed configuration** | `ai-assistant-mcp` replaces hardcoded values with a typed `@ConfigurationProperties` bean (`assistant.*`), wired with the configuration processor. |
| **Structured logging** | Each module ships a `logback-spring.xml` with an application-tagged console pattern, plus a `banner.txt`. |
| **Documentation** | Per-module `README.md` covering purpose, stack, run instructions, config, REST API, and applied practices. |

## Running

Bring up PostgreSQL (the four databases are auto-created) plus the observability backend, then run each module with its Maven wrapper:

```bash
# Postgres + Prometheus + Tempo + Grafana
docker compose up -d

# Each service (separate terminals)
cd ticket-service && ./mvnw spring-boot:run     # :8081
# ...deployment :8082, notification :8083, hr :8084
```

Flyway applies each service's schema and seed data on startup.

The assistant needs an OpenAI key:

```bash
export OPENAI_API_KEY=sk-...
```

Then send a chat request:

```bash
curl -s localhost:8080/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"Who can cover for john.doe tomorrow?"}'
```

## Observability

Full setup in [OBSERVABILITY.md](OBSERVABILITY.md). `docker compose up -d` starts Prometheus (`:9090`), Tempo (`:4318` OTLP in, `:3200` query), and Grafana (`:3000`, admin/admin).

**Single pane of glass:** Grafana auto-loads one dashboard — *"Org MCP — Client & Servers Overview"* — with a **`Service` dropdown** (multi-select + *All*) that switches every panel between the MCP client and the four servers. Panels: up status, HTTP rate / p95 latency / 5xx, JVM heap, CPU, threads, DB connections, GC pause. Traces for a chat turn propagate client → server and are viewable in Grafana → Explore → Tempo.

Every service also exposes `GET /actuator/{health,info,metrics,prometheus,loggers,env}`.
