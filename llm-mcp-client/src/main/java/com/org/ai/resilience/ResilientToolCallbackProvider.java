package com.org.ai.resilience;

import com.org.ai.audit.ToolAuditLog;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.chat.model.ToolContext;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Wraps every MCP tool callback with per-server Resilience4j retry + circuit breaker.
 * Retry fires first (transient errors get retried before the circuit sees the failure);
 * only persistent failures propagate to the circuit breaker.
 * When a server's circuit is OPEN the tool returns a structured error message instead
 * of making a doomed network call so the AI model can explain the outage gracefully.
 */
@Slf4j
public class ResilientToolCallbackProvider implements ToolCallbackProvider {

    private static final Map<String, String> TOOL_SERVER = Map.ofEntries(
            Map.entry("applyLeave", "mcp-hr"),
            Map.entry("findReplacement", "mcp-hr"),
            Map.entry("createTicket", "mcp-ticket"),
            Map.entry("getTickets", "mcp-ticket"),
            Map.entry("getTicket", "mcp-ticket"),
            Map.entry("updateTicketStatus", "mcp-ticket"),
            Map.entry("assignTicket", "mcp-ticket"),
            Map.entry("getDeployments", "mcp-deployment"),
            Map.entry("getDeployment", "mcp-deployment"),
            Map.entry("createDeployment", "mcp-deployment"),
            Map.entry("assignOwner", "mcp-deployment"),
            Map.entry("rescheduleDeployment", "mcp-deployment"),
            Map.entry("cancelDeployment", "mcp-deployment"),
            Map.entry("getNotifications", "mcp-notification"),
            Map.entry("sendNotification", "mcp-notification"),
            Map.entry("getRepository", "mcp-github"),
            Map.entry("getCommitHistory", "mcp-github"),
            Map.entry("getCommitMetrics", "mcp-github"),
            Map.entry("listBranches", "mcp-github"),
            Map.entry("getPullRequests", "mcp-github"),
            Map.entry("getIssues", "mcp-github"),
            Map.entry("getContributors", "mcp-github"),
            Map.entry("getWorkflowRuns", "mcp-github"),
            Map.entry("getReleases", "mcp-github"),
            Map.entry("searchRepositories", "mcp-github"),
            Map.entry("getCodeFrequency", "mcp-github"),
            Map.entry("createIssue", "mcp-github"),
            Map.entry("listEmails", "mcp-gmail"),
            Map.entry("getEmail", "mcp-gmail"),
            Map.entry("searchEmails", "mcp-gmail"),
            Map.entry("getEmailThread", "mcp-gmail"),
            Map.entry("getGmailProfile", "mcp-gmail"),
            Map.entry("listLabels", "mcp-gmail"),
            Map.entry("getEmailsByLabel", "mcp-gmail"),
            Map.entry("markAsRead", "mcp-gmail"),
            Map.entry("markAsUnread", "mcp-gmail"),
            Map.entry("createDraft", "mcp-gmail"),
            Map.entry("sendEmail", "mcp-gmail"),
            Map.entry("deleteEmail", "mcp-gmail")
    );

    private final ToolCallbackProvider delegate;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final int toolTimeoutSeconds;
    private final ToolAuditLog auditLog;

    public ResilientToolCallbackProvider(ToolCallbackProvider delegate,
                                         CircuitBreakerRegistry circuitBreakerRegistry,
                                         RetryRegistry retryRegistry) {
        this(delegate, circuitBreakerRegistry, retryRegistry, 30, null);
    }

    public ResilientToolCallbackProvider(ToolCallbackProvider delegate,
                                         CircuitBreakerRegistry circuitBreakerRegistry,
                                         RetryRegistry retryRegistry,
                                         int toolTimeoutSeconds) {
        this(delegate, circuitBreakerRegistry, retryRegistry, toolTimeoutSeconds, null);
    }

