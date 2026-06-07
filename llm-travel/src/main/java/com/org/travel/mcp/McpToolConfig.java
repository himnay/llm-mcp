package com.org.travel.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class McpToolConfig {

    @Bean
    public ToolCallbackProvider travelTools(FlightMcpTools flightMcpTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(flightMcpTools)
                .build();
    }
}
