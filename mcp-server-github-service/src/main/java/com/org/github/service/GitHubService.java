package com.org.github.service;

import com.org.github.config.GitHubProperties;
import com.org.github.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubService {

    private final RestClient gitHubRestClient;
    private final GitHubProperties gitHubProperties;

    @Cacheable(value = "github", key = "'repo:' + #owner + '/' + #repo")
    public String getRepository(String owner, String repo) {
        try {
            return gitHubRestClient.get()
                    .uri("/repos/{owner}/{repo}", owner, repo)
                    .retrieve()
                    .body(String.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("Repository " + owner + "/" + repo + " not found");
        }
    }

    @Cacheable(value = "github", key = "'commits:' + #owner + '/' + #repo + ':' + #branch + ':p' + #page")
    public String getCommitHistory(String owner, String repo, String branch, int page) {
        try {
            return gitHubRestClient.get()
                    .uri("/repos/{owner}/{repo}/commits?sha={branch}&per_page={perPage}&page={page}",
                            owner, repo, branch, gitHubProperties.getDefaultPageSize(), page)
                    .retrieve()
                    .body(String.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("Repository or branch " + owner + "/" + repo + "#" + branch + " not found");
        }
    }

    @Cacheable(value = "github", key = "'metrics:' + #owner + '/' + #repo + ':' + #since + ':' + #until")
    public String getCommitMetrics(String owner, String repo, String since, String until) {
        try {
            String uri = "/repos/{owner}/{repo}/commits?per_page=100&since={since}&until={until}";
            return gitHubRestClient.get()
                    .uri(uri, owner, repo, since, until)
                    .retrieve()
                    .body(String.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("Repository " + owner + "/" + repo + " not found");
        }
    }

    @Cacheable(value = "github", key = "'branches:' + #owner + '/' + #repo")
    public String listBranches(String owner, String repo) {
        try {
            return gitHubRestClient.get()
                    .uri("/repos/{owner}/{repo}/branches?per_page={perPage}",
                            owner, repo, gitHubProperties.getDefaultPageSize())
                    .retrieve()
                    .body(String.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("Repository " + owner + "/" + repo + " not found");
        }
    }

    @Cacheable(value = "github", key = "'prs:' + #owner + '/' + #repo + ':' + #state")
    public String getPullRequests(String owner, String repo, String state) {
        try {
            return gitHubRestClient.get()
                    .uri("/repos/{owner}/{repo}/pulls?state={state}&per_page={perPage}",
                            owner, repo, state, gitHubProperties.getDefaultPageSize())
                    .retrieve()
                    .body(String.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("Repository " + owner + "/" + repo + " not found");
        }
    }

    @Cacheable(value = "github", key = "'issues:' + #owner + '/' + #repo + ':' + #state + ':' + #labels")
    public String getIssues(String owner, String repo, String state, String labels) {
        try {
            String uri = labels != null && !labels.isBlank()
                    ? "/repos/{owner}/{repo}/issues?state={state}&labels={labels}&per_page={perPage}"
                    : "/repos/{owner}/{repo}/issues?state={state}&per_page={perPage}";
            return gitHubRestClient.get()
                    .uri(uri, owner, repo, state, labels, gitHubProperties.getDefaultPageSize())
                    .retrieve()
                    .body(String.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("Repository " + owner + "/" + repo + " not found");
        }
    }

    @Cacheable(value = "github", key = "'contributors:' + #owner + '/' + #repo")
    public String getContributors(String owner, String repo) {
        try {
            return gitHubRestClient.get()
                    .uri("/repos/{owner}/{repo}/contributors?per_page={perPage}",
                            owner, repo, gitHubProperties.getDefaultPageSize())
                    .retrieve()
                    .body(String.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("Repository " + owner + "/" + repo + " not found");
        }
    }

    @Cacheable(value = "github", key = "'workflows:' + #owner + '/' + #repo + ':' + #workflowId")
    public String getWorkflowRuns(String owner, String repo, String workflowId) {
        try {
            String uri = workflowId != null && !workflowId.isBlank()
                    ? "/repos/{owner}/{repo}/actions/workflows/{workflowId}/runs?per_page={perPage}"
                    : "/repos/{owner}/{repo}/actions/runs?per_page={perPage}";
            return gitHubRestClient.get()
                    .uri(uri, owner, repo, workflowId, gitHubProperties.getDefaultPageSize())
                    .retrieve()
                    .body(String.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("Repository or workflow " + owner + "/" + repo + " not found");
        }
    }

    @Cacheable(value = "github", key = "'releases:' + #owner + '/' + #repo")
    public String getReleases(String owner, String repo) {
        try {
            return gitHubRestClient.get()
                    .uri("/repos/{owner}/{repo}/releases?per_page={perPage}",
                            owner, repo, gitHubProperties.getDefaultPageSize())
                    .retrieve()
                    .body(String.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("Repository " + owner + "/" + repo + " not found");
        }
    }

    @Cacheable(value = "github", key = "'search:' + #query + ':' + #sort + ':' + #order")
    public String searchRepositories(String query, String sort, String order) {
        try {
            return gitHubRestClient.get()
                    .uri("/search/repositories?q={query}&sort={sort}&order={order}&per_page={perPage}",
                            query, sort, order, gitHubProperties.getDefaultPageSize())
                    .retrieve()
                    .body(String.class);
        } catch (HttpClientErrorException ex) {
            throw new IllegalArgumentException("GitHub search failed: " + ex.getMessage());
        }
    }

    @Cacheable(value = "github", key = "'codefreq:' + #owner + '/' + #repo")
    public String getCodeFrequency(String owner, String repo) {
        // GitHub computes stats asynchronously: returns 202 until ready. Retry up to 3×.
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                String[] result = {null};
                HttpStatusCode[] status = {null};
                gitHubRestClient.get()
                        .uri("/repos/{owner}/{repo}/stats/code_frequency", owner, repo)
                        .exchange((req, resp) -> {
                            status[0] = resp.getStatusCode();
                            result[0] = new String(resp.getBody().readAllBytes());
                            return result[0];
                        });
                if (status[0] != null && status[0].value() == 202) {
                    Thread.sleep(1500);
                    continue;
                }
                return result[0];
            } catch (HttpClientErrorException.NotFound ex) {
                throw new ResourceNotFoundException("Repository " + owner + "/" + repo + " not found");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted waiting for GitHub stats", ex);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to fetch code frequency: " + ex.getMessage(), ex);
            }
        }
        return "[]";
    }

    public String createIssue(String owner, String repo, String title, String body, String labels) {
        try {
            String requestBody = buildIssueRequestBody(title, body, labels);
            return gitHubRestClient.post()
                    .uri("/repos/{owner}/{repo}/issues", owner, repo)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
        } catch (HttpClientErrorException ex) {
            throw new IllegalArgumentException("Failed to create issue: " + ex.getMessage());
        }
    }

    private String buildIssueRequestBody(String title, String body, String labels) {
        StringBuilder sb = new StringBuilder("{\"title\":\"").append(escapeJson(title)).append("\"");
        if (body != null && !body.isBlank()) {
            sb.append(",\"body\":\"").append(escapeJson(body)).append("\"");
        }
        if (labels != null && !labels.isBlank()) {
            sb.append(",\"labels\":[");
            String[] labelArr = labels.split(",");
            for (int i = 0; i < labelArr.length; i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(escapeJson(labelArr[i].trim())).append("\"");
            }
            sb.append("]");
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
