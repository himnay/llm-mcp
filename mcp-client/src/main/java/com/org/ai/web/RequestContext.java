package com.org.ai.web;

import java.util.concurrent.Callable;

/**
 * Per-request context propagated on the request thread. Holds the acting user
 * (so it can be forwarded to downstream MCP servers as {@code X-Acting-User}),
 * the conversation id (for chat memory) and whether write/destructive tools are
 * permitted for this request.
 */
public final class RequestContext {

    private static final ThreadLocal<Ctx> HOLDER = new ThreadLocal<>();

    private RequestContext() {
    }

    /** Handles set. */
    public static void set(String user, String conversationId, boolean allowWriteTools) {
        HOLDER.set(new Ctx(user, conversationId, allowWriteTools));
    }

    /**
     * Update conversation id / write flag while preserving the resolved user.
     */
    public static void update(String conversationId, boolean allowWriteTools) {
        Ctx current = HOLDER.get();
        String user = current == null ? null : current.user();
        HOLDER.set(new Ctx(user, conversationId, allowWriteTools));
    }

    /** Returns the user. */
    public static String user() {
        Ctx c = HOLDER.get();
        return c == null ? null : c.user();
    }

    /** Returns the conversation id. */
    public static String conversationId() {
        Ctx c = HOLDER.get();
        return c == null ? null : c.conversationId();
    }

    /** Returns the allow write tools. */
    public static boolean allowWriteTools() {
        Ctx c = HOLDER.get();
        return c != null && c.allowWriteTools();
    }

    /** Clears. */
    public static void clear() {
        HOLDER.remove();
    }

    /**
     * Wraps {@code task} so it runs with the calling thread's context. A thread-local is not
     * inherited by a pooled or virtual worker thread, so without this a task handed to an executor
     * sees no acting user at all.
     */
    public static <T> Callable<T> propagate(Callable<T> task) {
        Ctx captured = HOLDER.get();
        return () -> {
            Ctx previous = HOLDER.get();
            if (captured == null) {
                HOLDER.remove();
            } else {
                HOLDER.set(captured);
            }
            try {
                return task.call();
            } finally {
                if (previous == null) {
                    HOLDER.remove();
                } else {
                    HOLDER.set(previous);
                }
            }
        };
    }

    private record Ctx(String user, String conversationId, boolean allowWriteTools) {
    }
}
