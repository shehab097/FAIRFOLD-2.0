package com.fairfold.assessment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AssessmentAttemptRepository extends JpaRepository<AssessmentAttempt, UUID> {

    long countByAssessmentId(UUID assessmentId);
}
