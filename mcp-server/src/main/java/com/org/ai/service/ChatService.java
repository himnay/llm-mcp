package com.org.ai.service;

import com.org.ai.config.AssistantProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;
    private final ToolCallbackProvider mcpToolProvider;
    private final PromptLoader promptLoader;
    private final AssistantProperties assistantProperties;

    public String handleMessage(String message) {

        String processedPrompt = promptLoader.loadPrompt(message);

        String currentUser = assistantProperties.getDefaultUser();
        String currentTime = java.time.ZonedDateTime.now().toString();

        String systemPrompt = """
            You are an enterprise AI assistant.
            Context:
            - Current user: %s
            - Current date and time: %s
            """.formatted(currentUser, currentTime);

        String response = chatClient
                .prompt()
                .system(systemPrompt)
                .user(processedPrompt)
                .toolCallbacks(mcpToolProvider)
                .call()
                .content();

        return response;
    }

}
