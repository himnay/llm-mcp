# Graph Report - .  (2026-06-08)

## Corpus Check

- Corpus is ~40,983 words - fits in a single context window. You may not need a graph.

## Summary

- 622 nodes · 741 edges · 86 communities detected
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 6 edges (avg confidence: 0.78)
- Token cost: 0 input · 0 output
- Edge kinds: method: 299 · calls: 166 · contains: 158 · imports: 42 · references: 37 · inherits: 21 · implements: 12 ·
  semantically_similar_to: 4 · conceptually_related_to: 1 · shares_data_with: 1

## Input Scope

- Requested: auto
- Resolved: committed (source: cli)
- Included files: 195 · Candidates: 296
- Excluded: 188 untracked · 323 ignored · 6 sensitive · 35 missing committed
- Recommendation: Use --scope all or graphify.yaml inputs.corpus for a knowledge-base folder.

## God Nodes (most connected - your core abstractions)

1. `GlobalExceptionHandler` - 19 edges
2. `GitHubMcpTools` - 18 edges
3. `GmailMcpTools` - 17 edges
4. `DeploymentMcpToolsValidationTest` - 15 edges
5. `RateLimiterTest` - 15 edges
6. `GitHubService` - 15 edges
7. `DeploymentMcpTools` - 14 edges
8. `GmailService` - 14 edges
9. `McpAuthFilterTest` - 14 edges
10. `McpToolConfig` - 12 edges

## Surprising Connections (you probably didn't know these)

- `mcp-server-deployment-service docker-compose.yml` --semantically_similar_to-->
  `postgres service (docker-compose)`  [INFERRED] [semantically similar]
  mcp-server-deployment-service/docker-compose.yml → docker-compose.yml
- `mcp-server-github-service application.yaml` --semantically_similar_to-->
  `mcp-server-gmail-service application.yaml`  [INFERRED] [semantically similar]
  mcp-server-github-service/src/main/resources/application.yaml →
  mcp-server-gmail-service/src/main/resources/application.yaml
- `mcp-server-hr-service docker-compose.yml` --semantically_similar_to-->
  `mcp-server-deployment-service docker-compose.yml`  [INFERRED] [semantically similar]
  mcp-server-hr-service/docker-compose.yml → mcp-server-deployment-service/docker-compose.yml
- `llm-mcp-client Insomnia Collection` --references-->
  `llm-mcp-client (MCP Client / Enterprise AI Assistant)`  [EXTRACTED]
  llm-mcp-client/insomnia-collection.yaml → README.md
- `llm-mcp-client banner.txt` --references--> `llm-mcp-client (MCP Client / Enterprise AI Assistant)`  [EXTRACTED]
  llm-mcp-client/src/main/resources/banner.txt → README.md

## Hyperedges (group relationships)

- **Shared PostgreSQL Instance with Per-Service Flyway Isolation** — hr_service, ticket_service, deployment_service,
  notification_service, llm_mcp_client [EXTRACTED 0.85]
- **Observability Stack: Prometheus + Tempo + Grafana wired across MCP services** — prometheus_yml, tempo_yml,
  grafana_datasources_provisioning [INFERRED 0.85]
- **Shared MCP token-auth/rate-limit security pattern across services** — ticket_service_application_yaml,
  travel_service_application_yaml [INFERRED 0.80]

## Communities

### Community 0 - "HR/Deployment JPA Entities"

Cohesion: 0.06
Nodes (18): ChatMessageEntity, Deployment, Employee, JpaRepository, LeaveRecord, Long, TicketPromptProvider,
PostgresConversationStore (+10 more)

### Community 1 - "AI Assistant Chat Config"

Cohesion: 0.07
Nodes (8): AppConfig, McpToolConfig, CircuitBreakerToolCallback, ResilientToolCallbackProvider, ChatService,
TruncatingToolCallback, ToolCallback, ToolCallbackProvider

### Community 2 - "Docker Compose Services"

Cohesion: 0.08
Nodes (36): llm-mcp-client application.yaml, llm-mcp-client banner.txt, github-service container (docker-compose),
gmail-service container (docker-compose), grafana service (docker-compose), mcp-inspector container (docker-compose),
postgres service (docker-compose), prometheus service (docker-compose) (+28 more)

### Community 3 - "Global Exception Handling"

Cohesion: 0.16
Nodes (1): GlobalExceptionHandler

