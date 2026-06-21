package com.org.ai.mcp;

import com.org.ai.config.AssistantProperties;
import com.org.ai.web.RequestContext;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import org.springframework.ai.mcp.customizer.McpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.net.http.HttpRequest;

/**
 * Secures the outbound MCP connections to the downstream servers. Per connection, attaches
 * one of two {@code Authorization} schemes plus the acting user as {@code X-Acting-User} for
 * server-side authorization and audit:
 * <ul>
 *   <li>{@code deployment} — OAuth2.1: a client-credentials access token fetched from Keycloak
 *       via {@link KeycloakTokenService}, since deployment-service is an OAuth2 resource
 *       server (see its {@code OAuth2ResourceServerConfig}).</li>
 *   <li>every other connection — the legacy shared bearer token
 *       ({@code assistant.mcp-auth-token}, bound from {@code MCP_AUTH_TOKEN}).</li>
 * </ul>
 * In Spring AI 2.0.x the Streamable-HTTP transport is configured through a
 * {@link McpClientCustomizer}, which is the only customizer hook that receives the connection
 * name — it installs a per-connection {@link McpSyncHttpClientRequestCustomizer}.
 */
@Configuration
public class McpClientSecurityConfig {

    private static final String OAUTH2_CONNECTION_NAME = "deployment";

    @Bean
    public McpClientCustomizer<HttpClientStreamableHttpTransport.Builder> mcpAuthTransportCustomizer(
            AssistantProperties properties, KeycloakTokenService keycloakTokenService) {
        McpSyncHttpClientRequestCustomizer oauth2Customizer = oauth2RequestCustomizer(keycloakTokenService);
        McpSyncHttpClientRequestCustomizer staticTokenCustomizer = staticBearerRequestCustomizer(properties);

        return (name, transportBuilder) -> transportBuilder.httpRequestCustomizer(
                OAUTH2_CONNECTION_NAME.equals(name) ? oauth2Customizer : staticTokenCustomizer);
    }

    private McpSyncHttpClientRequestCustomizer staticBearerRequestCustomizer(AssistantProperties properties) {
        return (builder, method, endpoint, body, context) -> {
            String token = properties.getMcpAuthToken();
            if (StringUtils.hasText(token)) {
                builder.header("Authorization", "Bearer " + token);
            }
            attachActingUser(builder);
        };
    }

    private McpSyncHttpClientRequestCustomizer oauth2RequestCustomizer(KeycloakTokenService keycloakTokenService) {
        return (builder, method, endpoint, body, context) -> {
            builder.header("Authorization", "Bearer " + keycloakTokenService.getToken());
            attachActingUser(builder);
        };
    }

    private static void attachActingUser(HttpRequest.Builder builder) {
        String user = RequestContext.user();
        if (StringUtils.hasText(user)) {
            builder.header("X-Acting-User", user);
        }
    }
}
