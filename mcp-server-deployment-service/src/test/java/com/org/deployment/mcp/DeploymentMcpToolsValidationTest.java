package com.org.deployment.mcp;

import com.org.deployment.exception.InvalidToolArgumentException;
import com.org.deployment.exception.WriteGateException;
import com.org.deployment.security.ActingUserContext;
import com.org.deployment.security.RateLimiter;
import com.org.deployment.security.SecurityProperties;
import com.org.deployment.service.DeploymentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
        tools = new DeploymentMcpTools(deploymentService, securityProperties, new RateLimiter(120));
        ActingUserContext.set("test-user");
    }

    @AfterEach
    void tearDown() {
        ActingUserContext.clear();
    }

    // ── createDeployment validation ───────────────────────────────────────────

    @Test
    @DisplayName("Rejects deployment creation with a blank service name")
    void createDeployment_rejectsBlankServiceName() {
        assertThatThrownBy(() -> tools.createDeployment("", "DEV", "2025-06-01T10:00:00", "owner"))
                .isInstanceOf(InvalidToolArgumentException.class)
                .hasMessageContaining("serviceName");
    }

    @Test
    @DisplayName("Rejects deployment creation with a null environment")
    void createDeployment_rejectsNullEnvironment() {
        assertThatThrownBy(() -> tools.createDeployment("svc", null, "2025-06-01T10:00:00", "owner"))
                .isInstanceOf(InvalidToolArgumentException.class)
                .hasMessageContaining("environment");
    }

    @Test
    @DisplayName("Rejects deployment creation with an unrecognized environment value")
    void createDeployment_rejectsInvalidEnvironment() {
        assertThatThrownBy(() -> tools.createDeployment("svc", "PRODUCTION", "2025-06-01T10:00:00", "owner"))
                .isInstanceOf(InvalidToolArgumentException.class)
                .hasMessageContaining("PRODUCTION");
    }

    @Test
    @DisplayName("Rejects deployment creation with an unparseable date string")
    void createDeployment_rejectsInvalidDateFormat() {
        assertThatThrownBy(() -> tools.createDeployment("svc", "DEV", "not-a-date", "owner"))
                .isInstanceOf(InvalidToolArgumentException.class)
                .hasMessageContaining("not-a-date");
    }

    @Test
    @DisplayName("Rejects deployment creation with a blank owner")
    void createDeployment_rejectsBlankOwner() {
        assertThatThrownBy(() -> tools.createDeployment("svc", "DEV", "2025-06-01T10:00:00", "   "))
                .isInstanceOf(InvalidToolArgumentException.class)
                .hasMessageContaining("owner");
    }

    // ── getDeployment validation ──────────────────────────────────────────────

    @Test
    @DisplayName("Rejects fetching a deployment with a null id")
    void getDeployment_rejectsNullId() {
        assertThatThrownBy(() -> tools.getDeployment(null))
                .isInstanceOf(InvalidToolArgumentException.class)
                .hasMessageContaining("id");
    }

    @Test
    @DisplayName("Rejects fetching a deployment with a zero id")
    void getDeployment_rejectsZeroId() {
        assertThatThrownBy(() -> tools.getDeployment(0L))
                .isInstanceOf(InvalidToolArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    @DisplayName("Rejects fetching a deployment with a negative id")
    void getDeployment_rejectsNegativeId() {
        assertThatThrownBy(() -> tools.getDeployment(-5L))
                .isInstanceOf(InvalidToolArgumentException.class)
                .hasMessageContaining("positive");
    }

    // ── rescheduleDeployment validation ──────────────────────────────────────

    @Test
    @DisplayName("Rejects rescheduling with an unparseable date string")
    void rescheduleDeployment_rejectsInvalidDate() {
        assertThatThrownBy(() -> tools.rescheduleDeployment(1L, "bad-date"))
                .isInstanceOf(InvalidToolArgumentException.class)
                .hasMessageContaining("bad-date");
    }

    @Test
    @DisplayName("Rejects rescheduling with a blank date string")
    void rescheduleDeployment_rejectsBlankDate() {
        assertThatThrownBy(() -> tools.rescheduleDeployment(1L, ""))
                .isInstanceOf(InvalidToolArgumentException.class)
                .hasMessageContaining("newTime");
    }

    // ── write gate ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Rejects cancellation by the default user when the write gate is enabled")
    void cancelDeployment_rejectsDefaultUserWhenWriteGateEnabled() {
        securityProperties.setRequireUserForWrites(true);
        ActingUserContext.set("system"); // same as defaultUser
        assertThatThrownBy(() -> tools.cancelDeployment(1L))
                .isInstanceOf(WriteGateException.class)
                .hasMessageContaining("X-Acting-User");
    }

    @Test
    @DisplayName("Allows cancellation by a named acting user when the write gate is enabled")
    void cancelDeployment_allowsNamedUserWhenWriteGateEnabled() {
        securityProperties.setRequireUserForWrites(true);
        ActingUserContext.set("jane");
        org.mockito.Mockito.when(deploymentService.cancelDeployment(1L))
                .thenReturn(com.org.deployment.model.Deployment.builder().id(1L).build());
        // a named acting user must pass the write gate
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> tools.cancelDeployment(1L));
    }
}
