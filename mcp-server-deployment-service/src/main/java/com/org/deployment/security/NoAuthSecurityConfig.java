package com.org.deployment.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Fallback when {@code mcp.security.oauth2.enabled=false}: without this chain, Spring Boot's
 * default security auto-configuration would still lock every endpoint behind form login.
 * With it, the service behaves like the other MCP servers — open by default, protected only
 * by {@link McpAuthFilter}'s shared bearer token when {@code mcp.security.token} is set.
 */
@Configuration
@ConditionalOnProperty(prefix = "mcp.security.oauth2", name = "enabled", havingValue = "false")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class NoAuthSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(NoAuthSecurityConfig.class);

    @Bean
    public SecurityFilterChain permitAllFilterChain(HttpSecurity http) throws Exception {
        log.warn("OAuth2 resource server DISABLED (mcp.security.oauth2.enabled=false) — "
                + "MCP endpoint protected only by the shared-token McpAuthFilter, like the other servers. "
                + "Set MCP_OAUTH2_ENABLED=true to require Keycloak JWTs.");
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
