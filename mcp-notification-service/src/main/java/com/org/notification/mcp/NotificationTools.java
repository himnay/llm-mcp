package com.org.notification.mcp;

import com.org.notification.model.Notification;
import com.org.notification.model.NotificationChannel;
import com.org.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class NotificationTools {

    private final NotificationService notificationService;

    @Tool(
            name = "sendNotification",
            description = "Send a notification to a team using the specified channel"
    )
    public Notification sendNotification(
            @ToolParam(description = "The notification channel") NotificationChannel channel,
            @ToolParam(description = "The recipient team") String recipient,
            @ToolParam(description = "Notification message") String message) {
        return notificationService.sendNotification(channel, recipient, message);
    }

}
