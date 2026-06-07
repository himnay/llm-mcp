package com.org.gmail.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.client.RestClient;

@Slf4j
@Configuration
@EnableConfigurationProperties(GmailProperties.class)
public class GmailClientConfig {

    @Bean
    public RestClient gmailRestClient(GmailProperties props) {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(props.getApiBaseUrl());

        if (props.getAccessToken() != null && !props.getAccessToken().isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + props.getAccessToken());
        }
        return builder.build();
    }

    @EventListener(ContextRefreshedEvent.class)
    public void warnIfNoToken(ContextRefreshedEvent event) {
        GmailProperties props = event.getApplicationContext().getBean(GmailProperties.class);
        if (props.getAccessToken() == null || props.getAccessToken().isBlank()) {
            log.warn("Gmail access token not configured – API calls will fail with 401");
        }
    }
}
