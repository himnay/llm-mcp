package com.org.github.mcp;

final class OutputSizeCapUtil {
    static final int DEFAULT_MAX_CHARS = 8_000;
    private static final String TRUNCATION_SUFFIX = "…[truncated]";

    private OutputSizeCapUtil() {}

    static String cap(String text) {
        return cap(text, DEFAULT_MAX_CHARS);
    }

    static String cap(String text, int maxChars) {
        if (text == null || text.length() <= maxChars) return text;
        return text.substring(0, maxChars - TRUNCATION_SUFFIX.length()) + TRUNCATION_SUFFIX;
    }
}
