package com.nexacorp.deployment.repository;

import com.nexacorp.deployment.model.Deployment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeploymentRepository extends JpaRepository<Deployment, Long> {
}
