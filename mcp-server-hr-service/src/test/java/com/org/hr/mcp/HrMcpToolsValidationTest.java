package com.org.hr.mcp;

import com.org.hr.config.McpOutputProperties;
import com.org.hr.config.SecurityProperties;
import com.org.hr.service.HRService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for input validation in {@link HrMcpTools}.
 * No Spring context, no DB.
 */
@ExtendWith(MockitoExtension.class)
class HrMcpToolsValidationTest {

    @Mock
    private HRService hrService;

    private HrMcpTools tools;

    @BeforeEach
    void setUp() {
        SecurityProperties sec = new SecurityProperties();
        McpOutputProperties output = new McpOutputProperties();
        tools = new HrMcpTools(hrService, sec, output);
    }

    // ------------------------------------------------------------------
    // applyLeave — blank / null username
    // ------------------------------------------------------------------

    @Test
    @DisplayName("applyLeave throws IllegalArgumentException when username is blank")
    void applyLeave_blankUsername_throwsIllegalArgument() {
        assertThatThrownBy(() -> tools.applyLeave("", "2025-06-01"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("username");
    }

    @Test
    @DisplayName("applyLeave throws IllegalArgumentException when username is null")
    void applyLeave_nullUsername_throwsIllegalArgument() {
        assertThatThrownBy(() -> tools.applyLeave(null, "2025-06-01"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("username");
    }

    // ------------------------------------------------------------------
    // applyLeave — invalid date format
    // ------------------------------------------------------------------

    @Test
    @DisplayName("applyLeave throws IllegalArgumentException when date is not in yyyy-MM-dd format")
    void applyLeave_badDate_throwsIllegalArgument() {
        assertThatThrownBy(() -> tools.applyLeave("alice", "not-a-date"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("yyyy-MM-dd");
    }

    @Test
    @DisplayName("applyLeave throws IllegalArgumentException when date is blank")
    void applyLeave_blankDate_throwsIllegalArgument() {
        assertThatThrownBy(() -> tools.applyLeave("alice", "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("date");
    }

    // ------------------------------------------------------------------
    // findReplacement — blank / null username
    // ------------------------------------------------------------------

    @Test
    @DisplayName("findReplacement throws IllegalArgumentException when username is blank")
    void findReplacement_blankUsername_throwsIllegalArgument() {
        assertThatThrownBy(() -> tools.findReplacement("", "2025-06-01"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("username");
    }

    // ------------------------------------------------------------------
    // findReplacement — invalid date
    // ------------------------------------------------------------------

    @Test
    @DisplayName("findReplacement throws IllegalArgumentException when date is not in yyyy-MM-dd format")
    void findReplacement_badDate_throwsIllegalArgument() {
        assertThatThrownBy(() -> tools.findReplacement("bob", "01/06/2025"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("yyyy-MM-dd");
    }

    // ------------------------------------------------------------------
    // applyLeave — requireUserForWrites + default user → IllegalState
    // ------------------------------------------------------------------

    @Test
    @DisplayName("applyLeave throws IllegalStateException when writes require a user but only the default user is available")
    void applyLeave_requireUserForWritesAndDefaultUser_throwsIllegalState() {
        SecurityProperties sec = new SecurityProperties();
        sec.setRequireUserForWrites(true);
        sec.setDefaultUser("system");

        HrMcpTools strictTools = new HrMcpTools(hrService, sec, new McpOutputProperties());

        // ActingUserContext is empty → fallback to defaultUser "system"
        assertThatThrownBy(() -> strictTools.applyLeave("alice", "2025-06-01"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("X-Acting-User");
    }

    // ------------------------------------------------------------------
    // Happy path — valid input reaches service
    // ------------------------------------------------------------------

    @Test
    @DisplayName("applyLeave succeeds and reaches the service when input is valid")
    void applyLeave_validInput_succeeds() {
        when(hrService.applyLeave(anyString(), any())).thenReturn(null);
        // Should not throw
        tools.applyLeave("alice", "2025-06-01");
    }
}
