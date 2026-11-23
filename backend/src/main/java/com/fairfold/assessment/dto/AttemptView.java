package com.fairfold.assessment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One view for start, resume and result. Fields that do not apply are omitted:
 * an in-progress attempt has questions/answers/endsAt, a submitted attempt has
 * score/passed/resources. The correct option ids are never included.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AttemptView(
        UUID attemptId,
        UUID assessmentId,
        String assessmentTitle,
        String skillName,
        Instant endsAt,
        Instant serverTime,
        List<AttemptQuestion> questions,
        Map<String, String> answers,
        Integer score,
        Boolean passed,
        Integer passMark,
        Integer correctCount,
        Integer totalQuestions,
        String status,
        Boolean skillVerified,
        @JsonInclude(JsonInclude.Include.ALWAYS) Boolean proctoringConsent,
        String explanation,
        Instant retryAvailableAt,
        List<CandidateResource> resources
) {
}
