package com.org.ticket.exception;

/**
 * Thrown when a write operation is rejected because the acting user is not
 * permitted to perform mutations (e.g. write-gate requires an explicit
 * X-Acting-User header). Mapped to HTTP 403 by {@link GlobalExceptionHandler}.
 */
public class WriteNotPermittedException extends RuntimeException {

    public WriteNotPermittedException(String message) {
        super(message);
    }

    public WriteNotPermittedException(String message, Throwable cause) {
        super(message, cause);
    }
}
