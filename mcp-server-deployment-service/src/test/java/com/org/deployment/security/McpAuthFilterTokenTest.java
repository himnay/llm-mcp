package com.org.deployment.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for {@link McpAuthFilter} token validation — no Spring context, no DB.
 */
@ExtendWith(MockitoExtension.class)
class McpAuthFilterTokenTest {

    @Mock
    private FilterChain filterChain;

    private SecurityProperties props;
    private McpAuthFilter filter;

    @BeforeEach
    void setUp() {
        props = new SecurityProperties();
        props.setRateLimitPerMinute(1000);
        RateLimiter rateLimiter = new RateLimiter(props.getRateLimitPerMinute());
        filter = new McpAuthFilter(props, new ObjectMapper(), rateLimiter);
    }

    @Test
    @DisplayName("Permits health endpoint access without a token")
    void permitsHealthWithoutToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Permits info endpoint access without a token")
    void permitsInfoWithoutToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/info");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Permits all requests when no token is configured")
    void permitsAllWhenTokenBlank() throws Exception {
        props.setToken("");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Rejects with 401 when a token is required but missing")
    void rejects401WhenTokenConfiguredButMissing() throws Exception {
        props.setToken("secret-token");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(filterChain);
    }

    @Test
    @DisplayName("Rejects with 401 when the bearer token does not match")
    void rejects401WhenTokenMismatch() throws Exception {
        props.setToken("secret-token");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
        request.addHeader("Authorization", "Bearer wrong-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(filterChain);
    }

    @Test
    @DisplayName("Permits the request when the bearer token matches")
    void permitsWhenTokenMatches() throws Exception {
        props.setToken("secret-token");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
        request.addHeader("Authorization", "Bearer secret-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Forwards the request to the chain when acting user header is set")
    void setsActingUserFromHeader() throws Exception {
        props.setToken("");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
        request.addHeader("X-Acting-User", "jane");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        // ActingUserContext is cleared after the chain, but we can confirm the chain was invoked
        verify(filterChain).doFilter(request, response);
    }
}
