package com.org.travel.security;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed-window per-user rate limiter. Each window is 60 seconds. Thread-safe; no external deps.
 */
public class RateLimiter {

    private static final long WINDOW_MS = 60_000L;

    private final int maxPerMinute;
    private final ConcurrentHashMap<String, long[]> counters = new ConcurrentHashMap<>();

    public RateLimiter(int maxPerMinute) {
        this.maxPerMinute = maxPerMinute;
    }

    public boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        long[] entry = counters.compute(key, (k, existing) -> {
            if (existing == null || (now - existing[1]) >= WINDOW_MS) {
                return new long[]{1L, now};
            }
            existing[0]++;
            return existing;
        });
        return entry[0] <= maxPerMinute;
    }

    int currentCount(String key) {
        long[] entry = counters.get(key);
        if (entry == null) return 0;
        if ((System.currentTimeMillis() - entry[1]) >= WINDOW_MS) return 0;
        return (int) entry[0];
    }
}
