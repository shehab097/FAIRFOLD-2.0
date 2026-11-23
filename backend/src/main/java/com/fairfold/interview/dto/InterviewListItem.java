package com.fairfold.interview.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InterviewListItem(
        UUID id,
        Instant scheduledAt,
        String candidateName,
        String jobTitle,
        List<InterviewerRef> interviewers,
        boolean myFeedbackSubmitted
) {
    public record InterviewerRef(UUID id, String name) {
    }
}
