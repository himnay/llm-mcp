# OAuth 2.1 MCP Security via Keycloak — Setup Guide

This document covers the OAuth 2.1 authorization flow protecting `mcp-server-deployment-service` (the first MCP
server migrated off the legacy shared-bearer-token scheme — see [`README.md`](README.md#spring-ai-20-mcp--feature-status)).
Keycloak acts as the **Authorization Server**; `llm-mcp-client` is the **OAuth2 client** (machine-to-machine,
client-credentials grant); `mcp-server-deployment-service` is the **Resource Server** validating the JWT.

```
llm-mcp-client  --(client_credentials)-->  Keycloak  --(JWT access token)-->  llm-mcp-client
llm-mcp-client  --(Bearer <JWT> on every MCP call)-->  mcp-server-deployment-service
```

## Option A — automatic (what this repo does by default)

`docker-compose.yml` brings up Keycloak pre-configured: `docker/keycloak/realm-export.json` is mounted into the
container and imported automatically on startup (`start-dev --import-realm`), so **no manual console clicking is
required** to run the demo as-is.

```bash
docker compose up -d keycloak postgres
```

Keycloak admin console: http://localhost:8180 (`admin` / `admin`). The realm `org-mcp` and client `llm-mcp-client`
already exist — open the console only if you want to inspect or change what was imported.

The rest of this document explains **how that realm was built**, step by step, in case you want to add a second
protected server, change the scope/secret, or just understand what the JSON import did.

## Option B — manual setup via the Keycloak Admin Console

### 1. Create the realm

1. Log in to http://localhost:8180 as `admin` / `admin`.
2. Top-left realm dropdown → **Create realm**.
3. Realm name: `org-mcp` → **Create**.

### 2. Create a client scope for deployment-service

A client scope is how we (a) get a custom value into the JWT's `scope` claim, and (b) stamp an `aud` (audience)
claim onto the token so it can't be replayed against a different MCP server.

1. **Client scopes** (left nav) → **Create client scope**.
2. Name: `deployment-invoke`. Type: **Default** (so it's always included, not opt-in per request). Protocol:
   `openid-connect`. Include In Token Scope: **On**. → **Save**.
3. Open the new scope → **Mappers** tab → **Add mapper** → **By configuration** → **Audience**.
4. Name: `deployment-service-audience`. Included Custom Audience: `deployment-service`. Add to access token:
   **On**. Add to ID token: **Off**. → **Save**.

This is exactly what `protocolMappers` + `oidc-audience-mapper` does in `docker/keycloak/realm-export.json` — the
console just has a friendlier UI for the same JSON.

### 3. Create the client (service account for `llm-mcp-client`)

1. **Clients** (left nav) → **Create client**.
2. General settings: Client type `OpenID Connect`, Client ID `llm-mcp-client` → **Next**.
3. Capability config: turn **Client authentication** **On** (this makes it a confidential client). Authentication
   flow: enable only **Service accounts roles** (client-credentials grant) — leave Standard flow, Direct access
   grants, and Implicit flow all **off**, since this is a backend-to-backend client with no human/browser login.
   → **Next** → **Save**.
4. Open the client → **Credentials** tab → copy the **Client secret** (or regenerate one). This is the value that
   goes into `MCP_OAUTH2_CLIENT_SECRET` below. (The pre-baked realm export hardcodes this to
   `llm-mcp-client-secret` for local dev — change it for anything beyond a local demo.)
5. **Client scopes** tab on the client → confirm `deployment-invoke` is listed under **Default**. If you created
   the client before the scope existed, add it here as a **Default** (not Optional) scope.

### 4. Verify by requesting a token directly

```bash
curl -s http://localhost:8180/realms/org-mcp/protocol/openid-connect/token \
  -d grant_type=client_credentials \
  -d client_id=llm-mcp-client \
  -d client_secret=llm-mcp-client-secret | jq .
```

Decode the `access_token` (e.g. on https://jwt.io or `jwt -` if you have a CLI) and confirm:
- `scope` contains `deployment-invoke`
- `aud` contains `deployment-service`
- `iss` is `http://localhost:8180/realms/org-mcp`

## Wiring the URLs into `application.yaml`

Two different URLs are needed, on two different sides — don't mix them up:

| URL | Used by | Property | Points at |
|-----|---------|----------|-----------|
| **Issuer URI** | Resource server (`mcp-server-deployment-service`) — used for OIDC discovery (JWKS, issuer check) | `spring.security.oauth2.resourceserver.jwt.issuer-uri` | `.../realms/org-mcp` (no `/protocol/...` suffix — Spring Security appends `/.well-known/openid-configuration` itself) |
| **Token URI** | OAuth2 client (`llm-mcp-client`) — used to actually request a token | `mcp.oauth2.token-uri` | `.../realms/org-mcp/protocol/openid-connect/token` (the full token endpoint) |

**`mcp-server-deployment-service/src/main/resources/application.yaml`:**

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${MCP_OAUTH2_ISSUER_URI:http://localhost:8180/realms/org-mcp}
```

**`llm-mcp-client/src/main/resources/application.yaml`:**

```yaml
mcp:
  oauth2:
    token-uri: ${MCP_OAUTH2_TOKEN_URI:http://localhost:8180/realms/org-mcp/protocol/openid-connect/token}
    client-id: ${MCP_OAUTH2_CLIENT_ID:llm-mcp-client}
    client-secret: ${MCP_OAUTH2_CLIENT_SECRET:llm-mcp-client-secret}
```

Both default to `localhost:8180` for local (`./mvnw spring-boot:run`) development, where Keycloak runs in Docker but
exposes its port to the host. When everything runs inside `docker-compose` instead, the env vars are overridden to
the in-network hostname (`http://keycloak:8080/...`) — see the `keycloak`/`deployment-service`/`mcp-client` service
blocks in `docker-compose.yml`.

> **Local dev vs. Docker network — why the host differs:** `localhost:8180` only resolves on the machine running
> Docker Desktop (the port mapping `8180:8080` in `docker-compose.yml` is what makes that work). A container talking
> to *another* container reaches it by **service name** on the internal Docker network instead — `keycloak:8080`
> (the container's own port, not the host-mapped one). Get this backwards and you'll see `UnknownHostException:
> keycloak` (running locally) or connection refused on 8180 (running fully in Docker).

## How the server actually enforces it

`mcp-server-deployment-service`'s `OAuth2ResourceServerConfig` (`com.org.deployment.security`):

- Builds a `JwtDecoder` wrapped in `SupplierJwtDecoder` so the Keycloak discovery call is **lazy** (first real
  request, not at app startup — keeps `./mvnw test` fast and not dependent on Keycloak being up).
- Adds a custom token validator on top of the default issuer check: requires `aud` to contain
  `deployment-service`.
- `SecurityFilterChain` requires authority `SCOPE_deployment-invoke` on every request to `/mcp/**` — Spring
  Security's default JWT-to-authority converter maps the token's `scope` claim entries to `SCOPE_<value>`
  authorities automatically, so no custom `JwtAuthenticationConverter` was needed.
- Set `mcp.security.oauth2.enabled=false` to drop this filter chain entirely (used in the test profile, and
  available as a manual kill switch if you need to run this service without Keycloak).

## How the client actually presents it

`llm-mcp-client`'s `KeycloakTokenService` (`com.org.ai.mcp`) fetches a token via `client_credentials`, caches it,
and refreshes 60 seconds before expiry — the same shape as `AmadeusTokenService` in `mcp-server-travel-service`
(both are a caching-proxy over an OAuth2 client-credentials endpoint). `McpClientSecurityConfig` installs this
token (instead of the legacy static bearer token) only on the connection named `deployment`; every other MCP
connection keeps using `assistant.mcp-auth-token`.

## Rolling this out to other servers

To protect a second server (say `mcp-server-github-service`) the same way:

1. Keycloak: add a new client scope (e.g. `github-invoke`) with its own audience mapper (`aud: github-service`),
   and add it as a **Default** scope on `llm-mcp-client` (one client can hold scopes for multiple servers — no
   need for a second OAuth2 client unless you want per-server credential rotation).
2. That server: copy `OAuth2ResourceServerConfig`, change `REQUIRED_SCOPE_AUTHORITY`/`REQUIRED_AUDIENCE`, add the
   `spring-boot-starter-oauth2-resource-server` dependency and the `issuer-uri` property.
3. `llm-mcp-client`: add `"github"` alongside `"deployment"` in `McpClientSecurityConfig`'s OAuth2 connection-name
   check (today a single `String` constant — promote it to a `Set<String>` once there's more than one).
