package com.fairfold.assessment.dto;

import java.time.Instant;
import java.util.UUID;

public record AttemptHistoryItem(
        UUID id,
        String assessmentTitle,
        Integer score,
        Boolean passed,
        String status,
        Instant submittedAt
) {
}
