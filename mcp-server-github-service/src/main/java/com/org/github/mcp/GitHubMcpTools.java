package com.org.github.mcp;

import com.org.github.service.GitHubService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * MCP tool surface for GitHub. Each method validates its inputs, then delegates
 * the invariant execution skeleton (acting user, write gate, audit log, output
 * cap) to {@link ToolExecutionTemplate}.
 */
@Component
@RequiredArgsConstructor
class GitHubMcpTools {

    private final GitHubService gitHubService;
    private final ToolExecutionTemplate template;

    // ── READ tools ────────────────────────────────────────────────────────────

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be null or blank");
        }
    }

    private static String resolveState(String state, String defaultState) {
        if (state == null || state.isBlank()) return defaultState;
        String s = state.trim().toLowerCase();
        if (!s.equals("open") && !s.equals("closed") && !s.equals("all")) {
            throw new IllegalArgumentException("state must be one of: open, closed, all. Got: " + state);
        }
        return s;
    }

    @Tool(name = "getRepository",
            description = "Get GitHub repository metadata including stars, forks, language, description, open issues count, "
                    + "default branch and visibility. Provide owner (GitHub username or org) and repo name.")
    public String getRepository(String owner, String repo) {
        requireNonBlank(owner, "owner");
        requireNonBlank(repo, "repo");
        return template.executeRead("getRepository", "owner=" + owner + " repo=" + repo,
                () -> gitHubService.getRepository(owner, repo));
    }

    @Tool(name = "getCommitHistory",
            description = "Get recent commit history for a GitHub repository. Provide owner, repo, branch (default: main) "
                    + "and optional page number (default: 1). Returns SHA, message, author, and date for each commit.")
    public String getCommitHistory(String owner, String repo, String branch, Integer page) {
        requireNonBlank(owner, "owner");
        requireNonBlank(repo, "repo");
        String resolvedBranch = (branch != null && !branch.isBlank()) ? branch : "main";
        int resolvedPage = (page != null && page > 0) ? page : 1;
        return template.executeRead("getCommitHistory",
                "owner=" + owner + " repo=" + repo + " branch=" + resolvedBranch + " page=" + resolvedPage,
                () -> gitHubService.getCommitHistory(owner, repo, resolvedBranch, resolvedPage));
    }

    @Tool(name = "getCommitMetrics",
            description = "Get commit metrics for a GitHub repository within a date range. Returns commit count, "
                    + "authors, and frequency statistics. Provide owner, repo, since (ISO date e.g. 2024-01-01T00:00:00Z) "
                    + "and until (ISO date e.g. 2024-12-31T23:59:59Z).")
    public String getCommitMetrics(String owner, String repo, String since, String until) {
        requireNonBlank(owner, "owner");
        requireNonBlank(repo, "repo");
        requireNonBlank(since, "since");
        requireNonBlank(until, "until");
        return template.executeRead("getCommitMetrics",
                "owner=" + owner + " repo=" + repo + " since=" + since + " until=" + until,
                () -> gitHubService.getCommitMetrics(owner, repo, since, until));
    }

    @Tool(name = "listBranches",
            description = "List all branches in a GitHub repository with their latest commit SHA. Provide owner and repo.")
    public String listBranches(String owner, String repo) {
        requireNonBlank(owner, "owner");
        requireNonBlank(repo, "repo");
        return template.executeRead("listBranches", "owner=" + owner + " repo=" + repo,
                () -> gitHubService.listBranches(owner, repo));
    }

    @Tool(name = "getPullRequests",
            description = "List pull requests for a GitHub repository. Provide owner, repo, and state (open, closed, or all). "
                    + "Returns PR number, title, author, labels, created date, and merge status.")
    public String getPullRequests(String owner, String repo, String state) {
        requireNonBlank(owner, "owner");
        requireNonBlank(repo, "repo");
        String resolvedState = resolveState(state, "open");
        return template.executeRead("getPullRequests",
                "owner=" + owner + " repo=" + repo + " state=" + resolvedState,
                () -> gitHubService.getPullRequests(owner, repo, resolvedState));
    }

    @Tool(name = "getIssues",
            description = "List issues for a GitHub repository. Provide owner, repo, state (open, closed, or all), "
                    + "and optional comma-separated labels to filter by.")
    public String getIssues(String owner, String repo, String state, String labels) {
        requireNonBlank(owner, "owner");
        requireNonBlank(repo, "repo");
        String resolvedState = resolveState(state, "open");
        return template.executeRead("getIssues",
                "owner=" + owner + " repo=" + repo + " state=" + resolvedState + " labels=" + labels,
                () -> gitHubService.getIssues(owner, repo, resolvedState, labels));
    }

    @Tool(name = "getContributors",
            description = "Get the list of contributors for a GitHub repository sorted by commit count. "
                    + "Returns login, avatar, contributions count per contributor.")
    public String getContributors(String owner, String repo) {
        requireNonBlank(owner, "owner");
        requireNonBlank(repo, "repo");
        return template.executeRead("getContributors", "owner=" + owner + " repo=" + repo,
                () -> gitHubService.getContributors(owner, repo));
    }

    @Tool(name = "getWorkflowRuns",
            description = "Get GitHub Actions workflow runs for a repository. Provide owner, repo and optional workflowId "
                    + "(filename like ci.yml or numeric ID). Returns run status, conclusion, branch, and triggered time.")
    public String getWorkflowRuns(String owner, String repo, String workflowId) {
        requireNonBlank(owner, "owner");
        requireNonBlank(repo, "repo");
        return template.executeRead("getWorkflowRuns",
                "owner=" + owner + " repo=" + repo + " workflowId=" + workflowId,
                () -> gitHubService.getWorkflowRuns(owner, repo, workflowId));
    }

    @Tool(name = "getReleases",
            description = "List releases for a GitHub repository including tags, release dates, and download counts. "
                    + "Provide owner and repo.")
    public String getReleases(String owner, String repo) {
        requireNonBlank(owner, "owner");
        requireNonBlank(repo, "repo");
        return template.executeRead("getReleases", "owner=" + owner + " repo=" + repo,
                () -> gitHubService.getReleases(owner, repo));
    }

    // ── WRITE tools ───────────────────────────────────────────────────────────

    @Tool(name = "searchRepositories",
            description = "Search GitHub repositories by query string. Supports GitHub search qualifiers like language:java, "
                    + "stars:>100, topic:spring-boot. Provide query, sort (stars, forks, updated), and order (asc, desc).")
    public String searchRepositories(String query, String sort, String order) {
        requireNonBlank(query, "query");
        String resolvedSort = (sort != null && !sort.isBlank()) ? sort : "stars";
        String resolvedOrder = (order != null && !order.isBlank()) ? order : "desc";
        return template.executeRead("searchRepositories",
                "query=" + query + " sort=" + resolvedSort + " order=" + resolvedOrder,
                () -> gitHubService.searchRepositories(query, resolvedSort, resolvedOrder));
    }

    // ── validation helpers ────────────────────────────────────────────────────

    @Tool(name = "getCodeFrequency",
            description = "Get weekly code frequency statistics (additions and deletions) for a GitHub repository. "
                    + "Returns an array of [timestamp, additions, deletions] tuples per week.")
    public String getCodeFrequency(String owner, String repo) {
        requireNonBlank(owner, "owner");
        requireNonBlank(repo, "repo");
        return template.executeRead("getCodeFrequency", "owner=" + owner + " repo=" + repo,
                () -> gitHubService.getCodeFrequency(owner, repo));
    }

    @Tool(name = "createIssue",
            description = "Create a new GitHub issue in a repository. Provide owner, repo, title, optional body, "
                    + "and optional comma-separated labels. Requires write access to the repository.")
    public String createIssue(String owner, String repo, String title, String body, String labels) {
        requireNonBlank(owner, "owner");
        requireNonBlank(repo, "repo");
        requireNonBlank(title, "title");
        return template.executeWrite("createIssue",
                "owner=" + owner + " repo=" + repo + " title=" + title,
                () -> gitHubService.createIssue(owner, repo, title, body, labels));
    }
}
