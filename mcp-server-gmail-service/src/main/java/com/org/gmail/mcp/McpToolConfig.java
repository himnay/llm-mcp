package com.org.gmail.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class McpToolConfig {

    @Bean
    ToolCallbackProvider gmailTools(GmailMcpTools gmailMcpTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(gmailMcpTools)
                .build();
    }
}
