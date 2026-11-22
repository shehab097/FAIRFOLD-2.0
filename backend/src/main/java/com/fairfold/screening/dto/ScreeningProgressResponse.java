package com.fairfold.screening.dto;

import java.time.Instant;
import java.util.UUID;

public record ScreeningProgressResponse(
        UUID runId,
        String status,
        String mode,
        int total,
        int done,
        int failed,
        Instant startedAt
) {
}
