package com.org.notification.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * Registers security infrastructure beans and emits a startup warning when the
 * MCP auth token is not configured (insecure dev mode).
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    @Bean
    public RateLimiter rateLimiter(SecurityProperties props) {
        return new RateLimiter(props.getRateLimitPerMinute());
    }

    @EventListener(ContextRefreshedEvent.class)
    public void warnIfInsecure(ContextRefreshedEvent event) {
        SecurityProperties props = event.getApplicationContext().getBean(SecurityProperties.class);
        if (props.getToken() == null || props.getToken().isBlank()) {
            log.warn("MCP auth token not configured – running in INSECURE dev mode");
        }
    }
}
