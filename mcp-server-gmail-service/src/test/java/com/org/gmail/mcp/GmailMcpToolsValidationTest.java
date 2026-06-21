package com.org.gmail.mcp;

import com.org.gmail.security.ActingUserContext;
import com.org.gmail.security.RateLimiter;
import com.org.gmail.security.SecurityProperties;
import com.org.gmail.service.GmailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class GmailMcpToolsValidationTest {

    @Mock
    private GmailService gmailService;

    private SecurityProperties securityProperties;
    private GmailMcpTools tools;

    @BeforeEach
    void setUp() {
        securityProperties = new SecurityProperties();
        securityProperties.setToken("");
        securityProperties.setDefaultUser("system");
        securityProperties.setRequireUserForWrites(false);
        tools = new GmailMcpTools(gmailService, securityProperties, new RateLimiter(120));
        ActingUserContext.set("test-user");
    }

    @AfterEach
    void tearDown() {
        ActingUserContext.clear();
    }

    @DisplayName("Rejects getEmail call when messageId is blank")
    @Test
    void getEmail_rejectsBlankMessageId() {
        assertThatThrownBy(() -> tools.getEmail("", "full"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messageId");
    }

    @DisplayName("Rejects searchEmails call when query is blank")
    @Test
    void searchEmails_rejectsBlankQuery() {
        assertThatThrownBy(() -> tools.searchEmails("", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("query");
    }

    @DisplayName("Rejects getEmailThread call when threadId is blank")
    @Test
    void getEmailThread_rejectsBlankThreadId() {
        assertThatThrownBy(() -> tools.getEmailThread(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("threadId");
    }

    @DisplayName("Rejects sendEmail call when recipient 'to' address is blank")
    @Test
    void sendEmail_rejectsBlankTo() {
        assertThatThrownBy(() -> tools.sendEmail("", "subject", "body"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("to");
    }

    @DisplayName("Rejects createDraft call when subject is blank")
    @Test
    void createDraft_rejectsBlankSubject() {
        assertThatThrownBy(() -> tools.createDraft("to@example.com", "", "body"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("subject");
    }

    @DisplayName("Rejects deleteEmail call when messageId is blank")
    @Test
    void deleteEmail_rejectsBlankMessageId() {
        assertThatThrownBy(() -> tools.deleteEmail(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messageId");
    }

    @DisplayName("Rejects markAsRead call when messageId is blank")
    @Test
    void markAsRead_rejectsBlankMessageId() {
        assertThatThrownBy(() -> tools.markAsRead(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messageId");
    }

    @DisplayName("Rejects getEmailsByLabel call when labelId is blank")
    @Test
    void getEmailsByLabel_rejectsBlankLabelId() {
        assertThatThrownBy(() -> tools.getEmailsByLabel("", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("labelId");
    }
}
