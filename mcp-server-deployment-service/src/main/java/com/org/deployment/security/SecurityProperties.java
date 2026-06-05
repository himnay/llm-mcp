package com.org.deployment.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalised security knobs for the MCP server.
 *
 * <pre>
 * mcp:
 *   security:
 *     token: ${MCP_AUTH_TOKEN:}
 *     defaultUser: system
 *     requireUserForWrites: false
 *     rateLimitPerMinute: 120
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "mcp.security")
public class SecurityProperties {

    /**
     * Shared bearer token.  Empty = insecure dev mode (startup WARN emitted).
     */
    private String token = "";

    /**
     * Fallback acting-user name when no X-Acting-User header is present.
     */
    private String defaultUser = "system";

    /**
     * When {@code true}, write/destructive tool calls must carry an explicit
     * X-Acting-User that differs from {@link #defaultUser}.
     */
    private boolean requireUserForWrites = false;

    /**
     * Max requests per user per minute before HTTP 429 is returned.
     */
    private int rateLimitPerMinute = 120;
}
