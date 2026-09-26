package com.org.gmail.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

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
                .requestFactory(boundedTimeouts())
                .baseUrl(props.getApiBaseUrl())
                .build();
    }

    /** Handles warn if no token. */
    @EventListener(ContextRefreshedEvent.class)
    public void warnIfNoToken(ContextRefreshedEvent event) {
        GmailProperties props = event.getApplicationContext().getBean(GmailProperties.class);
        if (props.getAccessToken() == null || props.getAccessToken().isBlank()) {
            log.warn("Gmail access token not configured – API calls will fail with 401. "
                    + "Set GMAIL_ACCESS_TOKEN or configure gmail.refresh-token for automatic refresh.");
        }
    }

    /** Bounded connect/read timeouts — the JDK client's default read timeout is infinite, so a hung upstream would pin the calling thread. */
    private static JdkClientHttpRequestFactory boundedTimeouts() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(30));
        return factory;
    }
}
