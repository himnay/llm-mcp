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

    /**
     * Creates a base RestClient for the Gmail API. The Authorization header is
     * set dynamically per request by GmailService using GmailTokenManager so
     * that token refreshes are picked up without rebuilding the client.
     */
    @Bean
    public RestClient gmailRestClient(GmailProperties props) {
        return RestClient.builder()
                .baseUrl(props.getApiBaseUrl())
                .build();
    }

    @EventListener(ContextRefreshedEvent.class)
    public void warnIfNoToken(ContextRefreshedEvent event) {
        GmailProperties props = event.getApplicationContext().getBean(GmailProperties.class);
        if (props.getAccessToken() == null || props.getAccessToken().isBlank()) {
            log.warn("Gmail access token not configured – API calls will fail with 401. "
                    + "Set GMAIL_ACCESS_TOKEN or configure gmail.refresh-token for automatic refresh.");
        }
    }
}
