package com.org.ai.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ToolAuditLog {

    /** Logs invocation. */
    public void logInvocation(String conversationId, String toolName, long durationMs, boolean success) {
        log.info("TOOL_AUDIT conversationId={} tool={} durationMs={} success={}",
                conversationId, toolName, durationMs, success);
    }

    /** Logs truncation. */
    public void logTruncation(String toolName, int originalSize, int maxSize) {
        log.warn("TOOL_TRUNCATION tool={} originalSize={} maxSize={}",
                toolName, originalSize, maxSize);
    }
}
