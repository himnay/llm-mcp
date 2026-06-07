package com.org.ai.config;

import com.org.ai.resilience.ResilientToolCallbackProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AppConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    /**
     * Wraps the auto-configured MCP {@link ToolCallbackProvider} with per-server
     * circuit breakers. Marked {@code @Primary} so {@link com.org.ai.service.ChatService}
     * injects this wrapper instead of the raw provider.
     */
    @Bean
    @Primary
    public ToolCallbackProvider resilientToolCallbackProvider(ToolCallbackProvider mcpToolCallbackProvider, CircuitBreakerRegistry circuitBreakerRegistry) {
        return new ResilientToolCallbackProvider(mcpToolCallbackProvider, circuitBreakerRegistry);
    }
}
