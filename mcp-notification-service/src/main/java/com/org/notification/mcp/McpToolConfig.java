package com.org.notification.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class McpToolConfig {

    @Bean
    ToolCallbackProvider notificationTools(NotificationTools notificationTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(notificationTools)
                .build();
    }
}
