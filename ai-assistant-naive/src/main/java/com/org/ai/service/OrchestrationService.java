package com.org.ai.service;

import com.org.ai.client.DeploymentClient;
import com.org.ai.client.HRClient;
import com.org.ai.intent.ChatIntent;
import com.org.ai.intent.IntentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrchestrationService {

    private final HRClient hrClient;
    private final DeploymentClient deploymentClient;

    public String execute(ChatIntent intent) {
        if (intent.getIntent() == IntentType.REASSIGN_DEPLOYMENT_DUE_TO_LEAVE) {
            hrClient.applyLeave(intent.getUsername(), intent.getDate());

            String replacement = hrClient.findReplacement(intent.getUsername(), intent.getDate());

            deploymentClient.reassignDeployments(intent.getUsername(), replacement);
            return "Leave applied and deployments reassigned to " + replacement;
        }

        if (intent.getIntent() == IntentType.RESCHEDULE_DEPLOYMENT) {
            deploymentClient.reschedule(intent.getDeploymentId(), intent.getDate());
            return "Deployment rescheduled successfully.";
        }

        return "Intent not recognized.";
    }
}
