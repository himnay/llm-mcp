package com.org.deployment.mcp;

import com.org.deployment.security.ActingUserContext;
import com.org.deployment.security.SecurityProperties;
import com.org.deployment.service.DeploymentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit tests for input-validation guards in {@link DeploymentMcpTools} — no Spring context, no DB.
 */
@ExtendWith(MockitoExtension.class)
class DeploymentMcpToolsValidationTest {

    @Mock
    private DeploymentService deploymentService;

    private SecurityProperties securityProperties;
    private DeploymentMcpTools tools;

    @BeforeEach
    void setUp() {
        securityProperties = new SecurityProperties();
        securityProperties.setToken("");
        securityProperties.setDefaultUser("system");
        securityProperties.setRequireUserForWrites(false);
        tools = new DeploymentMcpTools(deploymentService, securityProperties);
        ActingUserContext.set("test-user");
    }

    @AfterEach
    void tearDown() {
        ActingUserContext.clear();
    }

    // ── createDeployment validation ───────────────────────────────────────────

    @Test
    void createDeployment_rejectsBlankServiceName() {
        assertThatThrownBy(() -> tools.createDeployment("", "DEV", "2025-06-01T10:00:00", "owner"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("serviceName");
    }

    @Test
    void createDeployment_rejectsNullEnvironment() {
        assertThatThrownBy(() -> tools.createDeployment("svc", null, "2025-06-01T10:00:00", "owner"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("environment");
    }

    @Test
    void createDeployment_rejectsInvalidEnvironment() {
        assertThatThrownBy(() -> tools.createDeployment("svc", "STAGING", "2025-06-01T10:00:00", "owner"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("STAGING");
    }

    @Test
    void createDeployment_rejectsInvalidDateFormat() {
        assertThatThrownBy(() -> tools.createDeployment("svc", "DEV", "not-a-date", "owner"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not-a-date");
    }

    @Test
    void createDeployment_rejectsBlankOwner() {
        assertThatThrownBy(() -> tools.createDeployment("svc", "DEV", "2025-06-01T10:00:00", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("owner");
    }

    // ── getDeployment validation ──────────────────────────────────────────────

    @Test
    void getDeployment_rejectsNullId() {
        assertThatThrownBy(() -> tools.getDeployment(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id");
    }

    @Test
    void getDeployment_rejectsZeroId() {
        assertThatThrownBy(() -> tools.getDeployment(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void getDeployment_rejectsNegativeId() {
        assertThatThrownBy(() -> tools.getDeployment(-5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    // ── rescheduleDeployment validation ──────────────────────────────────────

    @Test
    void rescheduleDeployment_rejectsInvalidDate() {
        assertThatThrownBy(() -> tools.rescheduleDeployment(1L, "bad-date"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bad-date");
    }

    @Test
    void rescheduleDeployment_rejectsBlankDate() {
        assertThatThrownBy(() -> tools.rescheduleDeployment(1L, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("newTime");
    }

    // ── write gate ────────────────────────────────────────────────────────────

    @Test
    void cancelDeployment_rejectsDefaultUserWhenWriteGateEnabled() {
        securityProperties.setRequireUserForWrites(true);
        ActingUserContext.set("system"); // same as defaultUser
        assertThatThrownBy(() -> tools.cancelDeployment(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("X-Acting-User");
    }

    @Test
    void cancelDeployment_allowsNamedUserWhenWriteGateEnabled() {
        securityProperties.setRequireUserForWrites(true);
        ActingUserContext.set("jane");
        // no stub needed — we only care that NO IllegalStateException is thrown before the service call
        // The service mock returns null by default, which is fine for this test scope
        org.mockito.Mockito.when(deploymentService.cancelDeployment(1L)).thenReturn(null);
        // should not throw IllegalStateException
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> tools.cancelDeployment(1L));
    }
}
