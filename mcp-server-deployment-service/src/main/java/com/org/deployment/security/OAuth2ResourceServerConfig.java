package com.org.deployment.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

/**
 * OAuth2.1 resource server: this server now requires a Keycloak-issued JWT bearer token
 * (client-credentials grant) instead of the legacy shared-secret bearer token that
 * {@link McpAuthFilter} used to enforce. {@code McpAuthFilter} is left in place purely for
 * its other two jobs — {@code X-Acting-User} extraction and rate limiting — which run after
 * this filter chain has already authenticated the request, since {@code mcp.security.token}
 * is left blank for this service.
 *
 * <p>Tokens are required to: be issued by the configured Keycloak realm (standard issuer
 * validation), carry an {@code aud} claim containing {@code deployment-service} (so a token
 * minted for a different MCP server can't be replayed here), and carry the
 * {@code deployment-invoke} scope (mapped by Spring Security's default JWT converter to the
 * {@code SCOPE_deployment-invoke} authority).
 *
 * <p>{@code jwtDecoder} is wrapped in {@link SupplierJwtDecoder} — the same deferred-construction
 * wrapper Spring Boot's own autoconfiguration uses — so the Keycloak issuer-discovery HTTP call
 * happens lazily on first token validation, not eagerly at context startup. {@code @ConfigurationProperty}
 * {@code mcp.security.oauth2.enabled=false} (set in the test profile) is an extra, explicit
 * kill switch that drops this filter chain and decoder entirely.
 */
@Configuration
@ConditionalOnProperty(prefix = "mcp.security.oauth2", name = "enabled", matchIfMissing = true)
public class OAuth2ResourceServerConfig {

    private static final String REQUIRED_SCOPE_AUTHORITY = "SCOPE_deployment-invoke";
    private static final String REQUIRED_AUDIENCE = "deployment-service";

    @Bean
    public SecurityFilterChain oauth2ResourceServerFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/mcp/**", "/mcp")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().hasAuthority(REQUIRED_SCOPE_AUTHORITY))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                }));
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {
        return new SupplierJwtDecoder(() -> buildDecoder(issuerUri));
    }

    private NimbusJwtDecoder buildDecoder(String issuerUri) {
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(issuerUri);

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> withAudience = new JwtClaimValidator<List<String>>(
                "aud", audience -> audience != null && audience.contains(REQUIRED_AUDIENCE));
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience));

        return jwtDecoder;
    }
}
