package com.org.github.exception;

public class InvalidToolArgumentException extends RuntimeException {
    public InvalidToolArgumentException(String message) {
        super(message);
    }

    public InvalidToolArgumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
