package com.nexacorp.ai.service;

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

    public String handleMessage(String message) {

        String processedPrompt = promptLoader.loadPrompt(message);

        String currentUser = "john.doe";   // simulate authenticated user
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
