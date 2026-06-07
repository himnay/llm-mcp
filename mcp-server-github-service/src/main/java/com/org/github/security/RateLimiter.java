package com.org.github.security;

import java.util.concurrent.ConcurrentHashMap;

public class RateLimiter {
    private final int limitPerMinute;
    private final ConcurrentHashMap<String, long[]> windows = new ConcurrentHashMap<>();

    public RateLimiter(int limitPerMinute) {
        this.limitPerMinute = limitPerMinute;
    }

    public boolean tryAcquire(String user) {
        long now = System.currentTimeMillis();
        long windowMs = 60_000L;
        long[] state = windows.computeIfAbsent(user, k -> new long[]{0, now});
        synchronized (state) {
            if (now - state[1] >= windowMs) {
                state[0] = 0;
                state[1] = now;
            }
            if (state[0] >= limitPerMinute) return false;
            state[0]++;
            return true;
        }
    }
}
