package com.org.ai.service;

import com.org.ai.config.AssistantProperties;
import com.org.ai.memory.PostgresConversationStore;
import com.org.ai.web.RequestContext;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;
    private final ToolCallbackProvider mcpToolProvider;
    private final PromptLoader promptLoader;
    private final AssistantProperties assistantProperties;
    private final PostgresConversationStore conversationStore;
    private final MeterRegistry meterRegistry;

    @Value("classpath:prompts/system.st")
    private Resource systemPromptTemplate;

    public String handleMessage(String message) {
        String conversationId  = RequestContext.user();
        String processedPrompt = promptLoader.loadPrompt(message);

        // Render the system prompt from the .st template
        String systemPrompt = new PromptTemplate(systemPromptTemplate).render(Map.of(
                "assistantName", assistantProperties.getName(),
                "currentUser",   conversationId,
                "currentTime",   ZonedDateTime.now().toString()
        ));

        // Load previous turns from PostgreSQL (capped to memory window)
        List<Message> history = conversationStore.loadMessages(
                conversationId, assistantProperties.getMemoryWindow());

        ChatResponse aiResponse = chatClient
                .prompt()
                .system(systemPrompt)
                .messages(history)
                .user(processedPrompt)
                .toolCallbacks(mcpToolProvider)
                .call()
                .chatResponse();

        String content = aiResponse.getResult().getOutput().getText();

        // Persist this exchange so future turns have context
        conversationStore.saveExchange(conversationId, processedPrompt, content);

        // Record OpenAI token consumption for Grafana
        recordTokenUsage(aiResponse, conversationId);

        return content;
    }

    private void recordTokenUsage(ChatResponse response, String user) {
        try {
            Usage usage = response.getMetadata().getUsage();
            if (usage == null) return;

            double promptTokens     = usage.getPromptTokens()     != null ? usage.getPromptTokens()     : 0;
            double completionTokens = usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;

            meterRegistry.counter("ai.tokens", "type", "prompt",     "user", user).increment(promptTokens);
            meterRegistry.counter("ai.tokens", "type", "completion", "user", user).increment(completionTokens);

            log.info("token-usage user={} prompt={} completion={} total={}",
                    user, (long) promptTokens, (long) completionTokens, (long) (promptTokens + completionTokens));
        } catch (Exception e) {
            log.warn("Failed to record token usage: {}", e.getMessage());
        }
    }
}
