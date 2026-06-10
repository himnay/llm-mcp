package com.org.notification.service;

import com.org.notification.delivery.DeliveryStrategyRegistry;
import com.org.notification.model.Notification;
import com.org.notification.model.NotificationChannel;
import com.org.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final DeliveryStrategyRegistry deliveryStrategies;

    public Notification sendNotification(NotificationChannel channel,
                                         String recipient,
                                         String message) {

        Notification notification = Notification.builder()
                .channel(channel)
                .recipient(recipient)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();

        Notification saved = notificationRepository.save(notification);
        deliveryStrategies.strategyFor(channel).deliver(saved);
        return saved;
    }

    public List<Notification> getNotifications() {
        return notificationRepository.findAll();
    }
}