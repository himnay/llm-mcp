package com.org.gmail.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "gmail")
public class GmailProperties {
    /**
     * OAuth2 access token for Gmail API. Obtain via Google OAuth2 flow or service account.
     */
    private String accessToken = "";
    /**
     * OAuth2 refresh token. When set, the service will use it to obtain new access tokens
     * automatically when the current token expires.
     */
    private String refreshToken = "";
    /**
     * OAuth2 client ID (required for token refresh).
     */
    private String clientId = "";
    /**
     * OAuth2 client secret (required for token refresh).
     */
    private String clientSecret = "";
    /**
     * Google OAuth2 token endpoint.
     */
    private String tokenEndpoint = "https://oauth2.googleapis.com/token";
    /**
     * Gmail API base URL
     */
    private String apiBaseUrl = "https://gmail.googleapis.com/gmail/v1";
    /**
     * Default user ID ('me' refers to the authenticated user)
     */
    private String userId = "me";
    /**
     * Default page size for list operations
     */
    private int defaultPageSize = 20;
}
