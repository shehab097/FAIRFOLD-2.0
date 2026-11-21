package com.fairfold.resume.dto;

import java.time.Instant;
import java.util.UUID;

public record ResumeResponse(
        UUID id,
        String filename,
        long sizeBytes,
        String status,
        String failureReason,
        Instant uploadedAt
) {
}
