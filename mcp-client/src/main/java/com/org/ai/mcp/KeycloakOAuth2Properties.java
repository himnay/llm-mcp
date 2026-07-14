package com.org.ai.mcp;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Client-credentials settings for the Keycloak realm that issues access tokens this
 * client presents to OAuth2.1-protected MCP servers (currently: deployment-service).
 *
 * <pre>
 * mcp:
 *   oauth2:
 *     token-uri: ${MCP_OAUTH2_TOKEN_URI:http://localhost:8180/realms/org-mcp/protocol/openid-connect/token}
 *     client-id: ${MCP_OAUTH2_CLIENT_ID:llm-mcp-client}
 *     client-secret: ${MCP_OAUTH2_CLIENT_SECRET:llm-mcp-client-secret}
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "mcp.oauth2")
public class KeycloakOAuth2Properties {

    private String tokenUri = "http://localhost:8180/realms/org-mcp/protocol/openid-connect/token";

    private String clientId = "llm-mcp-client";

    private String clientSecret = "llm-mcp-client-secret";
}
