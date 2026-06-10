package com.org.github.mcp;

import com.org.github.security.ActingUserContext;
import com.org.github.security.SecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Template Method (GoF) for MCP tool execution. The invariant skeleton —
 * resolve the acting user, enforce the write gate for mutations, time the
 * call, emit a structured audit log line, cap the output size — is defined
 * once here. Each {@code @Tool} method supplies only the varying step: the
 * actual business call, passed as a {@link Supplier} (a lightweight Command).
 */
@Slf4j
@Component
@RequiredArgsConstructor
class ToolExecutionTemplate {

    private final SecurityProperties securityProperties;

    /** Read tools: no write gate; result is capped to protect the LLM context window. */
    String executeRead(String toolName, String args, Supplier<String> action) {
        return execute(toolName, args, false, true, action);
    }

    /** Write tools: write gate enforced; logged at AUDIT level; result returned uncapped. */
    String executeWrite(String toolName, String args, Supplier<String> action) {
        return execute(toolName, args, true, false, action);
    }

    private String execute(String toolName, String args, boolean write, boolean capOutput,
                           Supplier<String> action) {
        String actingUser = resolveUser();
        if (write) {
            enforceWriteGate(actingUser);
        }
        String logTag = write ? "AUDIT" : "TOOL";
        long start = System.nanoTime();
        try {
            String result = action.get();
            if (capOutput) {
                result = OutputSizeCapUtil.cap(result);
            }
            log.info("{} {} | user={} {} outcome=SUCCESS latencyMs={}",
                    logTag, toolName, actingUser, args, elapsedMs(start));
            return result;
        } catch (Exception ex) {
            log.error("{} {} | user={} {} outcome=ERROR latencyMs={} error={}",
                    logTag, toolName, actingUser, args, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    private String resolveUser() {
        String user = ActingUserContext.get();
        return (user != null && !user.isBlank()) ? user : securityProperties.getDefaultUser();
    }

    private void enforceWriteGate(String actingUser) {
        if (securityProperties.isRequireUserForWrites()
                && securityProperties.getDefaultUser().equals(actingUser)) {
            throw new IllegalStateException(
                    "Write operations require an explicit X-Acting-User header. "
                            + "Default user '" + actingUser + "' is not permitted to perform mutations.");
        }
    }

    private static long elapsedMs(long startNano) {
        return (System.nanoTime() - startNano) / 1_000_000L;
    }
}
