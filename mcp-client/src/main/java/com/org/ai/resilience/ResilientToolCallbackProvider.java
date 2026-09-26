package com.org.ai.resilience;

import com.org.ai.audit.ToolAuditLog;
import com.org.ai.exception.ToolInvocationException;
import com.org.ai.web.RequestContext;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Wraps every MCP tool callback with per-server Resilience4j retry + circuit breaker.
 * Retry fires first (transient errors get retried before the circuit sees the failure);
 * only persistent failures propagate to the circuit breaker. Write tools (names matching
 * {@code assistant.write-tool-keywords}) are never retried: a write that reached the server but
 * whose response was lost would otherwise run twice.
 * When a server's circuit is OPEN the tool returns a structured error message instead
 * of making a doomed network call so the AI model can explain the outage gracefully.
 *
 * <p>The tool-to-server routing is built at startup from each connected server's own tool list
 * (see {@code AppConfig}); it used to be a hand-maintained map that drifted as servers gained tools.
 * Tool calls run on a separate virtual thread (for the timeout), so the caller's
 * {@link RequestContext} is carried over explicitly — otherwise {@code X-Acting-User} never reached
 * the MCP servers.</p>
 */
@Slf4j
public class ResilientToolCallbackProvider implements ToolCallbackProvider {

    private final ToolCallbackProvider delegate;
    private final Map<String, String> toolServers;
    private final List<String> writeToolKeywords;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final int toolTimeoutSeconds;
    private final ToolAuditLog auditLog;

    public ResilientToolCallbackProvider(ToolCallbackProvider delegate,
                                         Map<String, String> toolServers,
                                         List<String> writeToolKeywords,
                                         CircuitBreakerRegistry circuitBreakerRegistry,
                                         RetryRegistry retryRegistry,
                                         int toolTimeoutSeconds,
                                         ToolAuditLog auditLog) {
        this.delegate = delegate;
        this.toolServers = Map.copyOf(toolServers);
        this.writeToolKeywords = writeToolKeywords.stream().map(k -> k.toLowerCase(Locale.ROOT)).toList();
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
        this.toolTimeoutSeconds = toolTimeoutSeconds;
        this.auditLog = auditLog;
    }

    /** True when the tool's leading verb is one of the configured write/destructive verbs. */
    boolean isWriteTool(String toolName) {
        return writeToolKeywords.contains(leadingVerb(toolName));
    }

    /**
     * The tool name's leading verb — {@code createIssue} → {@code create}, {@code list_repos} →
     * {@code list}. Write detection compares whole verbs: substring matching made the read
     * {@code getDeployments} a "write" because it contains {@code deploy}.
     */
    static String leadingVerb(String toolName) {
        if (toolName == null || toolName.isEmpty()) {
            return "";
        }
        String name = Character.toLowerCase(toolName.charAt(0)) + toolName.substring(1);
        int end = 0;
        while (end < name.length() && Character.isLowerCase(name.charAt(end))) {
            end++;
        }
        return name.substring(0, end);
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        return Arrays.stream(delegate.getToolCallbacks())
                .map(this::wrap)
                .toArray(ToolCallback[]::new);
    }

    private ToolCallback wrap(ToolCallback callback) {
        String toolName = callback.getToolDefinition().name();
        String serverName = toolServers.getOrDefault(toolName, "mcp-unknown");
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(serverName);
        Retry retry = isWriteTool(toolName) ? null : retryRegistry.retry(serverName);
        return new ResilientToolCallback(callback, cb, retry, serverName, toolTimeoutSeconds, auditLog);
    }

    private static final class ResilientToolCallback implements ToolCallback {

        private final ToolCallback delegate;
        private final CircuitBreaker circuitBreaker;
        private final Retry retry;
        private final String serverName;
        private final int toolTimeoutSeconds;
        private final ToolAuditLog auditLog;

        /** {@code retry} is null for write tools, which must not be re-executed. */
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
            Callable<String> withRetryAndCb = retry == null ? withCb : Retry.decorateCallable(retry, withCb);
            Callable<String> withContext = RequestContext.propagate(withRetryAndCb);
            String toolName = delegate.getToolDefinition().name();
            long start = System.currentTimeMillis();
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            Future<String> future = executor.submit(withContext);
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
                throw new ToolInvocationException("MCP tool call failed: " + cause.getMessage(), cause);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "Tool call interrupted";
            } finally {
                executor.shutdown();
            }
        }
    }
}
