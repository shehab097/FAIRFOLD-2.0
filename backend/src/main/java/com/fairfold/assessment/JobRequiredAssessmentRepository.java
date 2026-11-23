package com.fairfold.assessment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface JobRequiredAssessmentRepository extends JpaRepository<JobRequiredAssessment, UUID> {

    List<JobRequiredAssessment> findByJobId(UUID jobId);

    List<JobRequiredAssessment> findByJobIdIn(Collection<UUID> jobIds);

    boolean existsByJobIdAndAssessmentId(UUID jobId, UUID assessmentId);

    boolean existsByAssessmentId(UUID assessmentId);

    void deleteByJobId(UUID jobId);
}
