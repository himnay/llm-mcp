package com.org.gmail.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "mcp.security")
public class SecurityProperties {
    private String token = "";
    private String defaultUser = "system";
    private boolean requireUserForWrites = false;
    private int rateLimitPerMinute = 120;
}
