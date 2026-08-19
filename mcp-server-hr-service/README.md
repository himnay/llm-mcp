# <span style="color:hsl(102,68%,32%)">HR Service — `mcp-server-hr-service`</span>

An MCP server that manages employee leave and replacement lookups, backed by PostgreSQL (Flyway-migrated). Runs
on **`:8084`**, MCP protocol **STATELESS**, Spring app name `mcp-hr-service`.

---

## <span style="color:hsl(135,68%,32%)">MCP Tools</span>

Defined in `HrMcpTools` as `@McpTool`-annotated methods, auto-registered by Spring AI's MCP annotation scanner
(`McpServerAnnotationScannerAutoConfiguration`) — there is no `McpToolConfig` bean and no
`MethodToolCallbackProvider`:

| Tool name         | Type  | Description                                                       |
|-------------------|-------|-------------------------------------------------------------------|
| `applyLeave`      | WRITE | Apply leave for a user on a specific ISO-8601 date (`yyyy-MM-dd`) |
| `findReplacement` | READ  | Find an available replacement employee for a user on a given date |

---

## <span style="color:hsl(167,68%,36%)">Best Practices Applied</span>

| Practice                     | Status | Notes                                                                                                                         |                                  |
|------------------------------|--------|-------------------------------------------------------------------------------------------------------------------------------|----------------------------------|
| Centralised error handling   | ✅      | `GlobalExceptionHandler` (`@RestControllerAdvice`) — uniform `{status, error, message, details, timestamp}` body              |                                  |
| Meaningful 404s              | ✅      | `ResourceNotFoundException` for unknown employees / leave records                                                             |                                  |
| Input validation             | ✅      | Blank/null + ISO-8601 date-format guards on every tool argument → `IllegalArgumentException` → HTTP 400                       |                                  |
| Bearer token auth            | ✅      | `McpAuthFilter` validates `Authorization: Bearer <mcp.security.token>`; logs `WARN` and runs in insecure dev mode if unset    |                                  |
| Acting-user propagation      | ✅      | `X-Acting-User` header → `ActingUserContext` thread-local, defaults to `mcp.security.default-user`                            |                                  |
| Write-operation gating       | ✅      | `applyLeave` rejects the default user when `mcp.security.require-user-for-writes=true`, requiring an explicit `X-Acting-User` |                                  |
| Rate limiting                | ✅      | In-memory per-user fixed-window limiter (`RateLimiter`, `RateLimiterConfig`, default 120 req/min) → HTTP 429                  |                                  |
| Audit logging                | ✅      | `[AUDIT] tool=applyLeave actingUser=… username=… date=… outcome=success                                                       | failure:<Exception> latencyMs=…` |
| Output truncation            | ✅      | `ToolOutputUtil.cap` truncates tool responses beyond `mcp.output.max-chars` (`McpOutputProperties`, default 8 000)            |                                  |
| Database migrations          | ✅      | Flyway with a dedicated history table (`flyway_schema_history_hr`) so multiple services can share one DB safely               |                                  |
| Query timeouts               | ✅      | `jpa.properties.jakarta.persistence.query.timeout: 5000` — bounds slow queries                                                |                                  |
| Connection pool tuning       | ✅      | HikariCP `connection-timeout: 10000`                                                                                          |                                  |
| Externalised config          | ✅      | `SecurityProperties` / `McpOutputProperties` (`@ConfigurationProperties`) — DB creds, tokens, limits all env-overridable      |                                  |
| Structured logging           | ✅      | SLF4J/Lombok `@Slf4j`, application-tagged via `spring.application.name`                                                       |                                  |
| Distributed tracing          | ✅      | Micrometer Tracing → OTLP (`OTEL_EXPORTER_OTLP_ENDPOINT`) → Grafana Tempo                                                     |                                  |
| Prometheus metrics           | ✅      | `micrometer-registry-prometheus`, scraped at `/actuator/prometheus`                                                           |                                  |
| Liveness/readiness probes    | ✅      | `management.endpoint.health.probes.enabled: true`                                                                             |                                  |
| Health/auth allow-list       | ✅      | `/actuator/health` and `/actuator/info` are exempt from auth + rate limiting                                                  |                                  |
| Non-root container           | ✅      | Multi-stage Dockerfile runs as a dedicated system user on a `jre`-only runtime image                                          |                                  |
| Circuit breaker / resilience | ❌      | No Resilience4j — DB/downstream failures surface directly as tool errors                                                      |                                  |

---

## <span style="color:hsl(200,68%,44%)">Design Patterns (GoF)</span>

