package com.org.hr.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit test — no Spring context, no DB.
 */
class RateLimiterTest {

    @Test
    @DisplayName("Allows requests up to the limit then blocks further requests")
    void allowsUpToLimitThenBlocks() {
        RateLimiter limiter = new RateLimiter(3);

        assertThat(limiter.tryAcquire("alice")).isTrue();
        assertThat(limiter.tryAcquire("alice")).isTrue();
        assertThat(limiter.tryAcquire("alice")).isTrue();
        // 4th request within the same window → rejected
        assertThat(limiter.tryAcquire("alice")).isFalse();
    }

    @Test
    @DisplayName("Tracks separate request counters per user")
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
    @DisplayName("Allows exactly one request when the limit is one")
    void limitOfOneAllowsExactlyOneRequest() {
        RateLimiter limiter = new RateLimiter(1);

        assertThat(limiter.tryAcquire("dave")).isTrue();
        assertThat(limiter.tryAcquire("dave")).isFalse();
    }
}
