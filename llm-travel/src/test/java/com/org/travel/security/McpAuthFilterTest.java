package com.org.travel.security;

import com.org.travel.config.SecurityProperties;
import org.junit.jupiter.api.BeforeEach;
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

    @Test
    void allowsHealthWithoutToken() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(req, res, chain);
        assertThat(res.getStatus()).isEqualTo(200);
    }

    @Test
    void rejects401WhenTokenMissing() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/mcp");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        assertThat(res.getStatus()).isEqualTo(401);
    }

    @Test
    void rejects401WhenTokenWrong() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/mcp");
        req.addHeader("Authorization", "Bearer wrong-token");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        assertThat(res.getStatus()).isEqualTo(401);
    }

    @Test
    void allowsRequestWithCorrectToken() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/mcp");
        req.addHeader("Authorization", "Bearer secret-token");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(req, res, chain);
        assertThat(res.getStatus()).isEqualTo(200);
    }

    @Test
    void authDisabledWhenTokenBlank() throws Exception {
        props.setToken("");
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/mcp");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(req, res, chain);
        assertThat(res.getStatus()).isEqualTo(200);
    }

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
