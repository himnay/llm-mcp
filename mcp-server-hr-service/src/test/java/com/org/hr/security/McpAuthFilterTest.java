package com.org.hr.security;

import com.org.hr.config.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.Mockito.*;

/**
 * Pure unit test for {@link McpAuthFilter} — no Spring context, no DB.
 */
@ExtendWith(MockitoExtension.class)
class McpAuthFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private SecurityProperties props;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        props = new SecurityProperties();
        objectMapper = new ObjectMapper();
    }

    // ------------------------------------------------------------------
    // Auth disabled (blank token) — filter passes through
    // ------------------------------------------------------------------

    @DisplayName("Passes request through when the configured token is blank")
    @Test
    void whenTokenBlank_requestPassesThrough() throws Exception {
        props.setToken("");
        RateLimiter limiter = new RateLimiter(120);
        McpAuthFilter filter = new McpAuthFilter(props, objectMapper, limiter);

        when(request.getHeader("X-Acting-User")).thenReturn("alice");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoMoreInteractions(response);
    }

    // ------------------------------------------------------------------
    // Auth enabled — correct token passes
    // ------------------------------------------------------------------

    @DisplayName("Passes request through when the bearer token matches the configured token")
    @Test
    void whenTokenConfigured_correctTokenPasses() throws Exception {
        props.setToken("secret-token");
        RateLimiter limiter = new RateLimiter(120);
        McpAuthFilter filter = new McpAuthFilter(props, objectMapper, limiter);

        when(request.getHeader("Authorization")).thenReturn("Bearer secret-token");
        when(request.getHeader("X-Acting-User")).thenReturn("alice");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    // ------------------------------------------------------------------
    // Auth enabled — wrong token → 401
    // ------------------------------------------------------------------

    @DisplayName("Returns 401 when the bearer token does not match the configured token")
    @Test
    void whenTokenConfigured_wrongTokenReturns401() throws Exception {
        props.setToken("secret-token");
        RateLimiter limiter = new RateLimiter(120);
        McpAuthFilter filter = new McpAuthFilter(props, objectMapper, limiter);

        StringWriter sw = new StringWriter();
        when(request.getHeader("Authorization")).thenReturn("Bearer wrong-token");
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(401);
        verify(filterChain, never()).doFilter(any(), any());
    }

    // ------------------------------------------------------------------
    // Auth enabled — missing Authorization header → 401
    // ------------------------------------------------------------------

    @DisplayName("Returns 401 when the Authorization header is missing")
    @Test
    void whenTokenConfigured_missingAuthHeaderReturns401() throws Exception {
        props.setToken("secret-token");
        RateLimiter limiter = new RateLimiter(120);
        McpAuthFilter filter = new McpAuthFilter(props, objectMapper, limiter);

        StringWriter sw = new StringWriter();
        when(request.getHeader("Authorization")).thenReturn(null);
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(401);
        verify(filterChain, never()).doFilter(any(), any());
    }

    // ------------------------------------------------------------------
    // Rate limit exceeded → 429
    // ------------------------------------------------------------------

    @DisplayName("Returns 429 and blocks the filter chain when the rate limit is exceeded")
    @Test
    void whenRateLimitExceeded_returns429() throws Exception {
        props.setToken("");
        RateLimiter limiter = new RateLimiter(1); // allow only 1 per minute
        McpAuthFilter filter = new McpAuthFilter(props, objectMapper, limiter);

        when(request.getRequestURI()).thenReturn("/mcp/tools");
        when(request.getHeader("X-Acting-User")).thenReturn("alice");

        // First request must succeed
        filter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);

        // Second request must be rate-limited
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilterInternal(request, response, filterChain);
        verify(response).setStatus(429);
        // filterChain.doFilter was not called a second time
        verify(filterChain, times(1)).doFilter(any(), any());
    }
}
