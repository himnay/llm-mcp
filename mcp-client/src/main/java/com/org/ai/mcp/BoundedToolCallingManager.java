package com.org.ai.mcp;

import com.org.ai.exception.ToolInvocationException;
import com.org.ai.web.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;
import java.util.Map;

/**
 * Decorates the auto-configured {@link ToolCallingManager} (see {@code AppConfig}) to (1) cap the
 * number of tool-execution rounds per chat request — guarding against runaway tool-calling loops —
 * and (2) emit structured observability for every tool round (which tools, acting user, latency).
 *
 * <p>Both are derived from the {@link Prompt} rather than thread-locals, because on the streaming
 * path tools execute on Reactor threads, not the request thread: the round count is the number of
 * tool responses since the last user message, and the acting user travels in the tool context
 * under {@link #ACTING_USER}. While the delegate runs, that user is restored into
 * {@link RequestContext} so the MCP transport can still send it as {@code X-Acting-User}.</p>
 */
public class BoundedToolCallingManager implements ToolCallingManager {

    /** Tool-context key carrying the acting user from {@code ChatService} to tool execution. */
    public static final String ACTING_USER = "actingUser";

    private static final Logger log = LoggerFactory.getLogger(BoundedToolCallingManager.class);

    private final ToolCallingManager delegate;
    private final int maxIterations;

    public BoundedToolCallingManager(ToolCallingManager delegate, int maxIterations) {
        this.delegate = delegate;
        this.maxIterations = maxIterations;
    }

    @Override
    public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
        return delegate.resolveToolDefinitions(chatOptions);
    }

    @Override
    public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
        int iteration = completedRounds(prompt.getInstructions()) + 1;
        if (iteration > maxIterations) {
            throw new ToolInvocationException(
                    "Exceeded max tool-call iterations (" + maxIterations + ") for a single request");
        }

        String user = actingUser(prompt);
        if (chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null) {
            AssistantMessage output = chatResponse.getResult().getOutput();
            for (AssistantMessage.ToolCall call : output.getToolCalls()) {
                log.info("tool-call iteration={} user={} tool={}", iteration, user, call.name());
            }
        }

        boolean restoreContext = user != null && RequestContext.user() == null;
        if (restoreContext) {
            RequestContext.set(user, "default", false);
        }
        long start = System.nanoTime();
        try {
            return delegate.executeToolCalls(prompt, chatResponse);
        } finally {
            if (restoreContext) {
                RequestContext.clear();
            }
            log.info("tool-call iteration={} completed in {} ms",
                    iteration, (System.nanoTime() - start) / 1_000_000);
        }
    }

    /** Tool rounds already executed in this request: tool responses after the last user message. */
    static int completedRounds(List<Message> messages) {
        int rounds = 0;
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if (message instanceof UserMessage) {
                break;
            }
            if (message instanceof ToolResponseMessage) {
                rounds++;
            }
        }
        return rounds;
    }

    private static String actingUser(Prompt prompt) {
        if (prompt.getOptions() instanceof ToolCallingChatOptions options) {
            Map<String, Object> toolContext = options.getToolContext();
            if (toolContext != null && toolContext.get(ACTING_USER) instanceof String user) {
                return user;
            }
        }
        return RequestContext.user();
    }
}
