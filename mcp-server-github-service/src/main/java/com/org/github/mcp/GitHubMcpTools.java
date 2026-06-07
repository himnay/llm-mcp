package com.org.github.mcp;

import com.org.github.security.ActingUserContext;
import com.org.github.security.SecurityProperties;
import com.org.github.service.GitHubService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class GitHubMcpTools {

    private final GitHubService gitHubService;
    private final SecurityProperties securityProperties;

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
    }

    // ── READ tools ────────────────────────────────────────────────────────────

    @Tool(name = "getRepository",
          description = "Get GitHub repository metadata including stars, forks, language, description, open issues count, "
                  + "default branch and visibility. Provide owner (GitHub username or org) and repo name.")
    public String getRepository(String owner, String repo) {
        String actingUser = resolveUser();
        long start = System.nanoTime();
        requireNonBlank(owner, "owner");
        requireNonBlank(repo, "repo");
        try {
            String result = gitHubService.getRepository(owner, repo);
            String capped = OutputSizeCapUtil.cap(result);
            log.info("TOOL getRepository | user={} owner={} repo={} latencyMs={}", actingUser, owner, repo, elapsedMs(start));
            return capped;
        } catch (Exception ex) {
            log.error("TOOL getRepository | user={} owner={} repo={} outcome=ERROR latencyMs={} error={}",
                    actingUser, owner, repo, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    @Tool(name = "getCommitHistory",
          description = "Get recent commit history for a GitHub repository. Provide owner, repo, branch (default: main) "
                  + "and optional page number (default: 1). Returns SHA, message, author, and date for each commit.")
    public String getCommitHistory(String owner, String repo, String branch, Integer page) {
        String actingUser = resolveUser();
        long start = System.nanoTime();
        requireNonBlank(owner, "owner");
        requireNonBlank(repo, "repo");
        String resolvedBranch = (branch != null && !branch.isBlank()) ? branch : "main";
        int resolvedPage = (page != null && page > 0) ? page : 1;
        try {
            String result = gitHubService.getCommitHistory(owner, repo, resolvedBranch, resolvedPage);
            String capped = OutputSizeCapUtil.cap(result);
            log.info("TOOL getCommitHistory | user={} owner={} repo={} branch={} page={} latencyMs={}",
                    actingUser, owner, repo, resolvedBranch, resolvedPage, elapsedMs(start));
            return capped;
        } catch (Exception ex) {
            log.error("TOOL getCommitHistory | user={} owner={} repo={} outcome=ERROR latencyMs={} error={}",
                    actingUser, owner, repo, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    @Tool(name = "getCommitMetrics",
          description = "Get commit metrics for a GitHub repository within a date range. Returns commit count, "
                  + "authors, and frequency statistics. Provide owner, repo, since (ISO date e.g. 2024-01-01T00:00:00Z) "
                  + "and until (ISO date e.g. 2024-12-31T23:59:59Z).")
    public String getCommitMetrics(String owner, String repo, String since, String until) {
        String actingUser = resolveUser();
        long start = System.nanoTime();
        requireNonBlank(owner, "owner");
        requireNonBlank(repo, "repo");
        requireNonBlank(since, "since");
        requireNonBlank(until, "until");
        try {
            String result = gitHubService.getCommitMetrics(owner, repo, since, until);
            String capped = OutputSizeCapUtil.cap(result);
            log.info("TOOL getCommitMetrics | user={} owner={} repo={} since={} until={} latencyMs={}",
                    actingUser, owner, repo, since, until, elapsedMs(start));
            return capped;
        } catch (Exception ex) {
            log.error("TOOL getCommitMetrics | user={} owner={} repo={} outcome=ERROR latencyMs={} error={}",
                    actingUser, owner, repo, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    @Tool(name = "listBranches",
          description = "List all branches in a GitHub repository with their latest commit SHA. Provide owner and repo.")
    public String listBranches(String owner, String repo) {
        String actingUser = resolveUser();
        long start = System.nanoTime();
        requireNonBlank(owner, "owner");
        requireNonBlank(repo, "repo");
        try {
            String result = gitHubService.listBranches(owner, repo);
            String capped = OutputSizeCapUtil.cap(result);
            log.info("TOOL listBranches | user={} owner={} repo={} latencyMs={}", actingUser, owner, repo, elapsedMs(start));
            return capped;
        } catch (Exception ex) {
            log.error("TOOL listBranches | user={} owner={} repo={} outcome=ERROR latencyMs={} error={}",
                    actingUser, owner, repo, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    @Tool(name = "getPullRequests",
          description = "List pull requests for a GitHub repository. Provide owner, repo, and state (open, closed, or all). "
                  + "Returns PR number, title, author, labels, created date, and merge status.")
    public String getPullRequests(String owner, String repo, String state) {
        String actingUser = resolveUser();
        long start = System.nanoTime();
        requireNonBlank(owner, "owner");
        requireNonBlank(repo, "repo");
        String resolvedState = resolveState(state, "open");
        try {
            String result = gitHubService.getPullRequests(owner, repo, resolvedState);
            String capped = OutputSizeCapUtil.cap(result);
            log.info("TOOL getPullRequests | user={} owner={} repo={} state={} latencyMs={}",
                    actingUser, owner, repo, resolvedState, elapsedMs(start));
            return capped;
        } catch (Exception ex) {
            log.error("TOOL getPullRequests | user={} owner={} repo={} outcome=ERROR latencyMs={} error={}",
                    actingUser, owner, repo, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    @Tool(name = "getIssues",
          description = "List issues for a GitHub repository. Provide owner, repo, state (open, closed, or all), "
                  + "and optional comma-separated labels to filter by.")
    public String getIssues(String owner, String repo, String state, String labels) {
        String actingUser = resolveUser();
        long start = System.nanoTime();
        requireNonBlank(owner, "owner");
        requireNonBlank(repo, "repo");
        String resolvedState = resolveState(state, "open");
        try {
            String result = gitHubService.getIssues(owner, repo, resolvedState, labels);
            String capped = OutputSizeCapUtil.cap(result);
            log.info("TOOL getIssues | user={} owner={} repo={} state={} labels={} latencyMs={}",
                    actingUser, owner, repo, resolvedState, labels, elapsedMs(start));
            return capped;
        } catch (Exception ex) {
            log.error("TOOL getIssues | user={} owner={} repo={} outcome=ERROR latencyMs={} error={}",
                    actingUser, owner, repo, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    @Tool(name = "getContributors",
          description = "Get the list of contributors for a GitHub repository sorted by commit count. "
                  + "Returns login, avatar, contributions count per contributor.")
    public String getContributors(String owner, String repo) {
        String actingUser = resolveUser();
        long start = System.nanoTime();
        requireNonBlank(owner, "owner");
        requireNonBlank(repo, "repo");
        try {
            String result = gitHubService.getContributors(owner, repo);
            String capped = OutputSizeCapUtil.cap(result);
            log.info("TOOL getContributors | user={} owner={} repo={} latencyMs={}", actingUser, owner, repo, elapsedMs(start));
            return capped;
        } catch (Exception ex) {
            log.error("TOOL getContributors | user={} owner={} repo={} outcome=ERROR latencyMs={} error={}",
                    actingUser, owner, repo, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    @Tool(name = "getWorkflowRuns",
          description = "Get GitHub Actions workflow runs for a repository. Provide owner, repo and optional workflowId "
                  + "(filename like ci.yml or numeric ID). Returns run status, conclusion, branch, and triggered time.")
    public String getWorkflowRuns(String owner, String repo, String workflowId) {
        String actingUser = resolveUser();
        long start = System.nanoTime();
        requireNonBlank(owner, "owner");
        requireNonBlank(repo, "repo");
        try {
            String result = gitHubService.getWorkflowRuns(owner, repo, workflowId);
            String capped = OutputSizeCapUtil.cap(result);
            log.info("TOOL getWorkflowRuns | user={} owner={} repo={} workflowId={} latencyMs={}",
                    actingUser, owner, repo, workflowId, elapsedMs(start));
            return capped;
        } catch (Exception ex) {
            log.error("TOOL getWorkflowRuns | user={} owner={} repo={} outcome=ERROR latencyMs={} error={}",
                    actingUser, owner, repo, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    @Tool(name = "getReleases",
          description = "List releases for a GitHub repository including tags, release dates, and download counts. "
                  + "Provide owner and repo.")
    public String getReleases(String owner, String repo) {
        String actingUser = resolveUser();
        long start = System.nanoTime();
        requireNonBlank(owner, "owner");
        requireNonBlank(repo, "repo");
        try {
            String result = gitHubService.getReleases(owner, repo);
            String capped = OutputSizeCapUtil.cap(result);
            log.info("TOOL getReleases | user={} owner={} repo={} latencyMs={}", actingUser, owner, repo, elapsedMs(start));
            return capped;
        } catch (Exception ex) {
            log.error("TOOL getReleases | user={} owner={} repo={} outcome=ERROR latencyMs={} error={}",
                    actingUser, owner, repo, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    @Tool(name = "searchRepositories",
          description = "Search GitHub repositories by query string. Supports GitHub search qualifiers like language:java, "
                  + "stars:>100, topic:spring-boot. Provide query, sort (stars, forks, updated), and order (asc, desc).")
    public String searchRepositories(String query, String sort, String order) {
        String actingUser = resolveUser();
        long start = System.nanoTime();
        requireNonBlank(query, "query");
        String resolvedSort = (sort != null && !sort.isBlank()) ? sort : "stars";
        String resolvedOrder = (order != null && !order.isBlank()) ? order : "desc";
        try {
            String result = gitHubService.searchRepositories(query, resolvedSort, resolvedOrder);
            String capped = OutputSizeCapUtil.cap(result);
            log.info("TOOL searchRepositories | user={} query={} sort={} order={} latencyMs={}",
                    actingUser, query, resolvedSort, resolvedOrder, elapsedMs(start));
            return capped;
        } catch (Exception ex) {
            log.error("TOOL searchRepositories | user={} query={} outcome=ERROR latencyMs={} error={}",
                    actingUser, query, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    @Tool(name = "getCodeFrequency",
          description = "Get weekly code frequency statistics (additions and deletions) for a GitHub repository. "
                  + "Returns an array of [timestamp, additions, deletions] tuples per week.")
    public String getCodeFrequency(String owner, String repo) {
        String actingUser = resolveUser();
        long start = System.nanoTime();
        requireNonBlank(owner, "owner");
        requireNonBlank(repo, "repo");
        try {
            String result = gitHubService.getCodeFrequency(owner, repo);
            String capped = OutputSizeCapUtil.cap(result);
            log.info("TOOL getCodeFrequency | user={} owner={} repo={} latencyMs={}", actingUser, owner, repo, elapsedMs(start));
            return capped;
        } catch (Exception ex) {
            log.error("TOOL getCodeFrequency | user={} owner={} repo={} outcome=ERROR latencyMs={} error={}",
                    actingUser, owner, repo, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    // ── WRITE tools ───────────────────────────────────────────────────────────

    @Tool(name = "createIssue",
          description = "Create a new GitHub issue in a repository. Provide owner, repo, title, optional body, "
                  + "and optional comma-separated labels. Requires write access to the repository.")
    public String createIssue(String owner, String repo, String title, String body, String labels) {
        String actingUser = resolveUser();
        enforceWriteGate(actingUser);
        long start = System.nanoTime();
        requireNonBlank(owner, "owner");
        requireNonBlank(repo, "repo");
        requireNonBlank(title, "title");
        try {
            String result = gitHubService.createIssue(owner, repo, title, body, labels);
            log.info("AUDIT createIssue | user={} owner={} repo={} title={} outcome=SUCCESS latencyMs={}",
                    actingUser, owner, repo, title, elapsedMs(start));
            return result;
        } catch (Exception ex) {
            log.error("AUDIT createIssue | user={} owner={} repo={} outcome=ERROR latencyMs={} error={}",
                    actingUser, owner, repo, elapsedMs(start), ex.getMessage());
            throw ex;
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
}
