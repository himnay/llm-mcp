package com.nexacorp.notification.controller;

import com.nexacorp.notification.model.Notification;
import com.nexacorp.notification.model.NotificationChannel;
import com.nexacorp.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public Notification sendNotification(@RequestParam NotificationChannel channel,
                                         @RequestParam String recipient,
                                         @RequestParam String message) {

        return notificationService.sendNotification(channel, recipient, message);
    }

    @GetMapping
    public List<Notification> getNotifications() {
        return notificationService.getNotifications();
    }
}