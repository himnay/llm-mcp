package com.org.ticket.exception;

/**
 * Thrown when a requested ticket status transition is not allowed.
 * Mapped to HTTP 400 by {@link GlobalExceptionHandler}.
 */
public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(String message) {
        super(message);
    }

    public InvalidStatusTransitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
