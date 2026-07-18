package com.org.hr.security;

/**
 * ThreadLocal holder for the acting user propagated via {@code X-Acting-User}
 * request header.  The filter sets this before the handler runs and clears it
 * in a {@code finally} block to prevent context leakage.
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
