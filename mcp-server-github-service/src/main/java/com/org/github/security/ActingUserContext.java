package com.org.github.security;

public final class ActingUserContext {
    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private ActingUserContext() {
    }

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
