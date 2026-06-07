package com.org.gmail.mcp;

import com.org.gmail.security.ActingUserContext;
import com.org.gmail.security.SecurityProperties;
import com.org.gmail.service.GmailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
        tools = new GmailMcpTools(gmailService, securityProperties);
        ActingUserContext.set("test-user");
    }

    @AfterEach
    void tearDown() {
        ActingUserContext.clear();
    }

    @Test
    void getEmail_rejectsBlankMessageId() {
        assertThatThrownBy(() -> tools.getEmail("", "full"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messageId");
    }

    @Test
    void searchEmails_rejectsBlankQuery() {
        assertThatThrownBy(() -> tools.searchEmails("", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("query");
    }

    @Test
    void getEmailThread_rejectsBlankThreadId() {
        assertThatThrownBy(() -> tools.getEmailThread(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("threadId");
    }

    @Test
    void sendEmail_rejectsBlankTo() {
        assertThatThrownBy(() -> tools.sendEmail("", "subject", "body"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("to");
    }

    @Test
    void createDraft_rejectsBlankSubject() {
        assertThatThrownBy(() -> tools.createDraft("to@example.com", "", "body"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("subject");
    }

    @Test
    void deleteEmail_rejectsBlankMessageId() {
        assertThatThrownBy(() -> tools.deleteEmail(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messageId");
    }

    @Test
    void markAsRead_rejectsBlankMessageId() {
        assertThatThrownBy(() -> tools.markAsRead(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messageId");
    }

    @Test
    void getEmailsByLabel_rejectsBlankLabelId() {
        assertThatThrownBy(() -> tools.getEmailsByLabel("", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("labelId");
    }
}
