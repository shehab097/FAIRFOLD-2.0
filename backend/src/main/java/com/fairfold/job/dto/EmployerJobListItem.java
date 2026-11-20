package com.fairfold.job.dto;

import com.fairfold.job.JobStatus;

import java.time.Instant;
import java.util.UUID;

public record EmployerJobListItem(
        UUID id,
        String title,
        JobStatus status,
        long applicationsCount,
        long awaitingScreeningCount,
        Instant createdAt
) {
}
