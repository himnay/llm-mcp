package com.org.ai.web;

import com.org.ai.config.AssistantProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Establishes {@link RequestContext} for each request: resolves the acting user
 * from the {@code X-User-Id} header (falling back to the configured default) and
 * applies a per-user rate limit to the {@code /chat} endpoints. Always clears the
 * thread-local context in a finally block.
 */
@Component
@RequiredArgsConstructor
public class RequestContextFilter extends OncePerRequestFilter {

    private final AssistantProperties properties;
    private final RateLimiter rateLimiter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String headerUser = request.getHeader("X-User-Id");
        String user = StringUtils.hasText(headerUser) ? headerUser.trim() : properties.getDefaultUser();
        String path = request.getRequestURI();

        try {
            if (path.startsWith("/chat") && !rateLimiter.tryAcquire(user, properties.getRateLimitPerMinute())) {
                writeError(response, HttpStatus.TOO_MANY_REQUESTS,
                        "Rate limit exceeded (" + properties.getRateLimitPerMinute() + "/min) for user " + user);
                return;
            }
            RequestContext.set(user, "default", false);
            chain.doFilter(request, response);
        } finally {
            RequestContext.clear();
        }
    }

    private static void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String body = "{\"status\":" + status.value()
                + ",\"error\":\"" + status.getReasonPhrase() + "\""
                + ",\"message\":\"" + message.replace("\"", "'") + "\""
                + ",\"timestamp\":" + System.currentTimeMillis() + "}";
        response.getWriter().write(body);
    }
}
