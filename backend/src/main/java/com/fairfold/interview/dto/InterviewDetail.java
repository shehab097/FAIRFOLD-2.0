package com.fairfold.interview.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record InterviewDetail(
        UUID id,
        Instant scheduledAt,
        int durationMinutes,
        String meetingLink,
        CandidateRef candidate,
        UUID applicationId,
        UUID jobId,
        String jobTitle,
        List<InterviewerRef> interviewers,
        InterviewPackDto pack,
        FeedbackView myFeedback,
        List<FeedbackView> allFeedback
) {
    public record CandidateRef(String firstName, String lastName) {
    }

    public record InterviewerRef(UUID id, String name, String role) {
    }

    public record FeedbackView(
            UUID interviewerId,
            String interviewerName,
            Map<String, Integer> scores,
            String comments,
            String recommendation
    ) {
    }
}
