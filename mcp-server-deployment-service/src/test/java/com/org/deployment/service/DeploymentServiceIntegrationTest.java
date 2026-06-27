package com.org.deployment.service;

import com.org.deployment.TestcontainersConfiguration;
import com.org.deployment.exception.ResourceNotFoundException;
import com.org.deployment.model.Deployment;
import com.org.deployment.model.DeploymentEnvironment;
import com.org.deployment.model.DeploymentStatus;
import com.org.deployment.repository.DeploymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers(disabledWithoutDocker = true)
class DeploymentServiceIntegrationTest {

    private static final LocalDateTime NEXT_WEEK = LocalDateTime.now().plusDays(7);
    @Autowired
    private DeploymentService deploymentService;
    @Autowired
    private DeploymentRepository deploymentRepository;

    @BeforeEach
    void cleanup() {
        deploymentRepository.deleteAll();
    }

    @Test
    @DisplayName("Persists a new deployment with SCHEDULED status")
    void createDeployment_persistsWithScheduledStatus() {
        Deployment deployment = deploymentService.createDeployment(
                "payment-service", DeploymentEnvironment.STAGING, NEXT_WEEK, "alice");

        assertThat(deployment.getId()).isNotNull();
        assertThat(deployment.getServiceName()).isEqualTo("payment-service");
        assertThat(deployment.getEnvironment()).isEqualTo(DeploymentEnvironment.STAGING);
        assertThat(deployment.getStatus()).isEqualTo(DeploymentStatus.SCHEDULED);
        assertThat(deployment.getOwner()).isEqualTo("alice");
    }

    @Test
    @DisplayName("Returns the previously persisted deployment by id")
    void getDeployment_returnsPersistedDeployment() {
        Deployment created = deploymentService.createDeployment(
                "auth-service", DeploymentEnvironment.PROD, NEXT_WEEK, "bob");

        Deployment fetched = deploymentService.getDeployment(created.getId());

        assertThat(fetched.getServiceName()).isEqualTo("auth-service");
        assertThat(fetched.getEnvironment()).isEqualTo(DeploymentEnvironment.PROD);
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when the deployment id does not exist")
    void getDeployment_throwsWhenNotFound() {
        assertThatThrownBy(() -> deploymentService.getDeployment(999_999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999999");
    }

    @Test
    @DisplayName("Returns all persisted deployments")
    void getDeployments_returnsAll() {
        deploymentService.createDeployment("svc-a", DeploymentEnvironment.DEV, NEXT_WEEK, "alice");
        deploymentService.createDeployment("svc-b", DeploymentEnvironment.STAGING, NEXT_WEEK, "bob");

        List<Deployment> deployments = deploymentService.getDeployments();
        assertThat(deployments).hasSize(2);
    }

    @Test
    @DisplayName("Sets deployment status to CANCELLED when cancelled")
    void cancelDeployment_setsStatusToCancelled() {
        Deployment created = deploymentService.createDeployment(
                "reporting-svc", DeploymentEnvironment.STAGING, NEXT_WEEK, "carol");

        Deployment cancelled = deploymentService.cancelDeployment(created.getId());

        assertThat(cancelled.getStatus()).isEqualTo(DeploymentStatus.CANCELLED);
    }

    @Test
    @DisplayName("Updates the scheduled time when a deployment is rescheduled")
    void rescheduleDeployment_updatesScheduledTime() {
        Deployment created = deploymentService.createDeployment(
                "notify-svc", DeploymentEnvironment.PROD, NEXT_WEEK, "dave");

        LocalDateTime newTime = NEXT_WEEK.plusDays(3);
        Deployment rescheduled = deploymentService.rescheduleDeployment(created.getId(), newTime);

        assertThat(rescheduled.getScheduledTime()).isEqualToIgnoringNanos(newTime);
    }

    @Test
    @DisplayName("Updates the owner when assignOwner is called")
    void assignOwner_updatesOwner() {
        Deployment created = deploymentService.createDeployment(
                "search-svc", DeploymentEnvironment.DEV, NEXT_WEEK, "eve");

        Deployment reassigned = deploymentService.assignOwner(created.getId(), "frank");

        assertThat(reassigned.getOwner()).isEqualTo("frank");
    }
}
