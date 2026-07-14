package com.org.ai.web;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Minimal in-memory fixed-window rate limiter keyed by an arbitrary string
 * (here, the acting user). Not distributed — good enough for a single node and
 * to demonstrate the control; swap for a shared store (Redis/Bucket4j) in prod.
 */
@Component
public class RateLimiter {

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * @return {@code true} if the call is allowed, {@code false} if the per-minute
     * quota for {@code key} is exhausted.
     */
    public boolean tryAcquire(String key, int maxPerMinute) {
        if (maxPerMinute <= 0) {
            return true;
        }
        long minute = Instant.now().getEpochSecond() / 60;
        Window window = windows.compute(key, (k, cur) ->
                (cur == null || cur.epochMinute() != minute) ? new Window(minute, new AtomicInteger(0)) : cur);
        return window.count().incrementAndGet() <= maxPerMinute;
    }

    private record Window(long epochMinute, AtomicInteger count) {
    }
}
