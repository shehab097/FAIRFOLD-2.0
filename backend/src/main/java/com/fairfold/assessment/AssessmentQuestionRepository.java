package com.fairfold.assessment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, UUID> {

    List<AssessmentQuestion> findByAssessmentIdOrderByPositionAsc(UUID assessmentId);

    long countByAssessmentId(UUID assessmentId);

    void deleteByAssessmentId(UUID assessmentId);
}
