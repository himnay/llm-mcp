package com.org.ai.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.mcp.annotation.McpSampling;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Fulfils MCP <b>sampling</b> requests (spec.mcp.client.sampling): a downstream MCP server
 * (e.g. github) can ask the connected client to run a one-off LLM completion on its behalf,
 * rather than calling a model provider itself. This handler routes that request through the
 * same {@link ChatModel} that powers {@code /chat}, so MCP servers gain LLM access without
 * holding their own API key.
 *
 * <p>Registered automatically by Spring AI's MCP client annotation scanner — the {@code clients}
 * attribute must match a connection name under {@code spring.ai.mcp.client.streamable-http.connections}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpSamplingHandler {

    private final ChatModel chatModel;

    @McpSampling(clients = "github")
    public McpSchema.CreateMessageResult handleGithubSampling(McpSchema.CreateMessageRequest request) {
        List<Message> messages = new ArrayList<>();
        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            messages.add(new SystemMessage(request.systemPrompt()));
        }
        for (McpSchema.SamplingMessage samplingMessage : request.messages()) {
            messages.add(new UserMessage(extractText(samplingMessage)));
        }

        ChatOptions options = ChatOptions.builder()
                .maxTokens(request.maxTokens())
                .temperature(request.temperature())
                .build();

        ChatResponse response = chatModel.call(new Prompt(messages, options));
        String text = response.getResult().getOutput().getText();

        log.info("MCP sampling fulfilled | client=github inputMessages={} responseChars={}",
                request.messages().size(), text.length());

        return McpSchema.CreateMessageResult.builder(McpSchema.Role.ASSISTANT, text, response.getMetadata().getModel())
                .stopReason(McpSchema.CreateMessageResult.StopReason.END_TURN)
                .build();
    }

    private static String extractText(McpSchema.SamplingMessage message) {
        if (message.content() instanceof McpSchema.TextContent textContent) {
            return textContent.text();
        }
        return message.content().toString();
    }
}
