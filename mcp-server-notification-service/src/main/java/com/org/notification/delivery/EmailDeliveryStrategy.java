package com.org.notification.delivery;

import com.org.notification.model.Notification;
import com.org.notification.model.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Delivers EMAIL notifications (demo implementation logs the dispatch; swap in SMTP here).
 */
@Slf4j
@Component
class EmailDeliveryStrategy implements ChannelDeliveryStrategy {

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void deliver(Notification notification) {
        log.info("DELIVER channel=EMAIL recipient={} id={}",
                notification.getRecipient(), notification.getId());
    }
}
