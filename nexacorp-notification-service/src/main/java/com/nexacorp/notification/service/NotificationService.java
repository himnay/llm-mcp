package com.nexacorp.notification.service;

import com.nexacorp.notification.model.Notification;
import com.nexacorp.notification.model.NotificationChannel;
import com.nexacorp.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Notification sendNotification(NotificationChannel channel,
                                         String recipient,
                                         String message) {

        Notification notification = Notification.builder()
                .channel(channel)
                .recipient(recipient)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();

        return notificationRepository.save(notification);
    }

    public List<Notification> getNotifications() {
        return notificationRepository.findAll();
    }
}