    public ResilientToolCallbackProvider(ToolCallbackProvider delegate,
                                         CircuitBreakerRegistry circuitBreakerRegistry,
                                         RetryRegistry retryRegistry,
                                         int toolTimeoutSeconds,
                                         ToolAuditLog auditLog) {
        this.delegate = delegate;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
        this.toolTimeoutSeconds = toolTimeoutSeconds;
        this.auditLog = auditLog;
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        return Arrays.stream(delegate.getToolCallbacks())
                .map(this::wrap)
                .toArray(ToolCallback[]::new);
    }

    private ToolCallback wrap(ToolCallback callback) {
        String toolName = callback.getToolDefinition().name();
        String serverName = TOOL_SERVER.getOrDefault(toolName, "mcp-unknown");
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(serverName);
        Retry retry = retryRegistry.retry(serverName);
        return new ResilientToolCallback(callback, cb, retry, serverName, toolTimeoutSeconds, auditLog);
    }

    private static final class ResilientToolCallback implements ToolCallback {

        private final ToolCallback delegate;
        private final CircuitBreaker circuitBreaker;
        private final Retry retry;
        private final String serverName;
        private final int toolTimeoutSeconds;
        private final ToolAuditLog auditLog;

        ResilientToolCallback(ToolCallback delegate, CircuitBreaker cb, Retry retry,
                              String serverName, int toolTimeoutSeconds, ToolAuditLog auditLog) {
            this.delegate = delegate;
            this.circuitBreaker = cb;
            this.retry = retry;
            this.serverName = serverName;
            this.toolTimeoutSeconds = toolTimeoutSeconds;
            this.auditLog = auditLog;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return delegate.getToolDefinition();
        }

        @Override
        public ToolMetadata getToolMetadata() {
            return delegate.getToolMetadata();
        }

        @Override
        public String call(String toolInput) {
            return execute(() -> delegate.call(toolInput));
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            return execute(() -> delegate.call(toolInput, toolContext));
        }

        private String execute(Callable<String> action) {
            // Retry wraps the circuit breaker — transient failures are retried before
            // the circuit breaker counts them as failures.
            Callable<String> withCb = CircuitBreaker.decorateCallable(circuitBreaker, action);
            Callable<String> withRetryAndCb = Retry.decorateCallable(retry, withCb);
            String toolName = delegate.getToolDefinition().name();
            long start = System.currentTimeMillis();
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            Future<String> future = executor.submit(withRetryAndCb);
            try {
                String result = future.get(toolTimeoutSeconds, TimeUnit.SECONDS);
                long durationMs = System.currentTimeMillis() - start;
                if (auditLog != null) {
                    auditLog.logInvocation(serverName, toolName, durationMs, true);
                }
                return result;
            } catch (TimeoutException e) {
                future.cancel(true);
                long durationMs = System.currentTimeMillis() - start;
                if (auditLog != null) {
                    auditLog.logInvocation(serverName, toolName, durationMs, false);
                }
                log.warn("Tool call timed out after {}s for server {}", toolTimeoutSeconds, serverName);
                return "Tool call timed out after " + toolTimeoutSeconds + "s";
            } catch (ExecutionException e) {
                long durationMs = System.currentTimeMillis() - start;
                if (auditLog != null) {
                    auditLog.logInvocation(serverName, toolName, durationMs, false);
                }
                Throwable cause = e.getCause();
                if (cause instanceof CallNotPermittedException) {
                    log.warn("Circuit breaker OPEN for {} — returning fallback", serverName);
                    return "{\"error\":\"" + serverName + " is temporarily unavailable (circuit open). "
                            + "Please try again later.\"}";
                }
                log.error("Tool call failed for server {}: {}", serverName, cause.getMessage());
                throw new RuntimeException("MCP tool call failed: " + cause.getMessage(), cause);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "Tool call interrupted";
            } finally {
                executor.shutdown();
            }
        }
    }
}
