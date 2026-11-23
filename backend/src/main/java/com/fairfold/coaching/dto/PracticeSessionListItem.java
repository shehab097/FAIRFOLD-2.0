package com.fairfold.coaching.dto;

import java.time.Instant;
import java.util.UUID;

public record PracticeSessionListItem(
        UUID id,
        UUID questionId,
        String questionText,
        String topic,
        Integer rating,
        boolean feedbackAvailable,
        Instant createdAt
) {
}
