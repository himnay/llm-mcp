package com.org.deployment.security;

/**
 * Carries the acting user for the current request thread.
 * Set by {@link McpAuthFilter}, consumed by {@link com.org.deployment.mcp.DeploymentMcpTools}.
 * Always cleared in the filter's {@code finally} block.
 */
public final class ActingUserContext {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private ActingUserContext() {
    }

    /** Handles set. */
    public static void set(String user) {
        HOLDER.set(user);
    }

    /** Returns the get. */
    public static String get() {
        return HOLDER.get();
    }

    /** Clears. */
    public static void clear() {
        HOLDER.remove();
    }
}
