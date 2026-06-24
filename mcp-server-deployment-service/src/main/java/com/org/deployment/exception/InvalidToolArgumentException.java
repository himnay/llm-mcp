package com.org.deployment.exception;

/**
 * Thrown when an MCP tool receives an argument that fails validation
 * (null/blank/format/range checks). Mapped to HTTP 400 by {@link GlobalExceptionHandler}.
 */
public class InvalidToolArgumentException extends RuntimeException {

    public InvalidToolArgumentException(String message) {
        super(message);
    }

    public InvalidToolArgumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
