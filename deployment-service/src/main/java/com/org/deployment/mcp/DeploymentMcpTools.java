package com.org.deployment.mcp;

import com.org.deployment.model.Deployment;
import com.org.deployment.model.DeploymentEnvironment;
import com.org.deployment.service.DeploymentService;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
class DeploymentMcpTools {

    private final DeploymentService deploymentService;

    @McpTool(
            name = "getDeployments",
            description = "Get all deployments"
    )
    public List<Deployment> getDeployments() {
        return deploymentService.getDeployments();
    }

    @McpTool(
            name = "getDeployment",
            description = "Get a deployment by its id"
    )
    public Deployment getDeployment(Long id) {
        return deploymentService.getDeployment(id);
    }

    @McpTool(
            name = "createDeployment",
            description = "Create a new deployment. Provide serviceName, environment (DEV, QA, PROD), scheduledTime (ISO format yyyy-MM-ddTHH:mm:ss) and owner"
    )
    public Deployment createDeployment(String serviceName,
                                       String environment,
                                       String scheduledTime,
                                       String owner) {
        LocalDateTime scheduled = OffsetDateTime.parse(scheduledTime).toLocalDateTime();
        return deploymentService.createDeployment(
                serviceName,
                DeploymentEnvironment.valueOf(environment),
                scheduled,
                owner
        );
    }

    @McpTool(
            name = "assignOwner",
            description = "Assign a new owner to an existing deployment"
    )
    public Deployment assignOwner(Long id, String newOwner) {
        return deploymentService.assignOwner(id, newOwner);
    }

    @McpTool(
            name = "rescheduleDeployment",
            description = "Reschedule a deployment to a new ISO datetime (yyyy-MM-ddTHH:mm:ss)"
    )
    public Deployment rescheduleDeployment(Long id, String newTime) {
        return deploymentService.rescheduleDeployment(
                id,
                LocalDateTime.parse(newTime)
        );
    }


    @McpTool(
            name = "cancelDeployment",
            description = "Cancel a deployment by id"
    )
    public Deployment cancelDeployment(Long id) {
        return deploymentService.cancelDeployment(id);
    }

}
