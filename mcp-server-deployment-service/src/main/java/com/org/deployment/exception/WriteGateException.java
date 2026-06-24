package com.org.deployment.exception;

/**
 * Thrown when a write/mutating operation is rejected because the acting user
 * does not satisfy the configured write-gate requirements. Mapped to HTTP 403
 * by {@link GlobalExceptionHandler}.
 */
public class WriteGateException extends RuntimeException {

    public WriteGateException(String message) {
        super(message);
    }

    public WriteGateException(String message, Throwable cause) {
        super(message, cause);
    }
}
