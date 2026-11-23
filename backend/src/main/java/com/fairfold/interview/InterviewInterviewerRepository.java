package com.fairfold.interview;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InterviewInterviewerRepository extends JpaRepository<InterviewInterviewer, InterviewInterviewerId> {

    List<InterviewInterviewer> findByIdInterviewId(UUID interviewId);

    boolean existsByIdInterviewIdAndIdUserId(UUID interviewId, UUID userId);

    void deleteByIdInterviewId(UUID interviewId);

    List<InterviewInterviewer> findByIdUserId(UUID userId);
}
