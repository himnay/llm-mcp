package com.org.travel.security;

import com.org.travel.config.SecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class McpAuthFilterTest {

    private SecurityProperties props;
    private McpAuthFilter filter;

    @BeforeEach
    void setUp() {
        props = new SecurityProperties();
        props.setToken("secret-token");
        props.setDefaultUser("system");
        props.setRateLimitPerMinute(120);

        filter = new McpAuthFilter(props, new ObjectMapper(), new RateLimiter(120));
    }

    @DisplayName("Allows health endpoint requests without an auth token")
    @Test
    void allowsHealthWithoutToken() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(req, res, chain);
        assertThat(res.getStatus()).isEqualTo(200);
    }

    @DisplayName("Rejects with 401 when the Authorization header is missing")
    @Test
    void rejects401WhenTokenMissing() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/mcp");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        assertThat(res.getStatus()).isEqualTo(401);
    }

    @DisplayName("Rejects with 401 when the bearer token does not match")
    @Test
    void rejects401WhenTokenWrong() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/mcp");
        req.addHeader("Authorization", "Bearer wrong-token");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        assertThat(res.getStatus()).isEqualTo(401);
    }

    @DisplayName("Allows the request through when the bearer token matches")
    @Test
    void allowsRequestWithCorrectToken() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/mcp");
        req.addHeader("Authorization", "Bearer secret-token");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(req, res, chain);
        assertThat(res.getStatus()).isEqualTo(200);
    }

    @DisplayName("Skips authentication entirely when the configured token is blank")
    @Test
    void authDisabledWhenTokenBlank() throws Exception {
        props.setToken("");
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/mcp");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(req, res, chain);
        assertThat(res.getStatus()).isEqualTo(200);
    }

    @DisplayName("Sets the acting user context from the X-Acting-User header")
    @Test
    void setsActingUserFromHeader() throws Exception {
        props.setToken("");
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/mcp");
        req.addHeader("X-Acting-User", "alice");
        MockHttpServletResponse res = new MockHttpServletResponse();
        final String[] captured = {null};
        filter.doFilter(req, res, (rq, rs) -> captured[0] = ActingUserContext.get());
        assertThat(captured[0]).isEqualTo("alice");
    }
}
