package com.fairfold.screening.dto;

import java.time.Instant;
import java.util.UUID;

/** A candidate the employer has shortlisted, across all of its jobs. */
public record ShortlistedCandidate(
        UUID applicationId,
        UUID jobId,
        String jobTitle,
        String candidateName,
        String candidateEmail,
        Integer score,
        String status,
        Instant shortlistedAt
) {
}
