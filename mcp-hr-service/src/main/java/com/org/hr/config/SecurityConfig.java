package com.org.hr.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * Activates {@link SecurityProperties} and emits a startup warning when
 * MCP auth is not configured.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({SecurityProperties.class, McpOutputProperties.class})
public class SecurityConfig {

    private final SecurityProperties securityProperties;

    public SecurityConfig(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onStartup() {
        if (securityProperties.getToken() == null || securityProperties.getToken().isBlank()) {
            log.warn("MCP auth token not configured – running in INSECURE dev mode");
        }
    }
}
