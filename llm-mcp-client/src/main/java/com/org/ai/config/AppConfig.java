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
     * ToolCallbackProvider bean that wraps the MCP ToolCallbackProvider with a ResilientToolCallbackProvider to add resilience features like circuit breaking.
     * This allows the application to handle failures gracefully when invoking tools, improving overall robustness.
     * @param mcpToolCallbackProvider
     * @param circuitBreakerRegistry
     * @return
     */
    @Bean
    @Primary
    public ToolCallbackProvider resilientToolCallbackProvider(ToolCallbackProvider mcpToolCallbackProvider, CircuitBreakerRegistry circuitBreakerRegistry) {
        return new ResilientToolCallbackProvider(mcpToolCallbackProvider, circuitBreakerRegistry);
    }
}
