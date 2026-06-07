package com.org.travel.config;

import com.org.travel.security.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

@Slf4j
@Configuration
@EnableConfigurationProperties({SecurityProperties.class, McpOutputProperties.class, AmadeusProperties.class})
public class SecurityConfig {

    private final SecurityProperties securityProperties;
    private final AmadeusProperties amadeusProperties;

    public SecurityConfig(SecurityProperties securityProperties, AmadeusProperties amadeusProperties) {
        this.securityProperties = securityProperties;
        this.amadeusProperties = amadeusProperties;
    }

    @Bean
    public RateLimiter rateLimiter() {
        return new RateLimiter(securityProperties.getRateLimitPerMinute());
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onStartup() {
        if (securityProperties.getToken() == null || securityProperties.getToken().isBlank()) {
            log.warn("MCP auth token not configured – running in INSECURE dev mode");
        }
        if (amadeusProperties.getClientId() == null || amadeusProperties.getClientId().isBlank()) {
            log.warn("Amadeus client_id not configured – flight search will fail");
        }
    }
}
