package com.org.github.mcp;

import com.org.github.service.GitHubService;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.ai.mcp.annotation.context.McpSyncRequestContext;
import org.springframework.stereotype.Component;

/**
 * Demonstrates MCP <b>sampling</b>: instead of calling an LLM provider directly, this tool asks
 * the connected MCP client to run the completion on its behalf via {@link McpSyncRequestContext#sample}.
 * The chat assistant's own model does the writing ({@code com.org.ai.mcp.McpSamplingHandler} on the
 * client side), so this server never needs its own model API key.
 *
 * <p>Split into its own class (rather than folded into {@link GitHubMcpTools}) only because
 * {@link McpSyncRequestContext} injection — and therefore sampling — requires a stateful
 * (STREAMABLE) server session; this method would be silently filtered out if this service ever
 * ran in STATELESS mode.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class GitHubAiInsightsTools {

    private final GitHubService gitHubService;

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be null or blank");
        }
    }

    private static String extractText(McpSchema.Content content) {
        if (content instanceof McpSchema.TextContent textContent) {
            return textContent.text();
        }
        return content.toString();
    }

    @McpTool(name = "summarizeRepositoryHealth",
            description = "Generate an AI-written narrative health summary for a GitHub repository covering "
                    + "open issues, open pull requests and recent commit activity. Provide owner and repo. "
                    + "Delegates the actual writing to the connected client's LLM via MCP sampling; falls back "
                    + "to a plain data digest if the client does not support sampling.")
    String summarizeRepositoryHealth(
            @McpToolParam(description = "Repository owner (GitHub username or org)", required = true) String owner,
            @McpToolParam(description = "Repository name", required = true) String repo,
            McpSyncRequestContext ctx) {

        requireNonBlank(owner, "owner");
        requireNonBlank(repo, "repo");

        String digest = """
                Repository: %s/%s
                Metadata: %s
                Open pull requests: %s
                Open issues: %s
                Recent commits (page 1, main): %s
                """.formatted(owner, repo,
                gitHubService.getRepository(owner, repo),
                gitHubService.getPullRequests(owner, repo, "open"),
                gitHubService.getIssues(owner, repo, "open", null),
                gitHubService.getCommitHistory(owner, repo, "main", 1));

        if (!ctx.sampleEnabled()) {
            log.info("TOOL summarizeRepositoryHealth | owner={} repo={} sampling=unavailable", owner, repo);
            return "Sampling is not supported by the connected MCP client; raw data follows:\n" + digest;
        }

        long start = System.nanoTime();
        McpSchema.CreateMessageResult result = ctx.sample(spec -> spec
                .systemPrompt("You are a senior engineering manager. Write a concise (5-8 sentence) narrative "
                        + "health summary of the repository data below: call out risk, stale pull requests, "
                        + "open issue volume and the recent commit activity trend. Plain text, no markdown headers.")
                .maxTokens(400)
                .message(digest));

        String summary = extractText(result.content());
        log.info("TOOL summarizeRepositoryHealth | owner={} repo={} sampling=ok latencyMs={}",
                owner, repo, (System.nanoTime() - start) / 1_000_000L);
        return summary;
    }
}
