package com.org.ai.exception;

/**
 * Raised when the MCP tool-calling pipeline fails — exceeding the bounded
 * tool-call iteration limit, failing to obtain an OAuth2 token for a
 * protected MCP server, or a tool invocation itself failing after retries
 * and circuit-breaker handling.
 */
public class ToolInvocationException extends RuntimeException {

    public ToolInvocationException(String message) {
        super(message);
    }

    public ToolInvocationException(String message, Throwable cause) {
        super(message, cause);
    }
}
