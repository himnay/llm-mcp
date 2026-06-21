package com.org.github.service;

import com.org.github.config.GitHubProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GitHubServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Mock
    private GitHubProperties gitHubProperties;

    @Mock
    private ObjectMapper objectMapper;

    private GitHubService gitHubService;

    @BeforeEach
    void setUp() {
        when(gitHubProperties.getDefaultPageSize()).thenReturn(30);
        gitHubService = new GitHubService(restClient, gitHubProperties, objectMapper);
    }

    @DisplayName("Returns repository details as a JSON string from the GitHub API")
    @Test
    @SuppressWarnings("unchecked")
    void getRepository_returnsJsonString() {
        String expectedJson = "{\"id\":123,\"name\":\"my-repo\",\"full_name\":\"owner/my-repo\"}";

        when(restClient.get()).thenReturn((RestClient.RequestHeadersUriSpec) requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString(), anyString()))
                .thenReturn((RestClient.RequestHeadersSpec) requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(expectedJson);

        String result = gitHubService.getRepository("owner", "my-repo");

        assertThat(result).isEqualTo(expectedJson);
        assertThat(result).contains("my-repo");
    }

    @DisplayName("Returns the list of branches as a JSON array from the GitHub API")
    @Test
    @SuppressWarnings("unchecked")
    void listBranches_returnsJsonArray() {
        String expectedJson = "[{\"name\":\"main\"},{\"name\":\"develop\"}]";

        when(restClient.get()).thenReturn((RestClient.RequestHeadersUriSpec) requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString(), anyString(), any()))
                .thenReturn((RestClient.RequestHeadersSpec) requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(expectedJson);

        String result = gitHubService.listBranches("owner", "my-repo");

        assertThat(result).isEqualTo(expectedJson);
        assertThat(result).contains("main");
    }
}
