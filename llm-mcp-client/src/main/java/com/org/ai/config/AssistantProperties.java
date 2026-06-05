package com.org.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Typed configuration for the AI assistant. Bound from the `assistant.*` prefix.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "assistant")
public class AssistantProperties {

    /**
     * Display name / persona of the assistant.
     */
    private String name = "Enterprise AI Assistant";

    /**
     * Default acting user when no authenticated principal is available.
     */
    private String defaultUser = "john.doe";

    /**
     * Shared bearer token sent to downstream MCP servers. Bound from MCP_AUTH_TOKEN.
     */
    private String mcpAuthToken = "";

    /**
     * Number of past messages retained per conversation for chat memory.
     */
    private int memoryWindow = 20;

    /**
     * Hard cap on tool-execution rounds per chat request (runaway-loop guard).
     */
    private int maxToolIterations = 5;

    /**
     * Max characters of a single tool result fed back into the model context.
     */
    private int maxToolResultChars = 8000;

    /**
     * Per-user chat requests allowed per minute.
     */
    private int rateLimitPerMinute = 30;

    /**
     * Words that, if present in the prompt, cause the request to be blocked (SafeGuard).
     */
    private List<String> sensitiveWords = new ArrayList<>();

    /**
     * Tool-name substrings treated as write/destructive; gated unless the request opts in.
     */
    private List<String> writeToolKeywords = new ArrayList<>(List.of(
            "apply", "create", "update", "delete", "send", "deploy",
            "trigger", "rollback", "cancel", "remove", "approve", "assign", "reschedule"));
}
