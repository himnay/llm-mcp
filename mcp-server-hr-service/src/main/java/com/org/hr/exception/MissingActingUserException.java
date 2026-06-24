package com.org.hr.exception;

/**
 * Thrown when a write operation requires an identified acting user
 * (e.g. via the X-Acting-User header) but none was provided.
 * Mapped to HTTP 400 by {@link GlobalExceptionHandler}.
 */
public class MissingActingUserException extends RuntimeException {

    public MissingActingUserException(String message) {
        super(message);
    }

    public MissingActingUserException(String message, Throwable cause) {
        super(message, cause);
    }
}
