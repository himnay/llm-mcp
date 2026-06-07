package com.org.ai.resilience;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.chat.model.ToolContext;

import java.util.Arrays;
import java.util.Map;

/**
 * Wraps every MCP tool callback with a per-server Resilience4j circuit breaker.
 * When a server's circuit is OPEN the tool returns a structured error message
 * instead of making a doomed network call — the AI model can then explain the
 * outage gracefully to the user.
 *
 * <p>Registered explicitly as a {@code @Primary} bean in {@link com.org.ai.config.AppConfig}
 * (not component-scanned) to avoid a duplicate bean-name collision.
 */
@Slf4j
@RequiredArgsConstructor
public class ResilientToolCallbackProvider implements ToolCallbackProvider {

    private static final Map<String, String> TOOL_SERVER = Map.ofEntries(
            // HR service
            Map.entry("applyLeave",           "mcp-hr"),
            Map.entry("findReplacement",      "mcp-hr"),
            // Ticket service
            Map.entry("createTicket",         "mcp-ticket"),
            Map.entry("getTickets",           "mcp-ticket"),
            Map.entry("getTicket",            "mcp-ticket"),
            Map.entry("updateTicketStatus",   "mcp-ticket"),
            Map.entry("assignTicket",         "mcp-ticket"),
            // Deployment service
            Map.entry("getDeployments",       "mcp-deployment"),
            Map.entry("getDeployment",        "mcp-deployment"),
            Map.entry("createDeployment",     "mcp-deployment"),
            Map.entry("assignOwner",          "mcp-deployment"),
            Map.entry("rescheduleDeployment", "mcp-deployment"),
            Map.entry("cancelDeployment",     "mcp-deployment"),
            // Notification service
            Map.entry("getNotifications",     "mcp-notification"),
            Map.entry("sendNotification",     "mcp-notification"),
            // GitHub service
            Map.entry("getRepository",        "mcp-github"),
            Map.entry("getCommitHistory",     "mcp-github"),
            Map.entry("getCommitMetrics",     "mcp-github"),
            Map.entry("listBranches",         "mcp-github"),
            Map.entry("getPullRequests",      "mcp-github"),
            Map.entry("getIssues",            "mcp-github"),
            Map.entry("getContributors",      "mcp-github"),
            Map.entry("getWorkflowRuns",      "mcp-github"),
            Map.entry("getReleases",          "mcp-github"),
            Map.entry("searchRepositories",   "mcp-github"),
            Map.entry("getCodeFrequency",     "mcp-github"),
            Map.entry("createIssue",          "mcp-github"),
            // Gmail service
            Map.entry("listEmails",           "mcp-gmail"),
            Map.entry("getEmail",             "mcp-gmail"),
            Map.entry("searchEmails",         "mcp-gmail"),
            Map.entry("getEmailThread",       "mcp-gmail"),
            Map.entry("getGmailProfile",      "mcp-gmail"),
            Map.entry("listLabels",           "mcp-gmail"),
            Map.entry("getEmailsByLabel",     "mcp-gmail"),
            Map.entry("markAsRead",           "mcp-gmail"),
            Map.entry("markAsUnread",         "mcp-gmail"),
            Map.entry("createDraft",          "mcp-gmail"),
            Map.entry("sendEmail",            "mcp-gmail"),
            Map.entry("deleteEmail",          "mcp-gmail")
    );

    private final ToolCallbackProvider delegate;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Override
    public ToolCallback[] getToolCallbacks() {
        return Arrays.stream(delegate.getToolCallbacks())
                .map(this::wrap)
                .toArray(ToolCallback[]::new);
    }

    private ToolCallback wrap(ToolCallback callback) {
        String toolName   = callback.getToolDefinition().name();
        String serverName = TOOL_SERVER.getOrDefault(toolName, "mcp-unknown");
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(serverName);
        return new CircuitBreakerToolCallback(callback, cb, serverName);
    }

    // -----------------------------------------------------------------------

    private static final class CircuitBreakerToolCallback implements ToolCallback {

        private final ToolCallback delegate;
        private final CircuitBreaker circuitBreaker;
        private final String serverName;

        CircuitBreakerToolCallback(ToolCallback delegate, CircuitBreaker cb, String serverName) {
            this.delegate      = delegate;
            this.circuitBreaker = cb;
            this.serverName    = serverName;
        }

        @Override
        public ToolDefinition getToolDefinition() { return delegate.getToolDefinition(); }

        @Override
        public ToolMetadata getToolMetadata() { return delegate.getToolMetadata(); }

        @Override
        public String call(String toolInput) {
            return execute(() -> delegate.call(toolInput));
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            return execute(() -> delegate.call(toolInput, toolContext));
        }

        private String execute(java.util.concurrent.Callable<String> action) {
            try {
                return circuitBreaker.executeCallable(action);
            } catch (CallNotPermittedException e) {
                log.warn("Circuit breaker OPEN for {} — returning fallback", serverName);
                return "{\"error\":\"" + serverName + " is temporarily unavailable (circuit open). "
                        + "Please try again later.\"}";
            } catch (Exception e) {
                log.error("Tool call failed for server {}: {}", serverName, e.getMessage());
                throw new RuntimeException("MCP tool call failed: " + e.getMessage(), e);
            }
        }
    }
}
