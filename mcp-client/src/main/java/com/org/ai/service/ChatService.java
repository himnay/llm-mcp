package com.org.ai.service;

import com.org.ai.config.AssistantProperties;
import com.org.ai.config.PromptInjectionGuard;
import com.org.ai.mcp.SemanticToolSelector;
import com.org.ai.web.RequestContext;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;
    private final SemanticToolSelector semanticToolSelector;
    private final PromptLoader promptLoader;
    private final AssistantProperties assistantProperties;
    private final MeterRegistry meterRegistry;
    private final PromptInjectionGuard injectionGuard;

    @Value("classpath:prompts/system.st")
    private Resource systemPromptTemplate;

    public String handleMessage(String message) {
        if (!injectionGuard.isQuerySafe(message)) {
            return injectionGuard.blockMessage();
        }
        String conversationId = RequestContext.user();
        String processedPrompt = promptLoader.loadPrompt(message);

        String systemPrompt = new PromptTemplate(systemPromptTemplate).render(Map.of(
                "assistantName", assistantProperties.getName(),
                "currentUser", conversationId,
                "currentTime", ZonedDateTime.now().toString()
        ));

        ChatResponse aiResponse = chatClient
                .prompt()
                .system(systemPrompt)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .user(processedPrompt)
                .toolCallbacks(semanticToolSelector.selectTools(processedPrompt))
                .call()
                .chatResponse();

        String content = aiResponse.getResult().getOutput().getText();
        recordTokenUsage(aiResponse, conversationId);
        return content;
    }

    public void streamChat(String conversationId, String message, SseEmitter emitter) {
        if (!injectionGuard.isQuerySafe(message)) {
            try {
                emitter.send(SseEmitter.event().name("error").data(injectionGuard.blockMessage()));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return;
        }
        String processedPrompt = promptLoader.loadPrompt(message);

        String systemPrompt = new PromptTemplate(systemPromptTemplate).render(Map.of(
                "assistantName", assistantProperties.getName(),
                "currentUser", conversationId,
                "currentTime", ZonedDateTime.now().toString()
        ));

        Executor executor = Executors.newVirtualThreadPerTaskExecutor();
        executor.execute(() -> {
            try {
                chatClient.prompt()
                        .system(systemPrompt)
                        .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                        .user(processedPrompt)
                        .toolCallbacks(semanticToolSelector.selectTools(processedPrompt))
                        .stream()
                        .content()
                        .doOnNext(token -> {
                            try {
                                emitter.send(SseEmitter.event().name("token").data(token));
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        })
                        .doOnComplete(emitter::complete)
                        .doOnError(emitter::completeWithError)
                        .subscribe();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
    }

    private void recordTokenUsage(ChatResponse response, String user) {
        try {
            Usage usage = response.getMetadata().getUsage();
            if (usage == null) return;

            double promptTokens = usage.getPromptTokens() != null ? usage.getPromptTokens() : 0;
            double completionTokens = usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;

            meterRegistry.counter("ai.tokens", "type", "prompt", "user", user).increment(promptTokens);
            meterRegistry.counter("ai.tokens", "type", "completion", "user", user).increment(completionTokens);

            log.info("token-usage user={} prompt={} completion={} total={}",
                    user, (long) promptTokens, (long) completionTokens, (long) (promptTokens + completionTokens));
        } catch (Exception e) {
            log.warn("Failed to record token usage: {}", e.getMessage());
        }
    }
}
