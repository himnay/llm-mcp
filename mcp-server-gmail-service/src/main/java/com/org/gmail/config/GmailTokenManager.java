package com.org.gmail.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages the Gmail OAuth2 access token lifecycle.
 * If a refresh token is configured, proactively refreshes the access token
 * ~58 minutes into its 1-hour lifetime. Also exposes {@link #refreshToken()}
 * for on-demand refresh (e.g. after a 401 response).
 */
@Slf4j
@Component
public class GmailTokenManager {

    private static final long REFRESH_BUFFER_SECONDS = 60;

    private final GmailProperties props;
    private final ReentrantLock lock = new ReentrantLock();

    private volatile String currentToken;
    private volatile Instant tokenExpiry = Instant.EPOCH;

    public GmailTokenManager(GmailProperties props) {
        this.props = props;
        this.currentToken = props.getAccessToken();
        // Assume initial token is valid for 1 hour if it is set
        if (currentToken != null && !currentToken.isBlank()) {
            this.tokenExpiry = Instant.now().plusSeconds(3600);
        }
    }

    /**
     * Returns a valid access token, refreshing if necessary.
     */
    public String getValidToken() {
        if (isValid()) {
            return currentToken;
        }
        if (canRefresh()) {
            lock.lock();
            try {
                if (isValid()) return currentToken;
                refreshToken();
            } finally {
                lock.unlock();
            }
        }
        return currentToken;
    }

    /**
     * Force-refresh the access token using the configured refresh token.
     * Called on 401 responses or by the scheduler.
     */
    public void refreshToken() {
        if (!canRefresh()) {
            log.warn("Cannot refresh Gmail token — refresh token, client ID, or client secret not configured");
            return;
        }
        try {
            log.info("Refreshing Gmail OAuth2 access token");
            // A form map is URL-encoded by the converter; hand-concatenating the values broke on any
            // secret or refresh token containing '+', '&', '=' or '/'.
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("refresh_token", props.getRefreshToken());
            body.add("client_id", props.getClientId());
            body.add("client_secret", props.getClientSecret());

            RestClient tokenClient = RestClient.builder()
                .requestFactory(boundedTimeouts())
                    .baseUrl(props.getTokenEndpoint())
                    .build();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = tokenClient.post()
                    .uri("")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("access_token")) {
                currentToken = (String) response.get("access_token");
                int expiresIn = response.containsKey("expires_in")
                        ? ((Number) response.get("expires_in")).intValue()
                        : 3600;
                tokenExpiry = Instant.now().plusSeconds(expiresIn);
                log.info("Gmail access token refreshed — expires in {}s", expiresIn);
            } else {
                log.warn("Gmail token refresh response did not contain access_token");
            }
        } catch (Exception e) {
            log.error("Failed to refresh Gmail access token: {}", e.getMessage(), e);
        }
    }

    /**
     * Proactive scheduled refresh — runs every ~58 minutes (3500 seconds).
     */
    @Scheduled(fixedDelay = 3500000)
    public void scheduledRefresh() {
        if (canRefresh() && !isValid()) {
            refreshToken();
        }
    }

    private boolean isValid() {
        return currentToken != null && !currentToken.isBlank()
                && Instant.now().isBefore(tokenExpiry.minusSeconds(REFRESH_BUFFER_SECONDS));
    }

    private boolean canRefresh() {
        return props.getRefreshToken() != null && !props.getRefreshToken().isBlank()
                && props.getClientId() != null && !props.getClientId().isBlank()
                && props.getClientSecret() != null && !props.getClientSecret().isBlank();
    }

    /** Bounded connect/read timeouts — the JDK client's default read timeout is infinite, so a hung upstream would pin the calling thread. */
    private static JdkClientHttpRequestFactory boundedTimeouts() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(30));
        return factory;
    }
}
