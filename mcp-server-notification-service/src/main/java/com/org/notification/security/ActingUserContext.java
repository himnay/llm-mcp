package com.org.notification.security;

/**
 * Carries the acting user for the current request thread.
 * Set by {@link McpAuthFilter}, consumed by {@link com.org.notification.mcp.NotificationTools}.
 * Always cleared in the filter's {@code finally} block.
 */
public final class ActingUserContext {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private ActingUserContext() {}

    public static void set(String user) {
        HOLDER.set(user);
    }

    public static String get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
