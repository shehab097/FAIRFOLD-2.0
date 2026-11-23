package com.fairfold.coaching.dto;

import com.fairfold.ai.StarFeedback;

import java.time.Instant;
import java.util.UUID;

public record PracticeSessionResponse(
        UUID id,
        UUID questionId,
        String questionText,
        String topic,
        String answer,
        boolean feedbackAvailable,
        StarFeedback feedback,
        String rubricVersion,
        String provider,
        Integer rating,
        Instant createdAt
) {
}
