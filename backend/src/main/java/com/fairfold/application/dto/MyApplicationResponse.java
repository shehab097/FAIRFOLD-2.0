package com.fairfold.application.dto;

import java.time.Instant;
import java.util.UUID;

/** The caller's application for one job, if any (used to disable Apply). */
public record MyApplicationResponse(
        boolean applied,
        UUID applicationId,
        String status,
        Instant appliedAt
) {
}
