package com.org.gmail.service;

import com.org.gmail.config.GmailProperties;
import com.org.gmail.config.GmailTokenManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GmailServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    @SuppressWarnings("rawtypes")
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Mock
    private GmailProperties gmailProperties;

    @Mock
    private GmailTokenManager tokenManager;

    private GmailService gmailService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(gmailProperties.getUserId()).thenReturn("me");
        when(tokenManager.getValidToken()).thenReturn("test-access-token");

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(), any(), any()))
                .thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString(), anyString()))
                .thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString()))
                .thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.header(anyString(), anyString()))
                .thenReturn(requestHeadersUriSpec);

        gmailService = new GmailService(restClient, gmailProperties, tokenManager);
    }

    @Test
    @DisplayName("Returns the raw JSON string response when listing emails")
    @SuppressWarnings("unchecked")
    void listEmails_returnsJsonString() {
        String expectedJson = "{\"messages\":[{\"id\":\"msg1\",\"threadId\":\"thread1\"}]}";
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(expectedJson);

        String result = gmailService.listEmails("INBOX", 10);

        assertThat(result).isEqualTo(expectedJson);
        assertThat(result).contains("msg1");
    }

    @Test
    @DisplayName("Refreshes the access token and retries when the API returns 401")
    @SuppressWarnings("unchecked")
    void listEmails_refreshesToken_on401() {
        String expectedJson = "{\"messages\":[]}";

        // First call throws 401, second call succeeds
        when(requestHeadersUriSpec.retrieve())
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED))
                .thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(expectedJson);

        String result = gmailService.listEmails("INBOX", 10);

        // Token refresh should have been triggered
        verify(tokenManager, atLeastOnce()).refreshToken();
        assertThat(result).isEqualTo(expectedJson);
    }

    @Test
    @DisplayName("Returns the raw JSON string response when listing labels")
    @SuppressWarnings("unchecked")
    void listLabels_returnsJsonString() {
        String expectedJson = "{\"labels\":[{\"id\":\"INBOX\",\"name\":\"INBOX\"}]}";

        when(requestHeadersUriSpec.uri(anyString(), anyString()))
                .thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(expectedJson);

        String result = gmailService.listLabels();

        assertThat(result).isEqualTo(expectedJson);
        assertThat(result).contains("INBOX");
    }
}
