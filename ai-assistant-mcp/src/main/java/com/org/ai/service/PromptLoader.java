package com.org.ai.service;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
class PromptLoader {

    private final List<McpSyncClient> mcpSyncClients;

    public String loadPrompt(String message) {

        if (!message.startsWith("/")) {
            return message;
        }
        String promptName = message.substring(1);

        for (McpSyncClient client : mcpSyncClients) {
            var prompts = client.listPrompts().prompts();

            boolean exists = prompts.stream()
                    .anyMatch(p -> p.name().equalsIgnoreCase(promptName));
            if (exists) {
                McpSchema.GetPromptResult response = client.getPrompt(new McpSchema.GetPromptRequest(promptName, Map.of()));
                McpSchema.TextContent content = (McpSchema.TextContent) response.messages().get(0).content();
                return content.text();
            }
        }
        return message;
    }

}
