package com.org.gmail.service;

import com.org.gmail.config.GmailProperties;
import com.org.gmail.config.GmailTokenManager;
import com.org.gmail.exception.ExternalServiceException;
import com.org.gmail.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class GmailService {

    private final RestClient gmailRestClient;
    private final GmailProperties gmailProperties;
    private final GmailTokenManager tokenManager;

    /**
     * Returns the Authorization header value with the current valid token.
     * On 401, callers should invoke {@link GmailTokenManager#refreshToken()} and retry once.
     */
    private String bearerToken() {
        return "Bearer " + tokenManager.getValidToken();
    }

    /**
     * Executes a RestClient call that supplies a String result.
     * Retries once after refreshing the token if a 401 is returned.
     */
    private String executeWithTokenRefresh(java.util.function.Supplier<String> call) {
        try {
            return call.get();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.info("Received 401 from Gmail API — refreshing token and retrying");
                tokenManager.refreshToken();
                return call.get();
            }
            throw e;
        }
    }

    public String listEmails(String labelIds, int maxResults) {
        String userId = gmailProperties.getUserId();
        String labels = (labelIds != null && !labelIds.isBlank()) ? labelIds : "INBOX";
        try {
            return executeWithTokenRefresh(() -> gmailRestClient.get()
                    .uri("/users/{userId}/messages?labelIds={labelIds}&maxResults={maxResults}",
                            userId, labels, maxResults)
                    .header("Authorization", bearerToken())
                    .retrieve()
                    .body(String.class));
        } catch (HttpClientErrorException ex) {
            throw new ExternalServiceException("Failed to list emails: " + ex.getMessage());
        }
    }

    public String getEmail(String messageId, String format) {
        String userId = gmailProperties.getUserId();
        String resolvedFormat = (format != null && !format.isBlank()) ? format : "full";
        try {
            return executeWithTokenRefresh(() -> gmailRestClient.get()
                    .uri("/users/{userId}/messages/{messageId}?format={format}", userId, messageId, resolvedFormat)
                    .header("Authorization", bearerToken())
                    .retrieve()
                    .body(String.class));
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("Email message " + messageId + " not found");
        }
    }

    public String searchEmails(String query, int maxResults) {
        String userId = gmailProperties.getUserId();
        try {
            return executeWithTokenRefresh(() -> gmailRestClient.get()
                    .uri("/users/{userId}/messages?q={query}&maxResults={maxResults}", userId, query, maxResults)
                    .header("Authorization", bearerToken())
                    .retrieve()
                    .body(String.class));
        } catch (HttpClientErrorException ex) {
            throw new ExternalServiceException("Gmail search failed: " + ex.getMessage());
        }
    }

    public String getEmailThread(String threadId) {
        String userId = gmailProperties.getUserId();
        try {
            return executeWithTokenRefresh(() -> gmailRestClient.get()
                    .uri("/users/{userId}/threads/{threadId}?format=full", userId, threadId)
                    .header("Authorization", bearerToken())
                    .retrieve()
                    .body(String.class));
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("Thread " + threadId + " not found");
        }
    }

    public String getProfile() {
        String userId = gmailProperties.getUserId();
        try {
            return executeWithTokenRefresh(() -> gmailRestClient.get()
                    .uri("/users/{userId}/profile", userId)
                    .header("Authorization", bearerToken())
                    .retrieve()
                    .body(String.class));
        } catch (HttpClientErrorException ex) {
            throw new ExternalServiceException("Failed to get profile: " + ex.getMessage());
        }
    }

    public String listLabels() {
        String userId = gmailProperties.getUserId();
        try {
            return executeWithTokenRefresh(() -> gmailRestClient.get()
                    .uri("/users/{userId}/labels", userId)
                    .header("Authorization", bearerToken())
                    .retrieve()
                    .body(String.class));
        } catch (HttpClientErrorException ex) {
            throw new ExternalServiceException("Failed to list labels: " + ex.getMessage());
        }
    }

    public String markAsRead(String messageId) {
        String userId = gmailProperties.getUserId();
        try {
            return executeWithTokenRefresh(() -> gmailRestClient.post()
                    .uri("/users/{userId}/messages/{messageId}/modify", userId, messageId)
                    .header("Authorization", bearerToken())
                    .header("Content-Type", "application/json")
                    .body("{\"removeLabelIds\":[\"UNREAD\"]}")
                    .retrieve()
                    .body(String.class));
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("Email message " + messageId + " not found");
        }
    }

    public String markAsUnread(String messageId) {
        String userId = gmailProperties.getUserId();
        try {
            return executeWithTokenRefresh(() -> gmailRestClient.post()
                    .uri("/users/{userId}/messages/{messageId}/modify", userId, messageId)
                    .header("Authorization", bearerToken())
                    .header("Content-Type", "application/json")
                    .body("{\"addLabelIds\":[\"UNREAD\"]}")
                    .retrieve()
                    .body(String.class));
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("Email message " + messageId + " not found");
        }
    }

    public String createDraft(String to, String subject, String body) {
        String userId = gmailProperties.getUserId();
        String rawEmail = buildRawEmail(to, subject, body);
        String requestBody = "{\"message\":{\"raw\":\"" + rawEmail + "\"}}";
        try {
            return executeWithTokenRefresh(() -> gmailRestClient.post()
                    .uri("/users/{userId}/drafts", userId)
                    .header("Authorization", bearerToken())
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class));
        } catch (HttpClientErrorException ex) {
            throw new ExternalServiceException("Failed to create draft: " + ex.getMessage());
        }
    }

    public String sendEmail(String to, String subject, String body) {
        String userId = gmailProperties.getUserId();
        String rawEmail = buildRawEmail(to, subject, body);
        String requestBody = "{\"raw\":\"" + rawEmail + "\"}";
        try {
            return executeWithTokenRefresh(() -> gmailRestClient.post()
                    .uri("/users/{userId}/messages/send", userId)
                    .header("Authorization", bearerToken())
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class));
        } catch (HttpClientErrorException ex) {
            throw new ExternalServiceException("Failed to send email: " + ex.getMessage());
        }
    }

    public String deleteEmail(String messageId) {
        String userId = gmailProperties.getUserId();
        try {
            return executeWithTokenRefresh(() -> gmailRestClient.post()
                    .uri("/users/{userId}/messages/{messageId}/trash", userId, messageId)
                    .header("Authorization", bearerToken())
                    .retrieve()
                    .body(String.class));
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("Email message " + messageId + " not found");
        }
    }

    public String getEmailsByLabel(String labelId, int maxResults) {
        String userId = gmailProperties.getUserId();
        try {
            return executeWithTokenRefresh(() -> gmailRestClient.get()
                    .uri("/users/{userId}/messages?labelIds={labelId}&maxResults={maxResults}",
                            userId, labelId, maxResults)
                    .header("Authorization", bearerToken())
                    .retrieve()
                    .body(String.class));
        } catch (HttpClientErrorException ex) {
            throw new ExternalServiceException("Failed to get emails by label: " + ex.getMessage());
        }
    }

    private String buildRawEmail(String to, String subject, String body) {
        String emailContent = "To: " + to + "\r\n"
                + "Subject: " + subject + "\r\n"
                + "Content-Type: text/plain; charset=UTF-8\r\n"
                + "\r\n"
                + body;
        return java.util.Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(emailContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
