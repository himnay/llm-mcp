package com.org.gmail.security;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

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
    @DisplayName("Permits health check requests without requiring a token")
    void permitsHealthWithoutToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, filterChain);
        assertThat(response.getStatus()).isEqualTo(200);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Allows requests through when no token is configured")
    void allowsRequestWhenNoTokenConfigured() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, filterChain);
        assertThat(response.getStatus()).isEqualTo(200);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Rejects requests with an incorrect bearer token with 401")
    void rejectsRequestWithWrongToken() throws Exception {
        props.setToken("correct-token");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
        request.addHeader("Authorization", "Bearer wrong-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, filterChain);
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("Accepts requests with the correct bearer token")
    void acceptsRequestWithCorrectToken() throws Exception {
        props.setToken("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
        request.addHeader("Authorization", "Bearer secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, filterChain);
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
