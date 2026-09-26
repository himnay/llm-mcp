package com.org.ai.config;

import com.org.ai.audit.ToolAuditLog;
import com.org.ai.mcp.BoundedToolCallingManager;
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
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Wraps whichever {@link ToolCallingManager} Spring AI auto-configures (the one the chat models
     * use for internal tool execution) in {@link BoundedToolCallingManager}, which caps tool rounds
     * per request at {@code assistant.max-tool-iterations}. Static, and reading the limit from the
     * {@link Environment}, so this post-processor does not pull other beans in early.
     */
    @Bean
    static BeanPostProcessor boundedToolCallingManagerPostProcessor(Environment environment) {
        int maxIterations = environment.getProperty("assistant.max-tool-iterations", Integer.class, 5);
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof ToolCallingManager manager && !(bean instanceof BoundedToolCallingManager)) {
                    return new BoundedToolCallingManager(manager, maxIterations);
                }
                return bean;
            }
        };
    }

    /** Defines the chat memory bean. */
    @Bean
    public ChatMemory chatMemory(JdbcChatMemoryRepository repository, AssistantProperties properties) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(properties.getMemoryWindow())
                .build();
    }

    /** Defines the chat client bean. */
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
            AssistantProperties assistantProperties,
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

        return new ResilientToolCallbackProvider(delegate, toolServers(available),
                assistantProperties.getWriteToolKeywords(), circuitBreakerRegistry, retryRegistry,
                toolTimeoutSeconds, toolAuditLog);
    }

    /**
     * Tool name → circuit-breaker/retry name ({@code mcp-<connection>}), read from each connected
     * server's own tool list, so a tool added to a server is routed without touching this client.
     * Spring AI names each client {@code <client-name> - <connection-name>}.
     */
    static Map<String, String> toolServers(List<McpSyncClient> clients) {
        Map<String, String> routing = new HashMap<>();
        for (McpSyncClient client : clients) {
            String name = clientName(client);
            int separator = name.lastIndexOf(" - ");
            String server = "mcp-" + (separator < 0 ? name : name.substring(separator + 3)).trim();
            try {
                client.listTools().tools().forEach(tool -> routing.putIfAbsent(tool.name(), server));
            } catch (Exception e) {
                log.warn("Could not list tools of {} — its tools share the 'mcp-unknown' breaker: {}", name, e.getMessage());
            }
        }
        return routing;
    }
}
