package com.org.ai.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpElicitation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Fulfils MCP <b>elicitation</b> requests (spec.mcp.client.elicitation): a downstream server can
 * pause a tool call and ask this client for structured input — e.g. the deployment service
 * requests explicit confirmation before executing a PROD deployment.
 *
 * <p>This is a headless REST service with no human at the keyboard mid-tool-call, so the policy is
 * configuration-driven rather than interactive:
 *
 * <ul>
 *   <li>{@code assistant.elicitation.auto-confirm=false} (default) — DECLINE every request. Safe
 *       default: destructive server-side actions gated on elicitation simply don't happen.
 *   <li>{@code assistant.elicitation.auto-confirm=true} — ACCEPT with {@code confirm=true} (for
 *       demos and integration tests of the elicitation flow).
 * </ul>
 *
 * <p>Every request and verdict is logged for audit either way.
 */
@Slf4j
@Component
public class McpElicitationHandler {

    @Value("${assistant.elicitation.auto-confirm:false}")
    private boolean autoConfirm;

    @McpElicitation(clients = "deployment")
    public McpSchema.ElicitResult handleDeploymentElicitation(McpSchema.ElicitRequest request) {
        if (autoConfirm) {
            log.warn("MCP elicitation | client=deployment verdict=ACCEPT (auto-confirm enabled) | message={}",
                    request.message());
            return new McpSchema.ElicitResult(
                    McpSchema.ElicitResult.Action.ACCEPT,
                    Map.of("confirm", true, "reason", "auto-confirmed by assistant.elicitation.auto-confirm"),
                    null);
        }
        log.info("MCP elicitation | client=deployment verdict=DECLINE (no human in the loop) | message={}",
                request.message());
        return new McpSchema.ElicitResult(McpSchema.ElicitResult.Action.DECLINE, null, null);
    }
}
