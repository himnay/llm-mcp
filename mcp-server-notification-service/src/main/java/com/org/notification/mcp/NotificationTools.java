package com.org.notification.mcp;

import com.org.notification.exception.InvalidToolArgumentException;
import com.org.notification.model.Notification;
import com.org.notification.model.NotificationChannel;
import com.org.notification.security.ActingUserContext;
import com.org.notification.security.RateLimiter;
import com.org.notification.security.SecurityProperties;
import com.org.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * MCP tool surface for notifications.
 *
 * <p>Every tool:
 * <ul>
 *   <li>Validates its inputs (null/blank/enum guards → {@link InvalidToolArgumentException})</li>
 *   <li>Resolves the acting user from {@link ActingUserContext} (set by the auth filter)</li>
 *   <li>Logs tool name, acting user, sanitised argument summary, outcome and latency</li>
 *   <li>Write/destructive methods additionally enforce the write-gate when
 *       {@code mcp.security.requireUserForWrites} is enabled</li>
 *   <li>Caps response size via {@link OutputSizeCapUtil} to protect the LLM context window</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
class NotificationTools {

    private final NotificationService notificationService;
    private final SecurityProperties securityProperties;
    private final RateLimiter rateLimiter;

    @Value("${mcp.output.max-chars:8000}")
    private int maxOutputChars;

    // ─────────────────────────────── READ tools ──────────────────────────────

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidToolArgumentException(fieldName + " must not be null or blank");
        }
    }

    // ─────────────────────────────── WRITE tools ─────────────────────────────

    private static long elapsedMs(long startNano) {
        return (System.nanoTime() - startNano) / 1_000_000L;
    }

    // ─────────────────────────────── helpers ─────────────────────────────────

    @McpTool(name = "getNotifications",
            description = "List all notifications across all channels and recipients. "
                    + "Returns id, channel (INTERNAL/EMAIL/SLACK), recipient team, message, sentAt, and status. "
                    + "Use this to check what notifications have been sent or are pending.")
    public String getNotifications() {
        String actingUser = resolveUser();
        long start = System.nanoTime();
        try {
            List<Notification> result = notificationService.getNotifications();
            String capped = OutputSizeCapUtil.cap(result.toString(), maxOutputChars);
            log.info("TOOL getNotifications | user={} resultCount={} latencyMs={}",
                    actingUser, result.size(), elapsedMs(start));
            return capped;
        } catch (Exception ex) {
            log.error("TOOL getNotifications | user={} outcome=ERROR latencyMs={} error={}",
                    actingUser, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    @McpTool(
            name = "sendNotification",
            description = "Send a notification to a team using the specified channel"
    )
    public String sendNotification(
            @McpToolParam(description = "The notification channel (INTERNAL, EMAIL, SLACK)") NotificationChannel channel,
            @McpToolParam(description = "The recipient team") String recipient,
            @McpToolParam(description = "Notification message") String message) {

        String actingUser = resolveUser();
        enforceWriteGate(actingUser);
        long start = System.nanoTime();

        // ── validation ────────────────────────────────────────────────────────
        if (channel == null) {
            throw new InvalidToolArgumentException(
                    "channel must not be null. Allowed values: INTERNAL, EMAIL, SLACK");
        }
        requireNonBlank(recipient, "recipient");
        requireNonBlank(message, "message");

        try {
            Notification result = notificationService.sendNotification(channel, recipient, message);
            log.info("AUDIT sendNotification | user={} channel={} recipient={} newId={} "
                            + "outcome=SUCCESS latencyMs={}",
                    actingUser, channel, recipient, result.getId(), elapsedMs(start));
            return OutputSizeCapUtil.cap(result.toString(), maxOutputChars);
        } catch (Exception ex) {
            log.error("AUDIT sendNotification | user={} channel={} recipient={} outcome=ERROR "
                            + "latencyMs={} error={}",
                    actingUser, channel, recipient, elapsedMs(start), ex.getMessage());
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
            throw new InvalidToolArgumentException(
                    "Write operations require an explicit X-Acting-User header. "
                            + "Default user '" + actingUser + "' is not permitted to perform mutations.");
        }
        if (!rateLimiter.tryAcquireWrite(actingUser)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Write rate limit exceeded (10 writes/min) for user " + actingUser);
        }
    }
}
