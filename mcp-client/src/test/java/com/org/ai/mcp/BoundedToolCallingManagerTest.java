package com.org.ai.mcp;

import com.org.ai.exception.ToolInvocationException;
import com.org.ai.web.RequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BoundedToolCallingManagerTest {

    private final ToolCallingManager delegate = mock(ToolCallingManager.class);
    private final ChatResponse toolCallResponse = new ChatResponse(List.of(new Generation(new AssistantMessage("calling tools"))));

    @AfterEach
    void clearContext() {
        RequestContext.clear();
    }

    @Test
    @DisplayName("Only tool responses after the last user message count as rounds of this request")
    void countsRoundsSinceLastUserMessage() {
        List<Message> history = List.of(
                new UserMessage("earlier question"), toolResponse(), toolResponse(),
                new UserMessage("current question"), new AssistantMessage("calling"), toolResponse());

        assertThat(BoundedToolCallingManager.completedRounds(history)).isEqualTo(1);
    }

    @Test
    @DisplayName("Executing beyond the cap throws instead of looping on")
    void rejectsRoundBeyondCap() {
        List<Message> history = new ArrayList<>(List.of(new UserMessage("q")));
        history.add(toolResponse());
        history.add(toolResponse());
        BoundedToolCallingManager manager = new BoundedToolCallingManager(delegate, 2);

        assertThatThrownBy(() -> manager.executeToolCalls(new Prompt(history), toolCallResponse))
                .isInstanceOf(ToolInvocationException.class)
                .hasMessageContaining("max tool-call iterations (2)");
        verify(delegate, never()).executeToolCalls(any(), any());
    }

    @Test
    @DisplayName("The acting user from the tool context is visible to the MCP transport off the request thread")
    void restoresActingUserFromToolContext() {
        AtomicReference<String> seenByDelegate = new AtomicReference<>();
        when(delegate.executeToolCalls(any(), any())).thenAnswer(invocation -> {
            seenByDelegate.set(RequestContext.user());
            return mock(ToolExecutionResult.class);
        });
        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .toolContext(Map.of(BoundedToolCallingManager.ACTING_USER, "alice"))
                .build();

        new BoundedToolCallingManager(delegate, 5)
                .executeToolCalls(new Prompt(List.of(new UserMessage("q")), options), toolCallResponse);

        assertThat(seenByDelegate.get()).isEqualTo("alice");
        assertThat(RequestContext.user()).as("context is cleaned up again").isNull();
    }

    private static ToolResponseMessage toolResponse() {
        return ToolResponseMessage.builder()
                .responses(List.of(new ToolResponseMessage.ToolResponse("id", "tool", "{}")))
                .build();
    }
}
