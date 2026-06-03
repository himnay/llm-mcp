package com.org.notification.controller;

import com.org.notification.model.Notification;
import com.org.notification.model.NotificationChannel;
import com.org.notification.service.NotificationService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public Notification sendNotification(@RequestParam @NotNull NotificationChannel channel,
                                         @RequestParam @NotBlank String recipient,
                                         @RequestParam @NotBlank String message) {

        return notificationService.sendNotification(channel, recipient, message);
    }

    @GetMapping
    public List<Notification> getNotifications() {
        return notificationService.getNotifications();
    }
}