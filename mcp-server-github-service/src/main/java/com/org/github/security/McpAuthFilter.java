package com.org.github.security;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class McpAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ACTING_USER_HEADER = "X-Acting-User";

    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;
    private final RateLimiter rateLimiter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (isPermitted(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String actingUser = request.getHeader(ACTING_USER_HEADER);
        if (actingUser == null || actingUser.isBlank()) {
            actingUser = securityProperties.getDefaultUser();
        }

        String configuredToken = securityProperties.getToken();
        if (configuredToken != null && !configuredToken.isBlank()) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                writeError(response, HttpStatus.UNAUTHORIZED, "Missing or malformed Authorization header");
                return;
            }
            String provided = authHeader.substring(BEARER_PREFIX.length());
            if (!configuredToken.equals(provided)) {
                writeError(response, HttpStatus.UNAUTHORIZED, "Invalid bearer token");
                return;
            }
        }

        if (!rateLimiter.tryAcquire(actingUser)) {
            log.warn("Rate limit exceeded | user={} path={}", actingUser, path);
            writeError(response, HttpStatus.TOO_MANY_REQUESTS,
                    "Rate limit exceeded. Maximum " + securityProperties.getRateLimitPerMinute() + " requests per minute.");
            return;
        }

        ActingUserContext.set(actingUser);
        try {
            filterChain.doFilter(request, response);
        } finally {
            ActingUserContext.clear();
        }
    }

    private boolean isPermitted(String path) {
        return path.equals("/actuator/health") || path.startsWith("/actuator/health/") || path.equals("/actuator/info");
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("timestamp", Instant.now().toEpochMilli());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
