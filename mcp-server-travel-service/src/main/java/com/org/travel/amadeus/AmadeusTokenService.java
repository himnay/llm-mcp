package com.org.travel.amadeus;

import com.org.travel.amadeus.model.TokenResponse;
import com.org.travel.config.AmadeusProperties;
import com.org.travel.exception.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Fetches and caches the Amadeus OAuth2 client-credentials token.
 * Refreshes automatically 60 seconds before expiry to avoid mid-request failures.
 */
@Slf4j
@Service
public class AmadeusTokenService {

    private static final long REFRESH_BUFFER_SECONDS = 60;

    private final RestClient restClient;
    private final AmadeusProperties props;
    private final ReentrantLock lock = new ReentrantLock();

    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.EPOCH;

    public AmadeusTokenService(@Qualifier("amadeusRestClient") RestClient restClient,
                               AmadeusProperties props) {
        this.restClient = restClient;
        this.props = props;
    }

    public String getToken() {
        if (isValid()) {
            return cachedToken;
        }
        lock.lock();
        try {
            // double-check inside lock
            if (isValid()) {
                return cachedToken;
            }
            return fetchToken();
        } finally {
            lock.unlock();
        }
    }

    private boolean isValid() {
        return cachedToken != null
                && Instant.now().isBefore(expiresAt.minusSeconds(REFRESH_BUFFER_SECONDS));
    }

    private String fetchToken() {
        log.debug("Fetching new Amadeus OAuth2 token");
        String body = "grant_type=client_credentials"
                + "&client_id=" + props.getClientId()
                + "&client_secret=" + props.getClientSecret();

        TokenResponse response = restClient.post()
                .uri("/v1/security/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(TokenResponse.class);

        if (response == null || response.getAccessToken() == null) {
            throw new ExternalServiceException("Amadeus token response was null or missing access_token");
        }

        cachedToken = response.getAccessToken();
        expiresAt = Instant.now().plusSeconds(response.getExpiresIn());
        log.info("Amadeus token refreshed — expires in {}s", response.getExpiresIn());
        return cachedToken;
    }
}
