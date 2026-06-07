package com.org.travel.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "mcp.output")
public class McpOutputProperties {

    private int maxChars = 8000;
}
