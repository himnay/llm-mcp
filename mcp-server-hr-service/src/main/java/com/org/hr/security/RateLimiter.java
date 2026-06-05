package com.org.hr.security;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple fixed-window per-user rate limiter backed by a {@link ConcurrentHashMap}.
 * Each window is 60 seconds.  Thread-safe; no external dependencies.
 */
public class RateLimiter {

    private static final long WINDOW_MS = 60_000L;

    private final int maxPerMinute;

    /**
     * key → [count, windowStartMs] stored in a small long[2] array
     */
    private final ConcurrentHashMap<String, long[]> counters = new ConcurrentHashMap<>();

    public RateLimiter(int maxPerMinute) {
        this.maxPerMinute = maxPerMinute;
    }

    /**
     * Attempts to consume one request token for {@code key}.
     *
     * @return {@code true} if the request is within the allowed rate,
     * {@code false} if it should be rejected (429).
     */
    public boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        long[] entry = counters.compute(key, (k, existing) -> {
            if (existing == null || (now - existing[1]) >= WINDOW_MS) {
                // new or expired window — reset
                return new long[]{1L, now};
            }
            existing[0]++;
            return existing;
        });
        return entry[0] <= maxPerMinute;
    }

    /**
     * Exposed for tests — returns current count within active window.
     */
    int currentCount(String key) {
        long[] entry = counters.get(key);
        if (entry == null) return 0;
        if ((System.currentTimeMillis() - entry[1]) >= WINDOW_MS) return 0;
        return (int) entry[0];
    }
}
