package com.fairfold.assessment.dto;

import java.time.Instant;
import java.util.UUID;

/** Active assessment card for a candidate (P17). */
public record CandidateAssessment(
        UUID id,
        UUID skillId,
        String title,
        String skillName,
        int questionCount,
        int durationMinutes,
        int passMark,
        Integer bestScore,
        boolean passed,
        Instant lastAttemptAt,
        Instant retryAvailableAt,
        UUID inProgressAttemptId
) {
}
