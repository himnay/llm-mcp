package com.org.github.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.client.RestClient;

@Slf4j
@Configuration
@EnableConfigurationProperties(GitHubProperties.class)
public class GitHubClientConfig {

    @Bean
    public RestClient gitHubRestClient(GitHubProperties props) {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(props.getApiBaseUrl())
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28");

        if (props.getToken() != null && !props.getToken().isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + props.getToken());
        }
        return builder.build();
    }

    @EventListener(ContextRefreshedEvent.class)
    public void warnIfNoToken(ContextRefreshedEvent event) {
        GitHubProperties props = event.getApplicationContext().getBean(GitHubProperties.class);
        if (props.getToken() == null || props.getToken().isBlank()) {
            log.warn("GitHub token not configured – API calls will be unauthenticated (lower rate limits)");
        }
    }
}
