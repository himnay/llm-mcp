package com.org.notification.exception;

/**
 * Thrown when an MCP tool invocation receives invalid, missing, or
 * otherwise unacceptable input (including caller/permission preconditions
 * on a write operation).
 * Mapped to HTTP 400 by {@link GlobalExceptionHandler}.
 */
public class InvalidToolArgumentException extends RuntimeException {

    public InvalidToolArgumentException(String message) {
        super(message);
    }

    public InvalidToolArgumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
