package com.org.notification.delivery;

import com.org.notification.model.Notification;
import com.org.notification.model.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Delivers INTERNAL notifications (in-app inbox; demo implementation logs the dispatch).
 */
@Slf4j
@Component
class InternalDeliveryStrategy implements ChannelDeliveryStrategy {

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.INTERNAL;
    }

    @Override
    public void deliver(Notification notification) {
        log.info("DELIVER channel=INTERNAL recipient={} id={}",
                notification.getRecipient(), notification.getId());
    }
}
