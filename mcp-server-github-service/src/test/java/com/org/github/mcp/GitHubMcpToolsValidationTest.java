package com.org.github.mcp;

import com.org.github.security.ActingUserContext;
import com.org.github.security.RateLimiter;
import com.org.github.security.SecurityProperties;
import com.org.github.service.GitHubService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class GitHubMcpToolsValidationTest {

    @Mock
    private GitHubService gitHubService;

    private SecurityProperties securityProperties;
    private GitHubMcpTools tools;

    @BeforeEach
    void setUp() {
        securityProperties = new SecurityProperties();
        securityProperties.setToken("");
        securityProperties.setDefaultUser("system");
        securityProperties.setRequireUserForWrites(false);
        tools = new GitHubMcpTools(gitHubService, new ToolExecutionTemplate(securityProperties, new RateLimiter(120)));
        ActingUserContext.set("test-user");
    }

    @AfterEach
    void tearDown() {
        ActingUserContext.clear();
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when owner is blank")
    void getRepository_rejectsBlankOwner() {
        assertThatThrownBy(() -> tools.getRepository("", "spring-framework"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("owner");
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when repo is blank")
    void getRepository_rejectsBlankRepo() {
        assertThatThrownBy(() -> tools.getRepository("spring-projects", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("repo");
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when since is blank")
    void getCommitMetrics_rejectsBlankSince() {
        assertThatThrownBy(() -> tools.getCommitMetrics("owner", "repo", "", "2024-12-31T23:59:59Z"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("since");
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when pull request state is invalid")
    void getPullRequests_rejectsInvalidState() {
        assertThatThrownBy(() -> tools.getPullRequests("owner", "repo", "invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("state");
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when issue title is blank")
    void createIssue_rejectsBlankTitle() {
        assertThatThrownBy(() -> tools.createIssue("owner", "repo", "", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when search query is blank")
    void searchRepositories_rejectsBlankQuery() {
        assertThatThrownBy(() -> tools.searchRepositories("", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("query");
    }
}