| Pattern                     | Where                                                                                                                                              | Role                                                                                  |
|-----------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------|
| **Singleton**               | All Spring beans (`HRService`, `McpAuthFilter`, `RateLimiter`, properties)                                                                         | One shared, stateless instance per container                                          |
| **Facade**                  | `HRService`                                                                                                                                        | Single entry point hiding `EmployeeRepository` + `LeaveRecordRepository` coordination |
| **Factory Method**          | `@Bean` methods in `RateLimiterConfig`; `McpServerAnnotationScannerAutoConfiguration` builds each `@McpTool` method into a `SyncToolSpecification` | Container/framework builds and wires products (`RateLimiter`, tool registrations)     |
| **Proxy**                   | Spring Data JPA repositories, `@Transactional` AOP                                                                                                 | Interface-backed dynamic proxies add persistence/transaction behaviour                |
| **Template Method**         | `McpAuthFilter extends OncePerRequestFilter`                                                                                                       | Framework skeleton calls `doFilterInternal` / `shouldNotFilter` hooks                 |
| **Chain of Responsibility** | Servlet `FilterChain` (`McpAuthFilter` → MVC)                                                                                                      | Each filter handles or passes the request along                                       |
| **Command**                 | `@McpTool` methods (`applyLeave`, `findReplacement`) reified as MCP tool callbacks                                                                 | Tool invocations dispatched by name+arguments through the MCP runtime                 |

## <span style="color:hsl(233,68%,44%)">Configuration</span>

| Property / Env Var                      | Default                                      | Description                                           |
|-----------------------------------------|----------------------------------------------|-------------------------------------------------------|
| `SERVER_PORT`                           | `8084`                                       | HTTP port                                             |
| `DB_URL`                                | `jdbc:postgresql://localhost:5432/spring_ai` | PostgreSQL JDBC URL                                   |
| `DB_USERNAME`                           | `postgres`                                   | DB username                                           |
| `DB_PASSWORD`                           | `postgres`                                   | DB password                                           |
| `MCP_AUTH_TOKEN` (`mcp.security.token`) | *(empty → insecure dev mode)*                | Shared bearer token required from MCP clients         |
| `mcp.security.default-user`             | `system`                                     | Fallback acting user when `X-Acting-User` is absent   |
| `mcp.security.require-user-for-writes`  | `false`                                      | Reject `applyLeave` from the default user when `true` |
| `mcp.security.rate-limit-per-minute`    | `120`                                        | Per-user fixed-window request cap                     |
| `mcp.output.max-chars`                  | `8000`                                       | Max characters returned per tool before truncation    |
| `OTEL_EXPORTER_OTLP_ENDPOINT`           | `http://localhost:4318`                      | OTLP traces endpoint (Tempo)                          |
| `TRACING_SAMPLING`                      | `1.0`                                        | Trace sampling probability                            |

---

## <span style="color:hsl(266,68%,44%)">Running in Isolation</span>

```bash
cd mcp-server-hr-service
docker compose up -d         # starts a dedicated llm-postgres (:5432, db spring_ai)
export DB_URL=jdbc:postgresql://localhost:5432/spring_ai
export MCP_AUTH_TOKEN=$(uuidgen)
./mvnw spring-boot:run       # :8084
```

`docker-compose.yml` in this module brings up just its PostgreSQL dependency — no other MCP servers required.

---

## <span style="color:hsl(298,68%,44%)">curl Commands</span>

> MCP requests are JSON-RPC 2.0 over the streamable-HTTP endpoint `/mcp`. Replace `$TOKEN` with your
> `MCP_AUTH_TOKEN`.

### <span style="color:hsl(331,68%,44%)">List available tools</span>

```bash
curl -s http://localhost:8084/mcp \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'
```

### <span style="color:hsl(4,68%,44%)">Apply leave (write — pass `X-Acting-User` if `require-user-for-writes` is enabled)</span>

```bash
curl -s http://localhost:8084/mcp \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'X-Acting-User: himansu.nayak' \
  -d '{
        "jsonrpc":"2.0","id":2,"method":"tools/call",
        "params":{"name":"applyLeave","arguments":{"username":"himansu.nayak","date":"2026-06-10"}}
      }'
```

### <span style="color:hsl(37,68%,44%)">Find a replacement</span>

```bash
curl -s http://localhost:8084/mcp \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
        "jsonrpc":"2.0","id":3,"method":"tools/call",
        "params":{"name":"findReplacement","arguments":{"username":"himansu.nayak","date":"2026-06-10"}}
      }'
```

### <span style="color:hsl(69,68%,32%)">Actuator</span>

```bash
curl -s http://localhost:8084/actuator/health | jq
curl -s http://localhost:8084/actuator/prometheus | head -40
```
