package com.org.ai.config;

import com.org.ai.resilience.ResilientToolCallbackProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.modelcontextprotocol.client.McpSyncClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
public class AppConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    /**
     * Attempts to initialize each MCP client individually at startup. Clients whose
     * downstream server is unreachable are skipped rather than failing the whole boot,
     * so the assistant starts with whatever subset of tools is currently available.
     *
     * Uses ObjectProvider<List<McpSyncClient>> (same pattern as McpToolCallbackAutoConfiguration)
     * to correctly resolve the single List<McpSyncClient> bean registered by Spring AI autoconfigure,
     * rather than attempting to collect individual McpSyncClient beans (which would yield empty).
     */
    @Bean
    @Primary
    public ToolCallbackProvider resilientToolCallbackProvider(
            ObjectProvider<List<McpSyncClient>> mcpSyncClientsProvider,
            CircuitBreakerRegistry circuitBreakerRegistry) {

        List<McpSyncClient> allClients = mcpSyncClientsProvider.stream()
                .flatMap(List::stream)
                .toList();

        List<McpSyncClient> available = new ArrayList<>();
        for (McpSyncClient client : allClients) {
            String name = clientName(client);
            try {
                client.initialize();
                available.add(client);
                log.info("MCP server connected: {}", name);
            } catch (Exception e) {
                log.warn("MCP server unavailable, skipping: {} — {}", name, e.getMessage());
            }
        }

        if (available.isEmpty()) {
            log.warn("No MCP servers are reachable — the assistant will have no tools available");
        }

        SyncMcpToolCallbackProvider delegate = SyncMcpToolCallbackProvider.builder()
                .mcpClients(available)
                .build();

        return new ResilientToolCallbackProvider(delegate, circuitBreakerRegistry);
    }

    private static String clientName(McpSyncClient client) {
        try {
            return client.getClientInfo().name();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
