package com.org.gmail.mcp;

import com.org.gmail.security.ActingUserContext;
import com.org.gmail.security.RateLimiter;
import com.org.gmail.security.SecurityProperties;
import com.org.gmail.service.GmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Component
@RequiredArgsConstructor
class GmailMcpTools {

    private final GmailService gmailService;
    private final SecurityProperties securityProperties;
    private final RateLimiter rateLimiter;

    @Value("${mcp.output.max-chars:8000}")
    private int maxOutputChars;

    // ── validation helpers ────────────────────────────────────────────────────

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be null or blank");
        }
    }

    private static long elapsedMs(long startNano) {
        return (System.nanoTime() - startNano) / 1_000_000L;
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
        if (!rateLimiter.tryAcquireWrite(actingUser)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Write rate limit exceeded (10 writes/min) for user " + actingUser);
        }
    }

    // ── READ tools ────────────────────────────────────────────────────────────

    @Tool(name = "listEmails",
            description = "List emails from Gmail. Provide optional labelIds (e.g. INBOX, SENT, SPAM, TRASH or custom label ID) "
                    + "and optional maxResults (default: 20, max: 100). Returns message IDs and thread IDs.")
    public String listEmails(String labelIds, Integer maxResults) {
        String actingUser = resolveUser();
        long start = System.nanoTime();
        int size = (maxResults != null && maxResults > 0 && maxResults <= 100) ? maxResults : 20;
        try {
            String result = gmailService.listEmails(labelIds, size);
            String capped = OutputSizeCapUtil.cap(result, maxOutputChars);
            log.info("TOOL listEmails | user={} labelIds={} maxResults={} latencyMs={}",
                    actingUser, labelIds, size, elapsedMs(start));
            return capped;
        } catch (Exception ex) {
            log.error("TOOL listEmails | user={} outcome=ERROR latencyMs={} error={}",
                    actingUser, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    @Tool(name = "getEmail",
            description = "Get the full content of an email by its message ID. Provide messageId and optional format "
                    + "(full, metadata, minimal, raw). Default format is 'full' which includes headers and body.")
    public String getEmail(String messageId, String format) {
        String actingUser = resolveUser();
        long start = System.nanoTime();
        requireNonBlank(messageId, "messageId");
        try {
            String result = gmailService.getEmail(messageId, format);
            String capped = OutputSizeCapUtil.cap(result, maxOutputChars);
            log.info("TOOL getEmail | user={} messageId={} format={} latencyMs={}",
                    actingUser, messageId, format, elapsedMs(start));
            return capped;
        } catch (Exception ex) {
            log.error("TOOL getEmail | user={} messageId={} outcome=ERROR latencyMs={} error={}",
                    actingUser, messageId, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    @Tool(name = "searchEmails",
            description = "Search emails using Gmail query syntax. Examples: 'from:boss@example.com', 'subject:invoice', "
                    + "'is:unread', 'after:2024/01/01', 'has:attachment'. Provide query and optional maxResults.")
    public String searchEmails(String query, Integer maxResults) {
        String actingUser = resolveUser();
        long start = System.nanoTime();
        requireNonBlank(query, "query");
        int size = (maxResults != null && maxResults > 0 && maxResults <= 100) ? maxResults : 20;
        try {
            String result = gmailService.searchEmails(query, size);
            String capped = OutputSizeCapUtil.cap(result, maxOutputChars);
            log.info("TOOL searchEmails | user={} query={} maxResults={} latencyMs={}",
                    actingUser, query, size, elapsedMs(start));
            return capped;
        } catch (Exception ex) {
            log.error("TOOL searchEmails | user={} query={} outcome=ERROR latencyMs={} error={}",
                    actingUser, query, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    @Tool(name = "getEmailThread",
            description = "Get a full email conversation thread by thread ID. Returns all messages in the thread "
                    + "in chronological order with full headers and body.")
    public String getEmailThread(String threadId) {
        String actingUser = resolveUser();
        long start = System.nanoTime();
        requireNonBlank(threadId, "threadId");
        try {
            String result = gmailService.getEmailThread(threadId);
            String capped = OutputSizeCapUtil.cap(result, maxOutputChars);
            log.info("TOOL getEmailThread | user={} threadId={} latencyMs={}",
                    actingUser, threadId, elapsedMs(start));
            return capped;
        } catch (Exception ex) {
            log.error("TOOL getEmailThread | user={} threadId={} outcome=ERROR latencyMs={} error={}",
                    actingUser, threadId, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    @Tool(name = "getGmailProfile",
            description = "Get the Gmail profile of the authenticated user. Returns email address, total messages count, "
                    + "total threads count, and history ID.")
    public String getGmailProfile() {
        String actingUser = resolveUser();
        long start = System.nanoTime();
        try {
            String result = gmailService.getProfile();
            String capped = OutputSizeCapUtil.cap(result, maxOutputChars);
            log.info("TOOL getGmailProfile | user={} latencyMs={}", actingUser, elapsedMs(start));
            return capped;
        } catch (Exception ex) {
            log.error("TOOL getGmailProfile | user={} outcome=ERROR latencyMs={} error={}",
                    actingUser, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    @Tool(name = "listLabels",
            description = "List all Gmail labels (folders) for the authenticated user. Returns system labels like INBOX, SENT, "
                    + "TRASH, SPAM and any custom labels with their IDs, names, and message counts.")
    public String listLabels() {
        String actingUser = resolveUser();
        long start = System.nanoTime();
        try {
            String result = gmailService.listLabels();
            String capped = OutputSizeCapUtil.cap(result, maxOutputChars);
            log.info("TOOL listLabels | user={} latencyMs={}", actingUser, elapsedMs(start));
            return capped;
        } catch (Exception ex) {
            log.error("TOOL listLabels | user={} outcome=ERROR latencyMs={} error={}",
                    actingUser, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    @Tool(name = "getEmailsByLabel",
            description = "Get emails filtered by a specific Gmail label ID. Use listLabels first to get label IDs. "
                    + "Provide labelId and optional maxResults.")
    public String getEmailsByLabel(String labelId, Integer maxResults) {
        String actingUser = resolveUser();
        long start = System.nanoTime();
        requireNonBlank(labelId, "labelId");
        int size = (maxResults != null && maxResults > 0 && maxResults <= 100) ? maxResults : 20;
        try {
            String result = gmailService.getEmailsByLabel(labelId, size);
            String capped = OutputSizeCapUtil.cap(result, maxOutputChars);
            log.info("TOOL getEmailsByLabel | user={} labelId={} maxResults={} latencyMs={}",
                    actingUser, labelId, size, elapsedMs(start));
            return capped;
        } catch (Exception ex) {
            log.error("TOOL getEmailsByLabel | user={} labelId={} outcome=ERROR latencyMs={} error={}",
                    actingUser, labelId, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    // ── WRITE tools ───────────────────────────────────────────────────────────

    @Tool(name = "markAsRead",
            description = "Mark an email as read by removing the UNREAD label. Provide the messageId.")
    public String markAsRead(String messageId) {
        String actingUser = resolveUser();
        enforceWriteGate(actingUser);
        long start = System.nanoTime();
        requireNonBlank(messageId, "messageId");
        try {
            String result = gmailService.markAsRead(messageId);
            log.info("AUDIT markAsRead | user={} messageId={} outcome=SUCCESS latencyMs={}",
                    actingUser, messageId, elapsedMs(start));
            return result;
        } catch (Exception ex) {
            log.error("AUDIT markAsRead | user={} messageId={} outcome=ERROR latencyMs={} error={}",
                    actingUser, messageId, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    @Tool(name = "markAsUnread",
            description = "Mark an email as unread by adding the UNREAD label. Provide the messageId.")
    public String markAsUnread(String messageId) {
        String actingUser = resolveUser();
        enforceWriteGate(actingUser);
        long start = System.nanoTime();
        requireNonBlank(messageId, "messageId");
        try {
            String result = gmailService.markAsUnread(messageId);
            log.info("AUDIT markAsUnread | user={} messageId={} outcome=SUCCESS latencyMs={}",
                    actingUser, messageId, elapsedMs(start));
            return result;
        } catch (Exception ex) {
            log.error("AUDIT markAsUnread | user={} messageId={} outcome=ERROR latencyMs={} error={}",
                    actingUser, messageId, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    @Tool(name = "createDraft",
            description = "Create a Gmail draft email. Provide to (recipient email address), subject, and body (plain text).")
    public String createDraft(String to, String subject, String body) {
        String actingUser = resolveUser();
        enforceWriteGate(actingUser);
        long start = System.nanoTime();
        requireNonBlank(to, "to");
        requireNonBlank(subject, "subject");
        requireNonBlank(body, "body");
        try {
            String result = gmailService.createDraft(to, subject, body);
            log.info("AUDIT createDraft | user={} to={} subject={} outcome=SUCCESS latencyMs={}",
                    actingUser, to, subject, elapsedMs(start));
            return result;
        } catch (Exception ex) {
            log.error("AUDIT createDraft | user={} to={} outcome=ERROR latencyMs={} error={}",
                    actingUser, to, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    @Tool(name = "sendEmail",
            description = "Send an email via Gmail. Provide to (recipient email), subject, and body (plain text). "
                    + "This immediately sends the email — use createDraft if you want to review first.")
    public String sendEmail(String to, String subject, String body) {
        String actingUser = resolveUser();
        enforceWriteGate(actingUser);
        long start = System.nanoTime();
        requireNonBlank(to, "to");
        requireNonBlank(subject, "subject");
        requireNonBlank(body, "body");
        try {
            String result = gmailService.sendEmail(to, subject, body);
            log.info("AUDIT sendEmail | user={} to={} subject={} outcome=SUCCESS latencyMs={}",
                    actingUser, to, subject, elapsedMs(start));
            return result;
        } catch (Exception ex) {
            log.error("AUDIT sendEmail | user={} to={} outcome=ERROR latencyMs={} error={}",
                    actingUser, to, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    @Tool(name = "deleteEmail",
            description = "Move an email to Trash. Provide the messageId. The email can be permanently deleted from Trash later.")
    public String deleteEmail(String messageId) {
        String actingUser = resolveUser();
        enforceWriteGate(actingUser);
        long start = System.nanoTime();
        requireNonBlank(messageId, "messageId");
        try {
            String result = gmailService.deleteEmail(messageId);
            log.info("AUDIT deleteEmail | user={} messageId={} outcome=SUCCESS latencyMs={}",
                    actingUser, messageId, elapsedMs(start));
            return result;
        } catch (Exception ex) {
            log.error("AUDIT deleteEmail | user={} messageId={} outcome=ERROR latencyMs={} error={}",
                    actingUser, messageId, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }
}
