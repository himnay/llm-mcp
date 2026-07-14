package com.org.ai.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpProgress;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consumes MCP <b>progress notifications</b> (spec.mcp/notifications/progress) from downstream
 * servers — e.g. the deployment service's {@code executeDeployment} tool streams
 * validate/deploy/verify stage updates while the call is still running.
 *
 * <p>Each update is logged and the latest state per progress token is kept in memory so it can be
 * surfaced (last-known progress of an in-flight tool call) without any persistence dependency.
 */
@Slf4j
@Component
public class McpProgressHandler {

    /** Latest observed progress per progressToken; entries are removed on completion (>= total). */
    private final Map<Object, McpSchema.ProgressNotification> latestByToken = new ConcurrentHashMap<>();

    @McpProgress(clients = "deployment")
    public void handleDeploymentProgress(McpSchema.ProgressNotification notification) {
        double progress = notification.progress() != null ? notification.progress() : 0;
        Double total = notification.total();
        boolean done = total != null && progress >= total;
        log.info("MCP progress | client=deployment token={} progress={}{} message={}",
                notification.progressToken(),
                progress,
                total != null ? "/" + total : "",
                notification.message());
        if (done) {
            latestByToken.remove(notification.progressToken());
        } else {
            latestByToken.put(notification.progressToken(), notification);
        }
    }

    /** Last-known progress for an in-flight call, or {@code null} when finished/unknown. */
    public McpSchema.ProgressNotification latest(Object progressToken) {
        return latestByToken.get(progressToken);
    }
}