### Community 4 - "GitHub MCP Tools"

Cohesion: 0.33
Nodes (1): GitHubMcpTools

### Community 5 - "Gmail MCP Tools"

Cohesion: 0.36
Nodes (1): GmailMcpTools

### Community 6 - "MCP Auth Filter"

Cohesion: 0.20
Nodes (3): OncePerRequestFilter, McpAuthFilter, RequestContextFilter

### Community 7 - "Deployment Tools Validation Tests"

Cohesion: 0.13
Nodes (1): DeploymentMcpToolsValidationTest

### Community 8 - "Rate Limiter Tests"

Cohesion: 0.13
Nodes (1): RateLimiterTest

### Community 9 - "GitHub Service Logic"

Cohesion: 0.14
Nodes (1): GitHubService

### Community 10 - "Gmail Service Logic"

Cohesion: 0.15
Nodes (1): GmailService

### Community 11 - "MCP Auth Filter Tests"

Cohesion: 0.13
Nodes (1): McpAuthFilterTest

### Community 12 - "Deployment MCP Tools"

Cohesion: 0.38
Nodes (1): DeploymentMcpTools

### Community 13 - "Flight Tools Validation Tests"

Cohesion: 0.17
Nodes (1): FlightMcpToolsValidationTest

### Community 14 - "Gmail Tools Validation Tests"

Cohesion: 0.17
Nodes (1): GmailMcpToolsValidationTest

### Community 15 - "Acting User Context"

Cohesion: 0.17
Nodes (1): ActingUserContext

### Community 16 - "HR Tools Validation Tests"

Cohesion: 0.18
Nodes (1): HrMcpToolsValidationTest

### Community 17 - "Rate Limiter"

Cohesion: 0.18
Nodes (1): RateLimiter

### Community 18 - "Resource Not Found Exception"

Cohesion: 0.20
Nodes (2): ResourceNotFoundException, RuntimeException

### Community 19 - "GitHub Tools Validation Tests"

Cohesion: 0.20
Nodes (1): GitHubMcpToolsValidationTest

### Community 20 - "Ticket Controller"

Cohesion: 0.53
Nodes (1): TicketController

### Community 21 - "Request Context"

Cohesion: 0.28
Nodes (1): RequestContext

### Community 22 - "Output Size Cap Util"

Cohesion: 0.25
Nodes (1): OutputSizeCapUtil

### Community 23 - "Security Config"

Cohesion: 0.25
Nodes (1): SecurityConfig

### Community 24 - "Flight Offers Response Model"

Cohesion: 0.25
Nodes (7): AirportTime, Dictionaries, FlightOffer, FlightOffersResponse, Itinerary, Price, Segment

### Community 25 - "Bounded Tool Calling Manager"

Cohesion: 0.33
Nodes (2): BoundedToolCallingManager, ToolCallingManager

### Community 26 - "Notification Tools"

Cohesion: 0.57
Nodes (1): NotificationTools

### Community 27 - "Ticket Service Logic"

Cohesion: 0.38
Nodes (1): TicketService

### Community 28 - "Deployment Service Logic"

Cohesion: 0.43
Nodes (1): DeploymentService

### Community 29 - "Security Config (Travel)"

Cohesion: 0.33
Nodes (1): SecurityConfig

### Community 30 - "Flight MCP Tools"

Cohesion: 0.47
Nodes (1): FlightMcpTools

### Community 31 - "HR MCP Tools"

Cohesion: 0.60
Nodes (1): HrMcpTools

### Community 32 - "Security Properties"

Cohesion: 0.33
Nodes (1): SecurityProperties

### Community 33 - "Flight Search Service"

Cohesion: 0.53
Nodes (1): FlightSearchService

### Community 34 - "Tool Output Util"

Cohesion: 0.40
Nodes (1): ToolOutputUtil

### Community 35 - "MCP Client Health Indicator"

Cohesion: 0.60
Nodes (2): McpClientHealthIndicator, HealthIndicator

### Community 36 - "Travel Service Bundle"

Cohesion: 0.40
Nodes (5): mcp-travel-service, mcp-travel-service (container), Travel Service application.yaml, Travel Service Banner,
Travel Service docker-compose.yml

### Community 37 - "Amadeus Flight Client"

Cohesion: 0.50
Nodes (1): AmadeusFlightClient

