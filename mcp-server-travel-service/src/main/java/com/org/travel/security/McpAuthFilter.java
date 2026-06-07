package com.org.travel.security;

import com.org.travel.config.SecurityProperties;
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
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Security + rate-limit filter applied to every request.
 * <ul>
 *   <li>Skips {@code /actuator/health} and {@code /actuator/info} entirely.</li>
 *   <li>When {@code mcp.security.token} is non-blank, enforces Bearer auth.</li>
 *   <li>Extracts {@code X-Acting-User} header into {@link ActingUserContext}.</li>
 *   <li>Applies per-user fixed-window rate limiting.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_HEADER = "Authorization";
    private static final String ACTING_USER_HEADER = "X-Acting-User";

    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;
    private final RateLimiter rateLimiter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/health") || path.startsWith("/actuator/info");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = securityProperties.getToken();
        boolean authEnabled = token != null && !token.isBlank();

        if (authEnabled) {
            String authHeader = request.getHeader(AUTH_HEADER);
            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                writeError(response, HttpStatus.UNAUTHORIZED, "Missing or malformed Authorization header");
                return;
            }
            String provided = authHeader.substring(BEARER_PREFIX.length());
            if (!token.equals(provided)) {
                writeError(response, HttpStatus.UNAUTHORIZED, "Invalid MCP auth token");
                return;
            }
        }

        String actingUser = request.getHeader(ACTING_USER_HEADER);
        if (actingUser == null || actingUser.isBlank()) {
            actingUser = securityProperties.getDefaultUser();
        }
        ActingUserContext.set(actingUser);

        if (!rateLimiter.tryAcquire(actingUser)) {
            ActingUserContext.clear();
            log.warn("Rate limit exceeded | user={} path={}", actingUser, request.getRequestURI());
            writeError(response, HttpStatus.TOO_MANY_REQUESTS,
                    "Rate limit exceeded – maximum " + securityProperties.getRateLimitPerMinute()
                            + " requests per minute");
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            ActingUserContext.clear();
        }
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
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
