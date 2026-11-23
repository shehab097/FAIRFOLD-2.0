package com.fairfold.application.dto;

import java.util.UUID;

/** One assessment a candidate still needs for one of their applications. */
public record CandidateAssessmentRequirement(
        UUID applicationId,
        UUID jobId,
        String jobTitle,
        UUID assessmentId,
        String assessmentTitle,
        int minScore,
        Integer bestScore,
        String status
) {
}
