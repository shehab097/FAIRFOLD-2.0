package com.fairfold.assessment.dto;

import java.util.List;
import java.util.UUID;

public record AssessmentDetail(
        UUID id,
        String title,
        UUID skillId,
        String skillName,
        int durationMinutes,
        int passMark,
        boolean active,
        long attemptCount,
        String reviewStatus,
        int poolSize,
        int questionsPerAttempt,
        long candidateDemandCount,
        List<QuestionDto> questions
) {
    public record QuestionDto(
            UUID id,
            int position,
            String text,
            List<QuestionOption> options,
            String correctOptionId
    ) {
    }
}
