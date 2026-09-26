package com.org.ai.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ResilientToolCallbackProviderTest {

    private final ResilientToolCallbackProvider provider = new ResilientToolCallbackProvider(
            () -> new ToolCallback[0], Map.of("getRepository", "mcp-github"),
            List.of("create", "execute", "cancel"), CircuitBreakerRegistry.ofDefaults(), RetryRegistry.ofDefaults(),
            5, null);

    @Test
    @DisplayName("Write tools are recognised by their leading verb and therefore not retried")
    void classifiesWriteTools() {
        assertThat(provider.isWriteTool("createIssue")).isTrue();
        assertThat(provider.isWriteTool("executeDeployment")).isTrue();
        assertThat(provider.isWriteTool("CancelDeployment")).isTrue();
        assertThat(provider.isWriteTool("getRepository")).isFalse();
        assertThat(provider.isWriteTool("getDeployments")).as("a read that merely contains 'deploy'").isFalse();
    }
}
