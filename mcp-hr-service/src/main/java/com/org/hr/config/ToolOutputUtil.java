package com.org.hr.config;

/**
 * Utility for capping MCP tool output to avoid overwhelming the LLM context window.
 */
public final class ToolOutputUtil {

    private static final String TRUNCATION_SUFFIX = "…[truncated]";

    private ToolOutputUtil() {
    }

    /**
     * Returns {@code text} unchanged if it is at or below {@code maxChars};
     * otherwise truncates and appends {@link #TRUNCATION_SUFFIX}.
     */
    public static String cap(String text, int maxChars) {
        if (text == null || text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars) + TRUNCATION_SUFFIX;
    }
}
