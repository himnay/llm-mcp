package com.org.hr.exception;

/**
 * Thrown when an MCP tool receives an argument that is missing, blank, or
 * otherwise fails validation (e.g. malformed date).
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
