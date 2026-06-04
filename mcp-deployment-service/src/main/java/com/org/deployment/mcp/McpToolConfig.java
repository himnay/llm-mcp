package com.org.deployment.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class McpToolConfig {

    @Bean
    ToolCallbackProvider deploymentTools(DeploymentMcpTools deploymentMcpTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(deploymentMcpTools)
                .build();
    }
}
