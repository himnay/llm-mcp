package com.org.ai.mcp;

import com.org.ai.web.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;

/**
 * Decorates a {@link ToolCallingManager} to (1) cap the number of tool-execution
 * rounds per chat request — guarding against runaway tool-calling loops — and
 * (2) emit structured observability for every tool round (which tools, acting
 * user, latency). Call {@link #reset()} at the start of each request.
 */
public class BoundedToolCallingManager implements ToolCallingManager {

    private static final Logger log = LoggerFactory.getLogger(BoundedToolCallingManager.class);
    private static final ThreadLocal<Integer> ITERATIONS = ThreadLocal.withInitial(() -> 0);

    private final ToolCallingManager delegate;
    private final int maxIterations;

    public BoundedToolCallingManager(ToolCallingManager delegate, int maxIterations) {
        this.delegate = delegate;
        this.maxIterations = maxIterations;
    }

    /**
     * Reset the per-request iteration counter. Must be called before each chat call.
     */
    public static void reset() {
        ITERATIONS.set(0);
    }

    @Override
    public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
        return delegate.resolveToolDefinitions(chatOptions);
    }

    @Override
    public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
        int iteration = ITERATIONS.get() + 1;
        ITERATIONS.set(iteration);
        if (iteration > maxIterations) {
            throw new IllegalStateException(
                    "Exceeded max tool-call iterations (" + maxIterations + ") for a single request");
        }

        String user = RequestContext.user();
        if (chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null) {
            AssistantMessage output = chatResponse.getResult().getOutput();
            for (AssistantMessage.ToolCall call : output.getToolCalls()) {
                log.info("tool-call iteration={} user={} tool={}", iteration, user, call.name());
            }
        }

        long start = System.nanoTime();
        try {
            return delegate.executeToolCalls(prompt, chatResponse);
        } finally {
            log.info("tool-call iteration={} completed in {} ms",
                    iteration, (System.nanoTime() - start) / 1_000_000);
        }
    }
}
