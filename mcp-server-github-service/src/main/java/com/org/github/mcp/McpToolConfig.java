package com.org.github.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class McpToolConfig {

    @Bean
    ToolCallbackProvider githubTools(GitHubMcpTools gitHubMcpTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(gitHubMcpTools)
                .build();
    }
}
