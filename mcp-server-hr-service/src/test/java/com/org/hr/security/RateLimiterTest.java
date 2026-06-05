package com.org.hr.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit test — no Spring context, no DB.
 */
class RateLimiterTest {

    @Test
    void allowsUpToLimitThenBlocks() {
        RateLimiter limiter = new RateLimiter(3);

        assertThat(limiter.tryAcquire("alice")).isTrue();
        assertThat(limiter.tryAcquire("alice")).isTrue();
        assertThat(limiter.tryAcquire("alice")).isTrue();
        // 4th request within the same window → rejected
        assertThat(limiter.tryAcquire("alice")).isFalse();
    }

    @Test
    void differentUsersHaveSeparateCounters() {
        RateLimiter limiter = new RateLimiter(2);

        assertThat(limiter.tryAcquire("bob")).isTrue();
        assertThat(limiter.tryAcquire("bob")).isTrue();
        assertThat(limiter.tryAcquire("bob")).isFalse(); // bob is exhausted

        // carol is independent
        assertThat(limiter.tryAcquire("carol")).isTrue();
        assertThat(limiter.tryAcquire("carol")).isTrue();
        assertThat(limiter.tryAcquire("carol")).isFalse();
    }

    @Test
    void limitOfOneAllowsExactlyOneRequest() {
        RateLimiter limiter = new RateLimiter(1);

        assertThat(limiter.tryAcquire("dave")).isTrue();
        assertThat(limiter.tryAcquire("dave")).isFalse();
    }
}
