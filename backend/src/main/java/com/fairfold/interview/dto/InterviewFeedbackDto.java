package com.fairfold.interview.dto;

import java.util.Map;
import java.util.UUID;

public record InterviewFeedbackDto(
        UUID interviewerId,
        Map<String, Integer> scores,
        String comments,
        String recommendation
) {
}
