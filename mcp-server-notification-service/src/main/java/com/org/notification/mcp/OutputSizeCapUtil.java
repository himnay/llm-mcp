package com.org.notification.mcp;

/**
 * Guards against oversized MCP tool responses flooding the LLM context window.
 */
final class OutputSizeCapUtil {

    static final int DEFAULT_MAX_CHARS = 8_000;
    private static final String TRUNCATION_SUFFIX = "…[truncated]";

    private OutputSizeCapUtil() {
    }

    /**
     * Returns {@code text} unchanged if it is within {@link #DEFAULT_MAX_CHARS};
     * otherwise returns the first {@code maxChars} characters followed by
     * {@value #TRUNCATION_SUFFIX}.
     */
    static String cap(String text) {
        return cap(text, DEFAULT_MAX_CHARS);
    }

    static String cap(String text, int maxChars) {
        if (text == null || text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars) + TRUNCATION_SUFFIX;
    }
}
