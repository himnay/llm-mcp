package com.org.hr.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Controls MCP tool output size limits.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "mcp.output")
public class McpOutputProperties {

    /**
     * Maximum number of characters in a single tool response.
     * Responses longer than this are truncated with "…[truncated]".
     */
    private int maxChars = 8000;
}
