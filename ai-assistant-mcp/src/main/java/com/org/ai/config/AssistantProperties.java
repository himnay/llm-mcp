package com.org.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration for the AI assistant. Bound from the `assistant.*` prefix.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "assistant")
public class AssistantProperties {

    /** Display name / persona of the assistant. */
    private String name = "Enterprise AI Assistant";

    /** Default acting user when no authenticated principal is available. */
    private String defaultUser = "john.doe";
}
