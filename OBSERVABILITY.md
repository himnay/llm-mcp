# <span style="color:hsl(359,68%,44%)">Observability Setup — Metrics & Distributed Tracing</span>

Every service is instrumented for **metrics** (Micrometer → Prometheus) and **distributed tracing** (Micrometer
Tracing → OTLP → Grafana Tempo). The `docker-compose.yml` at the repo root provides PostgreSQL plus the full
observability backend.

## <span style="color:hsl(59,68%,32%)">Stack</span>

| Component  | Purpose                                    | URL                                 |
|------------|--------------------------------------------|-------------------------------------|
| PostgreSQL | App databases (auto-created on first run)  | `localhost:5432`                    |
| Prometheus | Scrapes `/actuator/prometheus` per service | http://localhost:9090               |
| Tempo      | Receives OTLP traces (HTTP `:4318`)        | http://localhost:3200               |
| Grafana    | Dashboards + Tempo/Prometheus datasources  | http://localhost:3000 (admin/admin) |

## <span style="color:hsl(119,68%,32%)">Quick start</span>

```bash
# 1. Start Postgres + observability backend
docker compose up -d

# 2. Run each service (separate terminals)
cd ticket-service     && ./mvnw spring-boot:run   # :8081
cd deployment-service && ./mvnw spring-boot:run   # :8082
cd notification-service && ./mvnw spring-boot:run # :8083
cd hr-service         && ./mvnw spring-boot:run   # :8084
cd ai-assistant-mcp            && OPENAI_API_KEY=sk-... ./mvnw spring-boot:run  # :8080
```

## <span style="color:hsl(179,68%,36%)">How tracing is wired</span>

Each service's `application.yaml`:

```yaml
management:
  tracing:
    sampling:
      probability: ${TRACING_SAMPLING:1.0}        # sample 100% in dev
  otlp:
    tracing:
      endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318}/v1/traces
```

Spans are exported over OTLP/HTTP to Tempo (`:4318`). A request that flows from
`ai-assistant-mcp` into an MCP server propagates the same trace ID, so you can
follow a single chat turn across services in Grafana → Explore → **Tempo**.

Metrics are exposed at `/actuator/prometheus` and scraped per
`observability/prometheus.yml`. View them in Grafana → Explore → **Prometheus**,
or browse targets at http://localhost:9090/targets.

## <span style="color:hsl(239,68%,44%)">Endpoints exposed per service</span>

`health`, `info`, `metrics`, `prometheus`, `loggers`, `env` — e.g.
`GET http://localhost:8082/actuator/health`.

## <span style="color:hsl(299,68%,44%)">Overriding for a real collector</span>

Point every service at a shared collector without editing YAML:

```bash
export OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4318
export TRACING_SAMPLING=0.1     # sample 10% in production
```