### Community 38 - "GitHub Client Config"

Cohesion: 0.50
Nodes (1): GitHubClientConfig

### Community 39 - "Gmail Client Config"

Cohesion: 0.50
Nodes (1): GmailClientConfig

### Community 40 - "REST Client Config"

Cohesion: 0.50
Nodes (1): RestClientConfig

### Community 41 - "MCP Client Security Config"

Cohesion: 0.50
Nodes (1): McpClientSecurityConfig

### Community 42 - "AI Assistant App Entry"

Cohesion: 0.67
Nodes (1): AiAssistantMcpApplication

### Community 43 - "AI Assistant App Tests"

Cohesion: 0.67
Nodes (1): AiAssistantMcpApplicationTests

### Community 44 - "MCP Output Properties"

Cohesion: 0.67
Nodes (1): McpOutputProperties

### Community 45 - "Rate Limiter Config"

Cohesion: 0.67
Nodes (1): RateLimiterConfig

### Community 46 - "Security Properties (alt)"

Cohesion: 0.67
Nodes (1): SecurityProperties

### Community 47 - "Chat Controller"

Cohesion: 0.67
Nodes (1): ChatController

### Community 48 - "Deployment Service App Entry"

Cohesion: 0.67
Nodes (1): DeploymentServiceApplication

### Community 49 - "Deployment Service App Tests"

Cohesion: 0.67
Nodes (1): DeploymentServiceApplicationTests

### Community 50 - "GitHub Service App Entry"

Cohesion: 0.67
Nodes (1): GitHubServiceApplication

### Community 51 - "GitHub Service App Tests"

Cohesion: 0.67
Nodes (1): GitHubServiceApplicationTests

### Community 52 - "Gmail Service App Entry"

Cohesion: 0.67
Nodes (1): GmailServiceApplication

### Community 53 - "Gmail Service App Tests"

Cohesion: 0.67
Nodes (1): GmailServiceApplicationTests

### Community 54 - "HR Service App Entry"

Cohesion: 0.67
Nodes (1): HrServiceApplication

### Community 55 - "HR Service App Tests"

Cohesion: 0.67
Nodes (1): HrServiceApplicationTests

### Community 56 - "Chat Message Entity"

Cohesion: 0.67
Nodes (1): ChatMessageEntity

### Community 57 - "Resilience Config"

Cohesion: 0.67
Nodes (1): ResilienceConfig

### Community 58 - "Prompt Loader"

Cohesion: 0.67
Nodes (1): PromptLoader

### Community 59 - "Rate Limiter Core"

Cohesion: 0.67
Nodes (1): RateLimiter

### Community 60 - "HR/Deployment DB Schema"

Cohesion: 0.67
Nodes (2): employee, leave_record

### Community 61 - "Notification Service App Entry"

Cohesion: 0.67
Nodes (1): NotificationServiceApplication

### Community 62 - "Notification Service App Tests"

Cohesion: 0.67
Nodes (1): NotificationServiceApplicationTests

### Community 63 - "Ticket Service App Entry"

Cohesion: 0.67
Nodes (1): TicketServiceApplication

### Community 64 - "Ticket Service App Tests"

Cohesion: 0.67
Nodes (1): TicketServiceApplicationTests

### Community 65 - "Travel Service App Entry"

Cohesion: 0.67
Nodes (1): TravelServiceApplication

### Community 66 - "Travel Service App Tests"

Cohesion: 0.67
Nodes (1): TravelServiceApplicationTests

### Community 67 - "Amadeus Properties"

Cohesion: 1.00
Nodes (1): AmadeusProperties

### Community 68 - "Assistant Properties"

Cohesion: 1.00
Nodes (1): AssistantProperties

### Community 69 - "GitHub Properties"

Cohesion: 1.00
Nodes (1): GitHubProperties

### Community 70 - "Gmail Properties"

Cohesion: 1.00
Nodes (1): GmailProperties

### Community 71 - "Chat Request Model"

Cohesion: 1.00
Nodes (1): ChatRequest

### Community 72 - "Chat Response Model"

Cohesion: 1.00
Nodes (1): ChatResponse

### Community 73 - "Grafana Provisioning"

Cohesion: 1.00
Nodes (2): Grafana Dashboards Provisioning, Grafana Datasources Provisioning

### Community 74 - "Chat History DB Schema"

