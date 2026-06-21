package com.org.notification.delivery;

import com.org.notification.model.Notification;
import com.org.notification.model.NotificationChannel;

/**
 * Strategy (GoF): one implementation per {@link NotificationChannel}.
 * {@code NotificationService} picks the right strategy at runtime via
 * {@link DeliveryStrategyRegistry}, so adding a channel means adding a new
 * strategy bean — no switch statements to touch (Open/Closed Principle).
 */
public interface ChannelDeliveryStrategy {

    /**
     * The channel this strategy handles.
     */
    NotificationChannel channel();

    /**
     * Deliver the (already persisted) notification over this channel.
     */
    void deliver(Notification notification);
}
