package com.org.ai.config;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${assistant.security.api-key:${API_KEY:}}")
    private String apiKey;

    @Value("${assistant.security.enabled:true}")
    private boolean securityEnabled;

    /**
     * Fails fast when API-key auth is on but no key is configured. It used to skip the check in
     * that case, so "security enabled" silently meant "every caller is accepted".
     */
    @PostConstruct
    void requireKeyWhenEnabled() {
        if (securityEnabled && !StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("assistant.security.enabled=true but no API key is configured: "
                    + "set API_KEY=<secret> (clients send it in the X-API-Key header), "
                    + "or API_AUTH_ENABLED=false for local development.");
        }
    }

    /** Constant-time comparison, so response timing does not leak how much of the key matched. */
    private boolean keyMatches(String incomingKey) {
        return incomingKey != null && MessageDigest.isEqual(
                apiKey.getBytes(StandardCharsets.UTF_8), incomingKey.getBytes(StandardCharsets.UTF_8));
    }

    /** Defines the security filter chain bean. */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/actuator/prometheus"      // scraped without credentials; env/loggers stay protected
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(apiKeyFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private OncePerRequestFilter apiKeyFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain) throws ServletException, IOException {
                if (securityEnabled) {
                    String incomingKey = request.getHeader("X-API-Key");
                    if (!keyMatches(incomingKey)) {
                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                        response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Invalid or missing X-API-Key\"}");
                        return;
                    }
                }

                // Key check passed (or security disabled): populate the SecurityContext,
                // otherwise .anyRequest().authenticated() rejects the request as anonymous.
                var authentication = new UsernamePasswordAuthenticationToken(
                        "api-client", null, List.of(new SimpleGrantedAuthority("ROLE_API")));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                chain.doFilter(request, response);
            }
        };
    }
}
