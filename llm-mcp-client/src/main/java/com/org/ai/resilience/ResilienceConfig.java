package com.org.ai.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
public class ResilienceConfig {

    private static final List<String> MCP_SERVERS =
            List.of("mcp-hr", "mcp-ticket", "mcp-deployment", "mcp-notification", "mcp-github", "mcp-gmail");

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(MeterRegistry meterRegistry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .failureRateThreshold(50)                         // open after 50% failures
                .slowCallRateThreshold(80)                        // also open on 80% slow calls
                .slowCallDurationThreshold(Duration.ofSeconds(5)) // threshold for "slow"
                .waitDurationInOpenState(Duration.ofSeconds(30))  // stay open 30s before retry
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(Exception.class)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);

        // Pre-create named circuit breakers for each MCP server
        MCP_SERVERS.forEach(registry::circuitBreaker);

        // Publish circuit breaker state, failure rate, call stats to Prometheus
        TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry).bindTo(meterRegistry);

        return registry;
    }
}
