package com.org.ai.health;

import io.modelcontextprotocol.client.McpSyncClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Surfaces downstream MCP server reachability through the actuator health
 * endpoint by pinging each configured client. Reports DOWN if any server is
 * unreachable, with per-server detail.
 */
@Component
@RequiredArgsConstructor
public class McpClientHealthIndicator implements HealthIndicator {

    private final List<McpSyncClient> mcpSyncClients;

    @Override
    public Health health() {
        Map<String, Object> details = new LinkedHashMap<>();
        boolean allUp = true;

        for (McpSyncClient client : mcpSyncClients) {
            String name = serverName(client);
            try {
                client.ping();
                details.put(name, "UP");
            } catch (Exception ex) {
                allUp = false;
                details.put(name, "DOWN: " + ex.getMessage());
            }
        }

        return (allUp ? Health.up() : Health.down()).withDetails(details).build();
    }

    private static String serverName(McpSyncClient client) {
        try {
            if (client.getServerInfo() != null && client.getServerInfo().name() != null) {
                return client.getServerInfo().name();
            }
        } catch (Exception ignored) {
            // fall through to default
        }
        return "mcp-server";
    }
}
