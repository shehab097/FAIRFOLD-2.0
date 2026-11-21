package com.fairfold.application.dto;

import java.time.Instant;
import java.util.UUID;

public record EmployerApplicantListItem(
        UUID id,
        String label,
        String status,
        Instant appliedAt
) {
}
