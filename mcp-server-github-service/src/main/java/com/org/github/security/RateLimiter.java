package com.org.github.security;

import java.util.concurrent.ConcurrentHashMap;

public class RateLimiter {
    private static final int WRITE_LIMIT_PER_MINUTE = 10;

    private final int limitPerMinute;
    private final ConcurrentHashMap<String, long[]> windows = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, long[]> writeWindows = new ConcurrentHashMap<>();

    public RateLimiter(int limitPerMinute) {
        this.limitPerMinute = limitPerMinute;
    }

    public boolean tryAcquire(String user) {
        return acquire(user, windows, limitPerMinute);
    }

    /**
     * Stricter rate limit for write/mutation operations.
     * Enforces a hard limit of {@value WRITE_LIMIT_PER_MINUTE} writes per minute per user.
     */
    public boolean tryAcquireWrite(String user) {
        return acquire(user, writeWindows, WRITE_LIMIT_PER_MINUTE);
    }

    private boolean acquire(String user, ConcurrentHashMap<String, long[]> windowMap, int limit) {
        long now = System.currentTimeMillis();
        long windowMs = 60_000L;
        long[] state = windowMap.computeIfAbsent(user, k -> new long[]{0, now});
        synchronized (state) {
            if (now - state[1] >= windowMs) {
                state[0] = 0;
                state[1] = now;
            }
            if (state[0] >= limit) return false;
            state[0]++;
            return true;
        }
    }
}
