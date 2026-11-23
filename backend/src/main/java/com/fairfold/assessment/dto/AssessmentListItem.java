package com.fairfold.assessment.dto;

import java.util.UUID;

public record AssessmentListItem(
        UUID id,
        String title,
        String skillName,
        int questionCount,
        int durationMinutes,
        int passMark,
        boolean active,
        long attemptCount
) {
}
