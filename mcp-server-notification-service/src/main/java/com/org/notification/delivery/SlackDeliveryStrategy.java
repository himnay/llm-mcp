package com.org.notification.delivery;

import com.org.notification.model.Notification;
import com.org.notification.model.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Delivers SLACK notifications (demo implementation logs the dispatch; swap in a webhook call here). */
@Slf4j
@Component
class SlackDeliveryStrategy implements ChannelDeliveryStrategy {

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SLACK;
    }

    @Override
    public void deliver(Notification notification) {
        log.info("DELIVER channel=SLACK recipient={} id={}",
                notification.getRecipient(), notification.getId());
    }
}
