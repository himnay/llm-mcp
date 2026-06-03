# AI Assistant (MCP Client)

The orchestrating **MCP client / chat assistant** for the Org platform. It exposes a
single chat endpoint and, for each user message, calls **OpenAI** for reasoning and delegates
tool execution to the downstream **MCP servers** (HR, Ticket, Deployment, Notification) over
Streamable HTTP. Slash-prefixed messages (e.g. `/promptName`) are expanded into pre-defined
MCP prompts before being sent to the model.

This module is a pure client — it has **no datasource of its own**.

## Tech Stack

| Concern              | Technology                                  |
|----------------------|---------------------------------------------|
| Language             | Java 17                                      |
| Framework            | Spring Boot 3.5.11                            |
| Web                  | Spring MVC (`spring-boot-starter-web`)       |
| AI                   | Spring AI — OpenAI model + MCP client         |
| Validation           | Jakarta Bean Validation                      |
| Observability        | Actuator + Micrometer + Prometheus registry   |
| Build                | Maven (`./mvnw`)                              |

## Running

```bash
./mvnw spring-boot:run
```


The downstream MCP servers (HR, Ticket, Deployment, Notification) must be reachable at their
configured URLs for tool calls to succeed.

### Required Environment Variables

| Variable         | Description                          |
|------------------|--------------------------------------|
| `OPENAI_API_KEY` | API key used by the Spring AI OpenAI client |

## Configuration

### `assistant.*` properties

| Property                | Default                   | Description                                         |
|-------------------------|---------------------------|-----------------------------------------------------|
| `assistant.name`        | `Enterprise AI Assistant` | Display name / persona of the assistant.            |
| `assistant.default-user`| `john.doe`                | Acting user when no authenticated principal exists. |

### MCP client connections (`spring.ai.mcp.client.streamable-http.connections.*`)

| Connection     | URL                     |
|----------------|-------------------------|
| `hr`           | `http://localhost:8084` |
| `deployment`   | `http://localhost:8082` |
| `notification` | `http://localhost:8083` |
| `ticket`       | `http://localhost:8081` |

## REST API

| Method | Path    | Body                      | Description                                              |
|--------|---------|---------------------------|----------------------------------------------------------|
| `POST` | `/chat` | `{ "message": "..." }`    | Send a message to the assistant and receive a response.  |

A message beginning with a slash (e.g. `/dailySummary`) is treated as an MCP prompt name and
expanded into the corresponding pre-defined prompt before being sent to the model.

## Best Practices Applied

- **Global exception handling** — `GlobalExceptionHandler` (`@RestControllerAdvice`) returns a
  consistent JSON error envelope (`status`, `error`, `message`, `details`, `timestamp`).
- **Request validation** — `@Valid` on the `/chat` request body with `@NotBlank` on `message`;
  invalid requests return `400 Bad Request` via the handler.
- **Typed configuration** — `@ConfigurationProperties(prefix = "assistant")` (`AssistantProperties`)
  replaces hardcoded values such as the acting user.
- **Observability** — Actuator endpoints (`health`, `info`, `metrics`, `prometheus`) with a
  Micrometer Prometheus registry.
- **Structured logging** — `logback-spring.xml` console pattern including the application name.
