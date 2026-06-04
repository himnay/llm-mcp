package com.org.hr.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class McpToolConfig {

    @Bean
    ToolCallbackProvider hrTools(HrMcpTools hrMcpTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(hrMcpTools)
                .build();
    }
}
