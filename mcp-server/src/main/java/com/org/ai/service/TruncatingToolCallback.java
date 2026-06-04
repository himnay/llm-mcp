package com.org.ai.service;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * Wraps an MCP {@link ToolCallback} and caps the size of the tool result fed back
 * into the model context. Oversized MCP responses can blow the context window and
 * are a vector for context-stuffing, so they are truncated with a marker.
 */
public class TruncatingToolCallback implements ToolCallback {

    private static final String MARKER = "…[truncated]";

    private final ToolCallback delegate;
    private final int maxChars;

    public TruncatingToolCallback(ToolCallback delegate, int maxChars) {
        this.delegate = delegate;
        this.maxChars = maxChars;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        return truncate(delegate.call(toolInput));
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        return truncate(delegate.call(toolInput, toolContext));
    }

    private String truncate(String result) {
        if (result != null && maxChars > 0 && result.length() > maxChars) {
            return result.substring(0, maxChars) + MARKER;
        }
        return result;
    }
}
