package com.org.deployment.mcp;

import com.org.deployment.exception.InvalidToolArgumentException;
import com.org.deployment.model.Deployment;
import com.org.deployment.model.DeploymentEnvironment;
import com.org.deployment.security.ActingUserContext;
import com.org.deployment.security.SecurityProperties;
import com.org.deployment.service.DeploymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.ai.mcp.annotation.context.McpSyncRequestContext;
import org.springframework.ai.mcp.annotation.context.StructuredElicitResult;
import org.springframework.stereotype.Component;

import io.modelcontextprotocol.spec.McpSchema.ElicitResult;

/**
 * Demonstrates MCP <b>progress notifications</b> and <b>elicitation</b> on a long-running tool.
 *
 * <p><b>Progress</b>: {@code executeDeployment} walks through validate → deploy → verify stages and
 * emits {@code notifications/progress} after each one via {@link McpSyncRequestContext#progress},
 * so the connected client can show a live progress bar instead of a silent multi-second call.
 * Progress is best-effort — a client that sent no {@code progressToken} simply gets none.
 *
 * <p><b>Elicitation</b>: before touching a PROD deployment the tool asks the connected client for
 * explicit confirmation via {@link McpSyncRequestContext#elicit}. A decline (or a client without
 * elicitation support, when {@code mcp.interactive.require-prod-confirmation} is true) aborts.
 *
 * <p>Split from {@link DeploymentMcpTools} for the same reason as the GitHub sampling tool:
 * {@link McpSyncRequestContext} injection requires a stateful (STREAMABLE) server session.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class DeploymentInteractiveTools {

    /** Structured answer requested from the client when confirming a PROD deployment. */
    record ProdConfirmation(boolean confirm, String reason) {}

    private final DeploymentService deploymentService;
    private final SecurityProperties securityProperties;

    @McpTool(name = "executeDeployment",
            description = "Execute (simulate) a scheduled deployment now, streaming MCP progress notifications "
                    + "through the validate, deploy and verify stages. PROD deployments additionally require "
                    + "interactive confirmation from the connected client via MCP elicitation. "
                    + "Provide the numeric deployment id (use getDeployments to find it).")
    String executeDeployment(
            @McpToolParam(description = "Numeric id of the deployment to execute", required = true) Long id,
            McpSyncRequestContext ctx) {

        String actingUser = resolveUser();
        if (id == null || id <= 0) {
            throw new InvalidToolArgumentException("id must be a positive number, got: " + id);
        }
        Deployment deployment = deploymentService.getDeployment(id);
        long start = System.nanoTime();

        if (deployment.getEnvironment() == DeploymentEnvironment.PROD) {
            String verdict = confirmProdExecution(ctx, deployment, actingUser);
            if (verdict != null) {
                return verdict;
            }
        }

        notifyProgress(ctx, 10, "Validating deployment " + id + " (" + deployment.getServiceName() + ")");
        simulateStage();
        notifyProgress(ctx, 40, "Deploying " + deployment.getServiceName()
                + " to " + deployment.getEnvironment());
        simulateStage();
        notifyProgress(ctx, 80, "Verifying " + deployment.getServiceName() + " health checks");
        simulateStage();
        notifyProgress(ctx, 100, "Deployment " + id + " finished");

        long latencyMs = (System.nanoTime() - start) / 1_000_000L;
        log.info("AUDIT executeDeployment | user={} id={} env={} outcome=SUCCESS latencyMs={}",
                actingUser, id, deployment.getEnvironment(), latencyMs);
        return "Deployment %d (%s → %s) executed successfully (simulated). Stages: validate, deploy, verify."
                .formatted(id, deployment.getServiceName(), deployment.getEnvironment());
    }

    /** Returns an abort message when PROD execution is not confirmed, or {@code null} to proceed. */
    private String confirmProdExecution(McpSyncRequestContext ctx, Deployment deployment, String actingUser) {
        if (!ctx.elicitEnabled()) {
            log.warn("AUDIT executeDeployment | user={} id={} env=PROD outcome=BLOCKED reason=no-elicitation-support",
                    actingUser, deployment.getId());
            return "Aborted: PROD deployments require interactive confirmation, but the connected "
                    + "MCP client does not support elicitation.";
        }
        StructuredElicitResult<ProdConfirmation> answer = ctx.elicit(
                spec -> spec.message("Confirm PROD deployment of '" + deployment.getServiceName()
                        + "' (id " + deployment.getId() + ") requested by " + actingUser
                        + ". Set confirm=true to proceed; optionally give a reason."),
                ProdConfirmation.class);

        boolean confirmed = answer.action() == ElicitResult.Action.ACCEPT
                && answer.structuredContent() != null
                && answer.structuredContent().confirm();
        if (!confirmed) {
            log.warn("AUDIT executeDeployment | user={} id={} env=PROD outcome=DECLINED action={}",
                    actingUser, deployment.getId(), answer.action());
            return "Aborted: PROD deployment " + deployment.getId() + " was not confirmed (client answered "
                    + answer.action() + ").";
        }
        log.info("AUDIT executeDeployment | user={} id={} env=PROD confirmation=ACCEPTED reason={}",
                actingUser, deployment.getId(), answer.structuredContent().reason());
        return null;
    }

    /** Best-effort progress: never let a notification failure break the tool call itself. */
    private void notifyProgress(McpSyncRequestContext ctx, int percentage, String message) {
        try {
            ctx.progress(p -> p.percentage(percentage).message(message));
        } catch (Exception e) {
            log.debug("executeDeployment | progress notification skipped | {}", e.getMessage());
        }
    }

    private void simulateStage() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String resolveUser() {
        String user = ActingUserContext.get();
        return (user != null && !user.isBlank()) ? user : securityProperties.getDefaultUser();
    }
}
