package com.org.github.mcp;

import com.org.github.exception.ResourceNotFoundException;
import com.org.github.security.RateLimiter;
import com.org.github.security.SecurityProperties;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolExecutionTemplateFallbackTest {

    private final ToolExecutionTemplate template = new ToolExecutionTemplate(new SecurityProperties(), new RateLimiter(60));

    @Test
    @DisplayName("GitHub outages (open circuit, 5xx, network) get the friendly 'unavailable' answer")
    void outagesUseFallback() {
        CallNotPermittedException open = CallNotPermittedException.createCallNotPermittedException(CircuitBreaker.ofDefaults("github-api"));
        for (Throwable outage : new Throwable[]{open,
                HttpServerErrorException.create(HttpStatus.BAD_GATEWAY, "bad gateway", null, null, null),
                new ResourceAccessException("connect timed out")}) {
            assertThat(template.githubFallback("getRepository", "", () -> "", outage)).contains("temporarily unavailable");
        }
    }

    @Test
    @DisplayName("Write-gate refusals and 404s are rethrown instead of being reported as an outage")
    void callerErrorsAreRethrown() {
        IllegalStateException gate = new IllegalStateException("Write operations require an explicit X-Acting-User header.");
        assertThatThrownBy(() -> template.githubFallback("createIssue", "", () -> "", gate)).isSameAs(gate);

        ResourceNotFoundException missing = new ResourceNotFoundException("Repository a/b not found");
        assertThatThrownBy(() -> template.githubFallback("getRepository", "", () -> "", missing)).isSameAs(missing);
    }
}
