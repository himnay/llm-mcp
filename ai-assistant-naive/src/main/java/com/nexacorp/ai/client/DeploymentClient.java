package com.nexacorp.ai.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeploymentClient {

    private final RestTemplate restTemplate;
    @Value("${services.deployment.base-url}")
    private String deploymentBaseUrl;

    public void reschedule(String deploymentId, String date) {
        String url = deploymentBaseUrl +
                "/deployments/" + deploymentId +
                "/reschedule?newTime=" + date + "T10:00:00";
        restTemplate.put(url, null);
    }

    public void reassignDeployments(String currentOwner, String newOwner) {
        log.info("Re-assigning deployment to: " + newOwner);

        String url = deploymentBaseUrl + "/deployments";
        DeploymentDto[] deployments = restTemplate.getForObject(url, DeploymentDto[].class);
        for (DeploymentDto deployment : deployments) {
            if (currentOwner.equals(deployment.getOwner())) {
                String assignUrl = deploymentBaseUrl +
                        "/deployments/" + deployment.getId() +
                        "/assign?assignee=" + newOwner;
                restTemplate.put(assignUrl, null);
            }
        }
    }
}
