package com.org.travel.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    @DisplayName("Allows requests up to the configured limit then rejects further ones")
    @Test
    void allowsRequestsUpToLimit() {
        RateLimiter limiter = new RateLimiter(3);
        assertThat(limiter.tryAcquire("user1")).isTrue();
        assertThat(limiter.tryAcquire("user1")).isTrue();
        assertThat(limiter.tryAcquire("user1")).isTrue();
        assertThat(limiter.tryAcquire("user1")).isFalse();
    }

    @DisplayName("Tracks request counts independently per user")
    @Test
    void isolatesCountsByUser() {
        RateLimiter limiter = new RateLimiter(2);
        assertThat(limiter.tryAcquire("alice")).isTrue();
        assertThat(limiter.tryAcquire("alice")).isTrue();
        assertThat(limiter.tryAcquire("alice")).isFalse();
        // bob's window is independent
        assertThat(limiter.tryAcquire("bob")).isTrue();
        assertThat(limiter.tryAcquire("bob")).isTrue();
    }

    @DisplayName("Reports the current acquisition count for a user")
    @Test
    void tracksCurrentCount() {
        RateLimiter limiter = new RateLimiter(10);
        limiter.tryAcquire("user");
        limiter.tryAcquire("user");
        assertThat(limiter.currentCount("user")).isEqualTo(2);
    }

    @DisplayName("Returns zero count for a user that has never made a request")
    @Test
    void unknownUserCountIsZero() {
        RateLimiter limiter = new RateLimiter(10);
        assertThat(limiter.currentCount("nobody")).isEqualTo(0);
    }
}
