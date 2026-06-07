package com.org.travel.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "mcp.security")
public class SecurityProperties {

    private String token = "";
    private String defaultUser = "system";
    private int rateLimitPerMinute = 120;
}
