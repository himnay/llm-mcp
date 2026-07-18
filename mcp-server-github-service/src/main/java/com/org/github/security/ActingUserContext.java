package com.org.github.security;

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
