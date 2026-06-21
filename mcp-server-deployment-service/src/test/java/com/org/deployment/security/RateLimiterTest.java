package com.org.deployment.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for {@link RateLimiter} — no Spring context, no DB.
 */
class RateLimiterTest {

    @Test
    @DisplayName("Allows requests up to the configured limit")
    void allowsUpToLimit() {
        RateLimiter limiter = new RateLimiter(3);
        assertThat(limiter.tryAcquire("alice")).isTrue();
        assertThat(limiter.tryAcquire("alice")).isTrue();
        assertThat(limiter.tryAcquire("alice")).isTrue();
    }

    @Test
    @DisplayName("Blocks requests once the limit is exceeded")
    void blocksAboveLimit() {
        RateLimiter limiter = new RateLimiter(2);
        limiter.tryAcquire("bob");
        limiter.tryAcquire("bob");
        assertThat(limiter.tryAcquire("bob")).isFalse();
    }

    @Test
    @DisplayName("Tracks rate limit windows independently per user")
    void differentUsersHaveIndependentWindows() {
        RateLimiter limiter = new RateLimiter(1);
        assertThat(limiter.tryAcquire("carol")).isTrue();
        assertThat(limiter.tryAcquire("carol")).isFalse();
        // "dave" has a fresh window
        assertThat(limiter.tryAcquire("dave")).isTrue();
    }

    @Test
    @DisplayName("Blocks every request when limit is zero")
    void zeroLimitBlocksEveryone() {
        RateLimiter limiter = new RateLimiter(0);
        assertThat(limiter.tryAcquire("eve")).isFalse();
    }
}
