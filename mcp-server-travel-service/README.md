# Travel Service — `mcp-server-travel-service`

An MCP server that exposes flight-availability lookups via the **Amadeus** self-service API. Runs on **`:8086`**
locally / **`:8087`** in the root compose stack (see [`docker-compose.yml`](../docker-compose.yml#L142)), MCP
protocol **STATELESS**, no datasource — Spring app name `mcp-travel-service`.

---

## MCP Tools

Defined in `FlightMcpTools` (registered via `MethodToolCallbackProvider` in `McpToolConfig`):

| Tool name        | Type | Description                                                                                       |
|------------------|------|---------------------------------------------------------------------------------------------------|
| `searchFlights`  | READ | Search flights between two IATA airport codes for a date — returns airline, times, duration, stops, price, seat availability. Args: `originCode`, `destinationCode`, `departureDate` (yyyy-MM-dd), `adults` (1–9), `maxResults` (1–20, capped server-side) |
| `getAirportInfo` | READ | Static lookup of common city/airport names → IATA codes (Dublin=DUB, Munich=MUC, …) to help the model pick the right code before calling `searchFlights` |

---

## Best Practices Applied

| Practice                       | Status | Notes                                                                                                          |
|--------------------------------|--------|----------------------------------------------------------------------------------------------------------------|
| Centralised error handling     | ✅      | `GlobalExceptionHandler` (`@RestControllerAdvice`) — uniform `{status, error, message, details, timestamp}` body |
| Meaningful 404s                | ✅      | `ResourceNotFoundException` for unresolvable routes/dates from the Amadeus API                                 |
| Input validation               | ✅      | Blank/date-format/range checks (`adults` 1–9, `maxResults` clamped 1–20) → `IllegalArgumentException` → HTTP 400 |
| Bearer token auth              | ✅      | `McpAuthFilter` validates `Authorization: Bearer <mcp.security.token>`; logs `WARN` and runs in insecure dev mode if unset |
| Acting-user propagation        | ✅      | `X-Acting-User` header → `ActingUserContext` thread-local, defaults to `mcp.security.default-user`             |
| Rate limiting                  | ✅      | In-memory per-user fixed-window limiter (`RateLimiter`, default 120 req/min) → HTTP 429                        |
| Audit logging                  | ✅      | `[AUDIT] tool=searchFlights actingUser=… origin=… destination=… date=… adults=… outcome=success|failure:<Exception> latencyMs=…` |
| Output truncation              | ✅      | `ToolOutputUtil.cap` truncates flight-offer JSON beyond `mcp.output.max-chars` (`McpOutputProperties`, default 8 000) |
| OAuth2 token caching           | ✅      | `AmadeusTokenService` caches the Amadeus client-credentials token and refreshes it 60 s before expiry under a `ReentrantLock`, avoiding a token round-trip on every search |
| HTTP client timeouts           | ✅      | `amadeus.timeout-seconds` (default 10s) bounds connect+read time on `RestClientConfig`'s `amadeusRestClient` |
| Graceful shutdown              | ✅      | `server.shutdown: graceful` + `spring.lifecycle.timeout-per-shutdown-phase: 30s`                               |
| Externalised config            | ✅      | `AmadeusProperties` / `SecurityProperties` / `McpOutputProperties` (`@ConfigurationProperties`) — base URL, credentials, timeouts, tokens, limits all env-overridable |
| Structured logging             | ✅      | SLF4J/Lombok `@Slf4j`, application-tagged via `spring.application.name`                                        |
| Distributed tracing            | ✅      | Micrometer Tracing → OTLP (`OTEL_EXPORTER_OTLP_ENDPOINT`) → Grafana Tempo                                       |
| Prometheus metrics + SLOs      | ✅      | `micrometer-registry-prometheus` with `http.server.requests` percentile histograms and explicit SLO buckets (50ms…5s) |
| Liveness/readiness probes      | ✅      | `management.endpoint.health.probes.enabled: true`                                                              |
| Health/auth allow-list         | ✅      | `/actuator/health` and `/actuator/info` are exempt from auth + rate limiting                                   |
| Non-root container             | ✅      | Multi-stage Dockerfile runs as a dedicated system user on a `jre`-only runtime image                           |
| Write-operation gating         | ➖      | N/A — both tools are read-only (no `enforceWriteGate` needed)                                                  |
| Circuit breaker / resilience   | ❌      | No Resilience4j — Amadeus API failures (including rate limits / sandbox quotas) surface directly as tool errors |

---

## Configuration

| Property / Env Var                          | Default                          | Description                                              |
|-----------------------------------------------|----------------------------------|----------------------------------------------------------|
| `SERVER_PORT`                                  | `8086`                           | HTTP port (root compose maps it to `8087`)               |
| `AMADEUS_CLIENT_ID` (`amadeus.client-id`)      | *(empty → 401 from Amadeus)*     | Amadeus self-service API client id                       |
| `AMADEUS_CLIENT_SECRET` (`amadeus.client-secret`) | *(empty)*                     | Amadeus self-service API client secret                   |
| `AMADEUS_BASE_URL` (`amadeus.base-url`)        | `https://test.api.amadeus.com`   | Amadeus API base URL (test vs production)                |
| `AMADEUS_TIMEOUT_SECONDS` (`amadeus.timeout-seconds`) | `10`                      | Connect+read timeout for Amadeus HTTP calls              |
| `MCP_AUTH_TOKEN` (`mcp.security.token`)        | *(empty → insecure dev mode)*    | Shared bearer token required from MCP clients            |
| `mcp.security.default-user`                    | `system`                         | Fallback acting user when `X-Acting-User` is absent      |
| `mcp.security.rate-limit-per-minute`           | `120`                            | Per-user fixed-window request cap                        |
| `mcp.output.max-chars`                         | `8000`                           | Max characters returned per tool before truncation       |
| `OTEL_EXPORTER_OTLP_ENDPOINT`                  | `http://localhost:4318`          | OTLP traces endpoint (Tempo)                             |
| `TRACING_SAMPLING`                             | `1.0`                            | Trace sampling probability                               |

---

## Running in Isolation

```bash
cd mcp-server-travel-service
export AMADEUS_CLIENT_ID=xxxx
export AMADEUS_CLIENT_SECRET=xxxx
export MCP_AUTH_TOKEN=$(uuidgen)
./mvnw spring-boot:run                         # :8086
```

Or via the bundled `docker-compose.yml` (single-service + healthcheck + resource limits):

```bash
AMADEUS_CLIENT_ID=xxxx AMADEUS_CLIENT_SECRET=xxxx docker compose up
```

> Get free sandbox credentials at the [Amadeus for Developers](https://developers.amadeus.com) self-service portal —
> the default `AMADEUS_BASE_URL` points at the `test.api.amadeus.com` sandbox.

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

### Look up an airport code

```bash
curl -s http://localhost:8086/mcp \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
        "jsonrpc":"2.0","id":2,"method":"tools/call",
        "params":{"name":"getAirportInfo","arguments":{"cityOrAirport":"Dublin"}}
      }'
```

### Search flights

```bash
curl -s http://localhost:8086/mcp \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
        "jsonrpc":"2.0","id":3,"method":"tools/call",
        "params":{"name":"searchFlights","arguments":{
          "originCode":"DUB","destinationCode":"MUC",
          "departureDate":"2026-08-15","adults":1,"maxResults":5
        }}
      }'
```

### Actuator

```bash
curl -s http://localhost:8086/actuator/health | jq
curl -s http://localhost:8086/actuator/prometheus | head -40
```
