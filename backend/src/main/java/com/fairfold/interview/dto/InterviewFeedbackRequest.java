package com.fairfold.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record InterviewFeedbackRequest(
        Map<String, Integer> scores,
        @Size(max = 2000) String comments,
        @NotBlank String recommendation
) {
}
