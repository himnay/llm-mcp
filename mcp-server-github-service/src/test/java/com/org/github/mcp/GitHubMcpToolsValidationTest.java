package com.org.github.mcp;

import com.org.github.security.ActingUserContext;
import com.org.github.security.SecurityProperties;
import com.org.github.service.GitHubService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
        tools = new GitHubMcpTools(gitHubService, new ToolExecutionTemplate(securityProperties));
        ActingUserContext.set("test-user");
    }

    @AfterEach
    void tearDown() {
        ActingUserContext.clear();
    }

    @Test
    void getRepository_rejectsBlankOwner() {
        assertThatThrownBy(() -> tools.getRepository("", "spring-framework"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("owner");
    }

    @Test
    void getRepository_rejectsBlankRepo() {
        assertThatThrownBy(() -> tools.getRepository("spring-projects", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("repo");
    }

    @Test
    void getCommitMetrics_rejectsBlankSince() {
        assertThatThrownBy(() -> tools.getCommitMetrics("owner", "repo", "", "2024-12-31T23:59:59Z"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("since");
    }

    @Test
    void getPullRequests_rejectsInvalidState() {
        assertThatThrownBy(() -> tools.getPullRequests("owner", "repo", "invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("state");
    }

    @Test
    void createIssue_rejectsBlankTitle() {
        assertThatThrownBy(() -> tools.createIssue("owner", "repo", "", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
    }

    @Test
    void searchRepositories_rejectsBlankQuery() {
        assertThatThrownBy(() -> tools.searchRepositories("", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("query");
    }
}
