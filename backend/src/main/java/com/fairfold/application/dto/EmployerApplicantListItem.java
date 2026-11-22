package com.fairfold.application.dto;

import java.time.Instant;
import java.util.UUID;

public record EmployerApplicantListItem(
        UUID id,
        String alias,
        String label,
        Integer score,
        String status,
        String biasStatus,
        String gateStatus,
        String scoringMode,
        String notMatchedRule,
        Instant appliedAt
) {
}
