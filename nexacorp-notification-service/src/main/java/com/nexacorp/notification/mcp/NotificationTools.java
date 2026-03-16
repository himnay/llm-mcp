package com.nexacorp.notification.mcp;

import com.nexacorp.notification.model.Notification;
import com.nexacorp.notification.model.NotificationChannel;
import com.nexacorp.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class NotificationTools {

    private final NotificationService notificationService;

    @McpTool(
            name = "sendNotification",
            description = "Send a notification to a team using the specified channel"
    )
    public Notification sendNotification(
            @McpToolParam(description = "The notification channel") NotificationChannel channel,
            @McpToolParam(description = "The recipient team") String recipient,
            @McpToolParam(description = "Notification message") String message) {
        return notificationService.sendNotification(channel, recipient, message);
    }

}
