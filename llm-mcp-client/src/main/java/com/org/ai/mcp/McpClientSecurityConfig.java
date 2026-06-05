package com.org.ai.mcp;

import com.org.ai.config.AssistantProperties;
import com.org.ai.web.RequestContext;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import org.springframework.ai.mcp.customizer.McpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Secures the outbound MCP connections to the downstream servers:
 * <ul>
 *   <li>attaches a shared bearer token ({@code assistant.mcp-auth-token}, bound from
 *       {@code MCP_AUTH_TOKEN}) so servers can authenticate the client, and</li>
 *   <li>forwards the acting user as {@code X-Acting-User} for server-side
 *       authorization and audit.</li>
 * </ul>
 * In Spring AI 2.0.x the Streamable-HTTP transport is configured through a
 * {@link McpClientCustomizer} that installs an {@link McpSyncHttpClientRequestCustomizer}.
 */
@Configuration
public class McpClientSecurityConfig {

    @Bean
    public McpSyncHttpClientRequestCustomizer mcpAuthRequestCustomizer(AssistantProperties properties) {
        return (builder, method, endpoint, body, context) -> {
            String token = properties.getMcpAuthToken();
            if (StringUtils.hasText(token)) {
                builder.header("Authorization", "Bearer " + token);
            }
            String user = RequestContext.user();
            if (StringUtils.hasText(user)) {
                builder.header("X-Acting-User", user);
            }
        };
    }

    @Bean
    public McpClientCustomizer<HttpClientStreamableHttpTransport.Builder> mcpAuthTransportCustomizer(
            McpSyncHttpClientRequestCustomizer requestCustomizer) {
        return (name, transportBuilder) -> transportBuilder.httpRequestCustomizer(requestCustomizer);
    }
}