Cohesion: 1.00
Nodes (1): chat_message

### Community 75 - "Deployment Entity"

Cohesion: 1.00
Nodes (1): Deployment

### Community 76 - "Deployment DB Schema"

Cohesion: 1.00
Nodes (1): deployment

### Community 77 - "Employee Entity"

Cohesion: 1.00
Nodes (1): Employee

### Community 78 - "Leave Record Entity"

Cohesion: 1.00
Nodes (1): LeaveRecord

### Community 79 - "Notification Entity"

Cohesion: 1.00
Nodes (1): Notification

### Community 80 - "Notification DB Schema"

Cohesion: 1.00
Nodes (1): notification

### Community 81 - "Ticket Entity"

Cohesion: 1.00
Nodes (1): Ticket

### Community 82 - "Ticket DB Schema"

Cohesion: 1.00
Nodes (1): ticket

### Community 83 - "HR Service Banner"

Cohesion: 1.00
Nodes (1): HR Service Banner

### Community 94 - "Prometheus Scrape Config"

Cohesion: 1.00
Nodes (1): prometheus.yml (scrape config)

### Community 95 - "Tempo Tracing Config"

Cohesion: 1.00
Nodes (1): tempo.yml (Tempo config)

## Knowledge Gaps

- **46 isolated node(s):** `AssistantProperties`, `ChatRequest`, `ChatResponse`, `chat_message`, `Deployment` (+41 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **Thin community `Global Exception Handling`** (1 nodes): `GlobalExceptionHandler`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `GitHub MCP Tools`** (1 nodes): `GitHubMcpTools`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Gmail MCP Tools`** (1 nodes): `GmailMcpTools`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Deployment Tools Validation Tests`** (1 nodes): `DeploymentMcpToolsValidationTest`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Rate Limiter Tests`** (1 nodes): `RateLimiterTest`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `GitHub Service Logic`** (1 nodes): `GitHubService`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Gmail Service Logic`** (1 nodes): `GmailService`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `MCP Auth Filter Tests`** (1 nodes): `McpAuthFilterTest`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Deployment MCP Tools`** (1 nodes): `DeploymentMcpTools`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Flight Tools Validation Tests`** (1 nodes): `FlightMcpToolsValidationTest`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Gmail Tools Validation Tests`** (1 nodes): `GmailMcpToolsValidationTest`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Acting User Context`** (1 nodes): `ActingUserContext`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `HR Tools Validation Tests`** (1 nodes): `HrMcpToolsValidationTest`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Rate Limiter`** (1 nodes): `RateLimiter`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Resource Not Found Exception`** (2 nodes): `ResourceNotFoundException`, `RuntimeException`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `GitHub Tools Validation Tests`** (1 nodes): `GitHubMcpToolsValidationTest`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Ticket Controller`** (1 nodes): `TicketController`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Request Context`** (1 nodes): `RequestContext`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Output Size Cap Util`** (1 nodes): `OutputSizeCapUtil`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Security Config`** (1 nodes): `SecurityConfig`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Bounded Tool Calling Manager`** (2 nodes): `BoundedToolCallingManager`, `ToolCallingManager`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Notification Tools`** (1 nodes): `NotificationTools`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Ticket Service Logic`** (1 nodes): `TicketService`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Deployment Service Logic`** (1 nodes): `DeploymentService`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Security Config (Travel)`** (1 nodes): `SecurityConfig`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Flight MCP Tools`** (1 nodes): `FlightMcpTools`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `HR MCP Tools`** (1 nodes): `HrMcpTools`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Security Properties`** (1 nodes): `SecurityProperties`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Flight Search Service`** (1 nodes): `FlightSearchService`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Tool Output Util`** (1 nodes): `ToolOutputUtil`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `MCP Client Health Indicator`** (2 nodes): `McpClientHealthIndicator`, `HealthIndicator`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Amadeus Flight Client`** (1 nodes): `AmadeusFlightClient`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `GitHub Client Config`** (1 nodes): `GitHubClientConfig`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Gmail Client Config`** (1 nodes): `GmailClientConfig`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `REST Client Config`** (1 nodes): `RestClientConfig`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `MCP Client Security Config`** (1 nodes): `McpClientSecurityConfig`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `AI Assistant App Entry`** (1 nodes): `AiAssistantMcpApplication`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `AI Assistant App Tests`** (1 nodes): `AiAssistantMcpApplicationTests`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `MCP Output Properties`** (1 nodes): `McpOutputProperties`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Rate Limiter Config`** (1 nodes): `RateLimiterConfig`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Security Properties (alt)`** (1 nodes): `SecurityProperties`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Chat Controller`** (1 nodes): `ChatController`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Deployment Service App Entry`** (1 nodes): `DeploymentServiceApplication`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Deployment Service App Tests`** (1 nodes): `DeploymentServiceApplicationTests`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `GitHub Service App Entry`** (1 nodes): `GitHubServiceApplication`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `GitHub Service App Tests`** (1 nodes): `GitHubServiceApplicationTests`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Gmail Service App Entry`** (1 nodes): `GmailServiceApplication`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Gmail Service App Tests`** (1 nodes): `GmailServiceApplicationTests`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `HR Service App Entry`** (1 nodes): `HrServiceApplication`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `HR Service App Tests`** (1 nodes): `HrServiceApplicationTests`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Chat Message Entity`** (1 nodes): `ChatMessageEntity`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Resilience Config`** (1 nodes): `ResilienceConfig`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Prompt Loader`** (1 nodes): `PromptLoader`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Rate Limiter Core`** (1 nodes): `RateLimiter`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `HR/Deployment DB Schema`** (2 nodes): `employee`, `leave_record`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Notification Service App Entry`** (1 nodes): `NotificationServiceApplication`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Notification Service App Tests`** (1 nodes): `NotificationServiceApplicationTests`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Ticket Service App Entry`** (1 nodes): `TicketServiceApplication`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Ticket Service App Tests`** (1 nodes): `TicketServiceApplicationTests`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Travel Service App Entry`** (1 nodes): `TravelServiceApplication`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Travel Service App Tests`** (1 nodes): `TravelServiceApplicationTests`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Amadeus Properties`** (1 nodes): `AmadeusProperties`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Assistant Properties`** (1 nodes): `AssistantProperties`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `GitHub Properties`** (1 nodes): `GitHubProperties`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Gmail Properties`** (1 nodes): `GmailProperties`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Chat Request Model`** (1 nodes): `ChatRequest`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Chat Response Model`** (1 nodes): `ChatResponse`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Grafana Provisioning`** (2 nodes): `Grafana Dashboards Provisioning`,
  `Grafana Datasources Provisioning`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Chat History DB Schema`** (1 nodes): `chat_message`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Deployment Entity`** (1 nodes): `Deployment`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Deployment DB Schema`** (1 nodes): `deployment`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Employee Entity`** (1 nodes): `Employee`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Leave Record Entity`** (1 nodes): `LeaveRecord`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Notification Entity`** (1 nodes): `Notification`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Notification DB Schema`** (1 nodes): `notification`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Ticket Entity`** (1 nodes): `Ticket`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Ticket DB Schema`** (1 nodes): `ticket`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `HR Service Banner`** (1 nodes): `HR Service Banner`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Prometheus Scrape Config`** (1 nodes): `prometheus.yml (scrape config)`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Tempo Tracing Config`** (1 nodes): `tempo.yml (Tempo config)`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.

## Suggested Questions

_Questions this graph is uniquely positioned to answer:_

- **Why does `DeploymentMcpTools` connect `Deployment MCP Tools` to `HR/Deployment JPA Entities`?**
  _High betweenness centrality (0.005) - this node is a cross-community bridge._
- **What connects `AssistantProperties`, `ChatRequest`, `ChatResponse` to the rest of the system?**
  _46 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `HR/Deployment JPA Entities` be split into smaller, more focused modules?**
  _Cohesion score 0.05990338164251208 - nodes in this community are weakly interconnected._
- **Should `AI Assistant Chat Config` be split into smaller, more focused modules?**
  _Cohesion score 0.06538461538461539 - nodes in this community are weakly interconnected._
- **Should `Docker Compose Services` be split into smaller, more focused modules?**
  _Cohesion score 0.08408408408408409 - nodes in this community are weakly interconnected._
- **Should `Deployment Tools Validation Tests` be split into smaller, more focused modules?**
  _Cohesion score 0.125 - nodes in this community are weakly interconnected._
- **Should `Rate Limiter Tests` be split into smaller, more focused modules?**
  _Cohesion score 0.125 - nodes in this community are weakly interconnected._