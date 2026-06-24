package com.org.notification.exception;

/**
 * Thrown when notification delivery infrastructure is misconfigured, e.g. a
 * {@link com.org.notification.model.NotificationChannel} has no registered
 * {@code ChannelDeliveryStrategy}. Typically surfaces at startup.
 * Mapped to HTTP 500 by {@link GlobalExceptionHandler} (no recovery possible
 * by the caller; this indicates a deployment/wiring defect).
 */
public class DeliveryConfigurationException extends RuntimeException {

    public DeliveryConfigurationException(String message) {
        super(message);
    }

    public DeliveryConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
