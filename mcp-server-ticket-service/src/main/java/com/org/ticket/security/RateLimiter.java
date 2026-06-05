package com.org.ticket.security;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory, per-user sliding-window rate limiter (1-minute window).
 * Thread-safe via {@link ConcurrentHashMap} + {@link AtomicInteger}.
 */
public class RateLimiter {

    private final int limitPerMinute;

    /**
     * key = user, value = [requestCount, windowStartMs] guarded by sync on the value array.
     */
    private final ConcurrentHashMap<String, long[]> windows = new ConcurrentHashMap<>();

    public RateLimiter(int limitPerMinute) {
        this.limitPerMinute = limitPerMinute;
    }

    /**
     * Returns {@code true} if the request is allowed; {@code false} if the user has exceeded
     * {@code limitPerMinute} requests in the current 1-minute window.
     */
    public boolean tryAcquire(String user) {
        long now = System.currentTimeMillis();
        long windowMs = 60_000L;

        long[] state = windows.computeIfAbsent(user, k -> new long[]{0, now});

        synchronized (state) {
            // reset window if expired
            if (now - state[1] >= windowMs) {
                state[0] = 0;
                state[1] = now;
            }
            if (state[0] >= limitPerMinute) {
                return false;
            }
            state[0]++;
            return true;
        }
    }
}
