package com.org.travel.config;

public final class ToolOutputUtil {

    private ToolOutputUtil() {
    }

    public static String cap(String output, int maxChars) {
        if (output == null) return "";
        if (output.length() <= maxChars) return output;
        return output.substring(0, maxChars) + "\n… [truncated — output exceeded " + maxChars + " chars]";
    }
}
