package com.org.hr.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code mcp.security.*} from application configuration.
 * <p>
 * All properties have safe defaults so the service runs in dev mode without
 * any environment variables set.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "mcp.security")
public class SecurityProperties {

    /**
     * Shared secret token. Bound to env {@code MCP_AUTH_TOKEN}.
     * When blank, auth is disabled and a startup WARN is emitted.
     */
    private String token = "";

    /**
     * Fallback acting-user name when {@code X-Acting-User} header is absent.
     */
    private String defaultUser = "system";

    /**
     * When {@code true}, WRITE tools reject requests whose acting-user
     * resolves to the default/fallback value.
     */
    private boolean requireUserForWrites = false;

    /**
     * Maximum number of requests per user per 60-second window.
     */
    private int rateLimitPerMinute = 120;
}
