package com.nexacorp.deployment.service;

import com.nexacorp.deployment.model.Deployment;
import com.nexacorp.deployment.model.DeploymentEnvironment;
import com.nexacorp.deployment.model.DeploymentStatus;
import com.nexacorp.deployment.repository.DeploymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeploymentService {

    private final DeploymentRepository deploymentRepository;

    public List<Deployment> getDeployments() {
        return deploymentRepository.findAll();
    }

    public Deployment getDeployment(Long id) {
        return deploymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Deployment not found"));
    }

    public Deployment createDeployment(String serviceName,
                                       DeploymentEnvironment environment,
                                       LocalDateTime scheduledTime,
                                       String owner) {

        Deployment deployment = Deployment.builder()
                .serviceName(serviceName)
                .environment(environment)
                .scheduledTime(scheduledTime)
                .status(DeploymentStatus.SCHEDULED)
                .owner(owner)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return deploymentRepository.save(deployment);
    }

    public Deployment assignOwner(Long id, String newOwner) {
        Deployment deployment = getDeployment(id);

        deployment.setOwner(newOwner);
        deployment.setUpdatedAt(LocalDateTime.now());

        return deploymentRepository.save(deployment);
    }

    public Deployment rescheduleDeployment(Long id, LocalDateTime newTime) {
        Deployment deployment = getDeployment(id);
        deployment.setScheduledTime(newTime);
        deployment.setUpdatedAt(LocalDateTime.now());
        return deploymentRepository.save(deployment);
    }

    public Deployment cancelDeployment(Long id) {
        Deployment deployment = getDeployment(id);
        deployment.setStatus(DeploymentStatus.CANCELLED);
        deployment.setUpdatedAt(LocalDateTime.now());
        return deploymentRepository.save(deployment);
    }

}
