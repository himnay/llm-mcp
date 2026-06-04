package com.org.deployment.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for {@link RateLimiter} — no Spring context, no DB.
 */
class RateLimiterTest {

    @Test
    void allowsUpToLimit() {
        RateLimiter limiter = new RateLimiter(3);
        assertThat(limiter.tryAcquire("alice")).isTrue();
        assertThat(limiter.tryAcquire("alice")).isTrue();
        assertThat(limiter.tryAcquire("alice")).isTrue();
    }

    @Test
    void blocksAboveLimit() {
        RateLimiter limiter = new RateLimiter(2);
        limiter.tryAcquire("bob");
        limiter.tryAcquire("bob");
        assertThat(limiter.tryAcquire("bob")).isFalse();
    }

    @Test
    void differentUsersHaveIndependentWindows() {
        RateLimiter limiter = new RateLimiter(1);
        assertThat(limiter.tryAcquire("carol")).isTrue();
        assertThat(limiter.tryAcquire("carol")).isFalse();
        // "dave" has a fresh window
        assertThat(limiter.tryAcquire("dave")).isTrue();
    }

    @Test
    void zeroLimitBlocksEveryone() {
        RateLimiter limiter = new RateLimiter(0);
        assertThat(limiter.tryAcquire("eve")).isFalse();
    }
}
