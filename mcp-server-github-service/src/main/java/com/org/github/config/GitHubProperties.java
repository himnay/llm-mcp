package com.org.github.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "github")
public class GitHubProperties {
    /** Personal access token or fine-grained token for GitHub API auth */
    private String token = "";
    /** GitHub API base URL */
    private String apiBaseUrl = "https://api.github.com";
    /** Default page size for list operations */
    private int defaultPageSize = 30;
}
