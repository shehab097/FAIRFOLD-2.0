package com.fairfold.interview;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterviewFeedbackRepository extends JpaRepository<InterviewFeedback, UUID> {

    List<InterviewFeedback> findByInterviewIdOrderByIdAsc(UUID interviewId);

    Optional<InterviewFeedback> findByInterviewIdAndInterviewerId(UUID interviewId, UUID interviewerId);

    long countByInterviewId(UUID interviewId);
}
