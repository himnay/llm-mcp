package com.org.travel.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    @Test
    void allowsRequestsUpToLimit() {
        RateLimiter limiter = new RateLimiter(3);
        assertThat(limiter.tryAcquire("user1")).isTrue();
        assertThat(limiter.tryAcquire("user1")).isTrue();
        assertThat(limiter.tryAcquire("user1")).isTrue();
        assertThat(limiter.tryAcquire("user1")).isFalse();
    }

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

    @Test
    void tracksCurrentCount() {
        RateLimiter limiter = new RateLimiter(10);
        limiter.tryAcquire("user");
        limiter.tryAcquire("user");
        assertThat(limiter.currentCount("user")).isEqualTo(2);
    }

    @Test
    void unknownUserCountIsZero() {
        RateLimiter limiter = new RateLimiter(10);
        assertThat(limiter.currentCount("nobody")).isEqualTo(0);
    }
}
