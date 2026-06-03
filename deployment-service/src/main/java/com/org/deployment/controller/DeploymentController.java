package com.org.deployment.controller;

import com.org.deployment.model.Deployment;
import com.org.deployment.model.DeploymentEnvironment;
import com.org.deployment.service.DeploymentService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/deployments")
@RequiredArgsConstructor
@Validated
class DeploymentController {
    private final DeploymentService deploymentService;

    @GetMapping
    public List<Deployment> getAllDeployments() {
        return deploymentService.getDeployments();
    }

    @GetMapping("/{id}")
    public Deployment getDeployment(@PathVariable @Positive Long id) {
        return deploymentService.getDeployment(id);
    }

    @PostMapping
    public Deployment createDeployment(
            @RequestParam @NotBlank String serviceName,
            @RequestParam @NotNull DeploymentEnvironment environment,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @NotNull LocalDateTime scheduledTime,
            @RequestParam @NotBlank String owner) {

        return deploymentService.createDeployment(serviceName, environment, scheduledTime, owner);
    }


    @PutMapping("/{id}/assign")
    public Deployment assignDeployment(
            @PathVariable @Positive Long id,
            @RequestParam @NotBlank String assignee) {

        return deploymentService.assignOwner(id, assignee);
    }

    @PutMapping("/{id}/reschedule")
    public Deployment rescheduleDeployment(
            @PathVariable @Positive Long id,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @NotNull LocalDateTime newTime) {

        return deploymentService.rescheduleDeployment(id, newTime);
    }

    @PutMapping("/{id}/cancel")
    public Deployment cancelDeployment(@PathVariable @Positive Long id) {
        return deploymentService.cancelDeployment(id);
    }

}
