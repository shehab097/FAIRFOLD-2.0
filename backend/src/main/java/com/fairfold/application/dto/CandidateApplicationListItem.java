package com.fairfold.application.dto;

import java.time.Instant;
import java.util.UUID;

public record CandidateApplicationListItem(
        UUID id,
        UUID jobId,
        String jobTitle,
        String companyName,
        UUID companyId,
        String status,
        Instant appliedAt
) {
}
