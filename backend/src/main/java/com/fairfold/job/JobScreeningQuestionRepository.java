package com.fairfold.job;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface JobScreeningQuestionRepository extends JpaRepository<JobScreeningQuestion, UUID> {

    List<JobScreeningQuestion> findByJobIdOrderByPositionAsc(UUID jobId);

    @Transactional
    void deleteByJobId(UUID jobId);
}
