package com.org.ai.service;

import com.org.ai.dto.ChatResponse;
import com.org.ai.intent.ChatIntent;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;
    private final OrchestrationService orchestrationService;


    public ChatResponse handleMessage(String message) {
        String currentUser = "john.doe";   // simulate authenticated user
        String currentTime = java.time.ZonedDateTime.now().toString();

        String systemPrompt = """
            You are an enterprise AI assistant.
        
            Context:
            - Current user: %s
            - Current date and time: %s
        
            Rules:
            1. Interpret "I", "me", or "my" as the current user.
            2. Resolve relative dates like "tomorrow", "next Friday", etc.
               into absolute dates in yyyy-MM-dd format.
            3. Always return STRICT JSON.
            4. Do not return explanations.
        
            Output format:
        
            {
              "intent": "REASSIGN_DEPLOYMENT_DUE_TO_LEAVE | RESCHEDULE_DEPLOYMENT | UNKNOWN",
              "username": "string or null",
              "date": "yyyy-MM-dd or null",
              "deploymentId": "string or null"
            }
            """.formatted(currentUser, currentTime);

        ChatIntent intent = chatClient.prompt()
                .user(message)
                .system(systemPrompt)
                .call()
                .entity(ChatIntent.class);

        String response = orchestrationService.execute(intent);

        return new ChatResponse(response);
    }
}
