package com.org.ai.config;

import com.org.ai.audit.ToolAuditLog;
import com.org.ai.resilience.ResilientToolCallbackProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.modelcontextprotocol.client.McpSyncClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
public class AppConfig {

    private static String clientName(McpSyncClient client) {
        try {
            return client.getClientInfo().name();
        } catch (Exception e) {
            return "unknown";
        }
    }

    @Bean
    public ChatMemory chatMemory(JdbcChatMemoryRepository repository, AssistantProperties properties) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(properties.getMemoryWindow())
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory, AssistantProperties properties) {
        List<String> sensitiveWords = properties.getSensitiveWords().isEmpty()
                ? List.of("ignore previous instructions", "jailbreak", "prompt injection",
                "ignore all previous", "forget your instructions", "you are now DAN")
                : properties.getSensitiveWords();

        return ChatClient.builder(chatModel)
                .defaultAdvisors(
                        SafeGuardAdvisor.builder()
                                .sensitiveWords(sensitiveWords)
                                .order(Integer.MIN_VALUE)
                                .build(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor()
                )
                .build();
    }

    /**
     * Attempts to initialize each MCP client individually at startup. Clients whose
     * downstream server is unreachable are skipped rather than failing the whole boot,
     * so the assistant starts with whatever subset of tools is currently available.
     */
    @Bean
    @Primary
    public ToolCallbackProvider resilientToolCallbackProvider(
            ObjectProvider<List<McpSyncClient>> mcpSyncClientsProvider,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            ToolAuditLog toolAuditLog,
            @Value("${assistant.tool.timeout-seconds:30}") int toolTimeoutSeconds) {

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

        return new ResilientToolCallbackProvider(delegate, circuitBreakerRegistry, retryRegistry,
                toolTimeoutSeconds, toolAuditLog);
    }
}
