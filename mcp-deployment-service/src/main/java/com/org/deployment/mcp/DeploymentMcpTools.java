package com.org.deployment.mcp;

import com.org.deployment.model.Deployment;
import com.org.deployment.model.DeploymentEnvironment;
import com.org.deployment.security.ActingUserContext;
import com.org.deployment.security.SecurityProperties;
import com.org.deployment.service.DeploymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * MCP tool surface for deployment management.
 *
 * <p>Every tool:
 * <ul>
 *   <li>Validates its inputs (null/blank/enum/date guards → {@link IllegalArgumentException})</li>
 *   <li>Logs tool name, acting user, sanitised argument summary, outcome and latency at INFO</li>
 *   <li>Write/destructive methods additionally enforce the write-gate when
 *       {@code mcp.security.requireUserForWrites} is enabled</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
class DeploymentMcpTools {

    private final DeploymentService deploymentService;
    private final SecurityProperties securityProperties;

    // ─────────────────────────────── READ tools ──────────────────────────────

    @Tool(name = "getDeployments", description = "Get all deployments")
    public String getDeployments() {
        String actingUser = resolveUser();
        long start = System.nanoTime();
        try {
            List<Deployment> result = deploymentService.getDeployments();
            String payload = result.toString();
            String capped = OutputSizeCapUtil.cap(payload);
            log.info("TOOL getDeployments | user={} resultCount={} latencyMs={}",
                    actingUser, result.size(), elapsedMs(start));
            return capped;
        } catch (Exception ex) {
            log.error("TOOL getDeployments | user={} outcome=ERROR latencyMs={} error={}",
                    actingUser, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    @Tool(name = "getDeployment", description = "Get a deployment by its id")
    public String getDeployment(Long id) {
        String actingUser = resolveUser();
        long start = System.nanoTime();
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (id <= 0) {
            throw new IllegalArgumentException("id must be a positive number, got: " + id);
        }
        try {
            Deployment result = deploymentService.getDeployment(id);
            log.info("TOOL getDeployment | user={} id={} latencyMs={}",
                    actingUser, id, elapsedMs(start));
            return result.toString();
        } catch (Exception ex) {
            log.error("TOOL getDeployment | user={} id={} outcome=ERROR latencyMs={} error={}",
                    actingUser, id, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    // ─────────────────────────────── WRITE tools ─────────────────────────────

    @Tool(
            name = "createDeployment",
            description = "Create a new deployment. Provide serviceName, environment (DEV, QA, PROD), "
                    + "scheduledTime (ISO format yyyy-MM-ddTHH:mm:ss or with offset) and owner"
    )
    public String createDeployment(String serviceName,
                                   String environment,
                                   String scheduledTime,
                                   String owner) {
        String actingUser = resolveUser();
        enforceWriteGate(actingUser);
        long start = System.nanoTime();

        // ── validation ────────────────────────────────────────────────────────
        requireNonBlank(serviceName, "serviceName");
        requireNonBlank(environment, "environment");
        requireNonBlank(scheduledTime, "scheduledTime");
        requireNonBlank(owner, "owner");

        DeploymentEnvironment env = parseEnvironment(environment);
        LocalDateTime scheduled = parseScheduledTime(scheduledTime);

        try {
            Deployment result = deploymentService.createDeployment(serviceName, env, scheduled, owner);
            log.info("AUDIT createDeployment | user={} serviceName={} environment={} scheduledTime={} owner={} "
                            + "newId={} outcome=SUCCESS latencyMs={}",
                    actingUser, serviceName, environment, scheduledTime, owner,
                    result.getId(), elapsedMs(start));
            return result.toString();
        } catch (Exception ex) {
            log.error("AUDIT createDeployment | user={} serviceName={} environment={} outcome=ERROR latencyMs={} error={}",
                    actingUser, serviceName, environment, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    @Tool(
            name = "assignOwner",
            description = "Assign a new owner to an existing deployment"
    )
    public String assignOwner(Long id, String newOwner) {
        String actingUser = resolveUser();
        enforceWriteGate(actingUser);
        long start = System.nanoTime();

        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (id <= 0) {
            throw new IllegalArgumentException("id must be a positive number, got: " + id);
        }
        requireNonBlank(newOwner, "newOwner");

        try {
            Deployment result = deploymentService.assignOwner(id, newOwner);
            log.info("AUDIT assignOwner | user={} id={} newOwner={} outcome=SUCCESS latencyMs={}",
                    actingUser, id, newOwner, elapsedMs(start));
            return result.toString();
        } catch (Exception ex) {
            log.error("AUDIT assignOwner | user={} id={} outcome=ERROR latencyMs={} error={}",
                    actingUser, id, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    @Tool(
            name = "rescheduleDeployment",
            description = "Reschedule a deployment to a new ISO datetime (yyyy-MM-ddTHH:mm:ss)"
    )
    public String rescheduleDeployment(Long id, String newTime) {
        String actingUser = resolveUser();
        enforceWriteGate(actingUser);
        long start = System.nanoTime();

        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (id <= 0) {
            throw new IllegalArgumentException("id must be a positive number, got: " + id);
        }
        requireNonBlank(newTime, "newTime");

        LocalDateTime parsed = parseLocalDateTime(newTime);

        try {
            Deployment result = deploymentService.rescheduleDeployment(id, parsed);
            log.info("AUDIT rescheduleDeployment | user={} id={} newTime={} outcome=SUCCESS latencyMs={}",
                    actingUser, id, newTime, elapsedMs(start));
            return result.toString();
        } catch (Exception ex) {
            log.error("AUDIT rescheduleDeployment | user={} id={} outcome=ERROR latencyMs={} error={}",
                    actingUser, id, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    @Tool(
            name = "cancelDeployment",
            description = "Cancel a deployment by id"
    )
    public String cancelDeployment(Long id) {
        String actingUser = resolveUser();
        enforceWriteGate(actingUser);
        long start = System.nanoTime();

        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (id <= 0) {
            throw new IllegalArgumentException("id must be a positive number, got: " + id);
        }

        try {
            Deployment result = deploymentService.cancelDeployment(id);
            log.info("AUDIT cancelDeployment | user={} id={} outcome=SUCCESS latencyMs={}",
                    actingUser, id, elapsedMs(start));
            return result.toString();
        } catch (Exception ex) {
            log.error("AUDIT cancelDeployment | user={} id={} outcome=ERROR latencyMs={} error={}",
                    actingUser, id, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    // ─────────────────────────────── helpers ─────────────────────────────────

    private String resolveUser() {
        String user = ActingUserContext.get();
        return (user != null && !user.isBlank()) ? user : securityProperties.getDefaultUser();
    }

    private void enforceWriteGate(String actingUser) {
        if (securityProperties.isRequireUserForWrites()
                && securityProperties.getDefaultUser().equals(actingUser)) {
            throw new IllegalStateException(
                    "Write operations require an explicit X-Acting-User header. "
                            + "Default user '" + actingUser + "' is not permitted to perform mutations.");
        }
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be null or blank");
        }
    }

    private static DeploymentEnvironment parseEnvironment(String environment) {
        try {
            return DeploymentEnvironment.valueOf(environment.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid environment '" + environment + "'. Allowed values: DEV, QA, PROD");
        }
    }

    /**
     * Parses an ISO datetime string that may include an offset (e.g. {@code 2025-06-01T10:00:00+05:30})
     * or be a plain local datetime (e.g. {@code 2025-06-01T10:00:00}).
     */
    private static LocalDateTime parseScheduledTime(String value) {
        String trimmed = value.trim();
        // Try OffsetDateTime first (contains offset info)
        try {
            return OffsetDateTime.parse(trimmed).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        // Fall back to plain LocalDateTime
        try {
            return LocalDateTime.parse(trimmed);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "scheduledTime '" + value + "' is not a valid ISO datetime. "
                            + "Use format yyyy-MM-ddTHH:mm:ss (e.g. 2025-06-01T14:00:00)");
        }
    }

    private static LocalDateTime parseLocalDateTime(String value) {
        String trimmed = value.trim();
        try {
            return LocalDateTime.parse(trimmed);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "newTime '" + value + "' is not a valid ISO local datetime. "
                            + "Use format yyyy-MM-ddTHH:mm:ss (e.g. 2025-06-01T14:00:00)");
        }
    }

    private static long elapsedMs(long startNano) {
        return (System.nanoTime() - startNano) / 1_000_000L;
    }
}
