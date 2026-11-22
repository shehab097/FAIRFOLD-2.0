package com.fairfold.resume.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ResumeResponse(
        UUID id,
        String filename,
        long sizeBytes,
        String status,
        String failureReason,
        String failureMessage,
        String strippedText,
        List<DroppedPlaceholder> droppedPlaceholders,
        Instant uploadedAt
) {
    public record DroppedPlaceholder(String placeholder, String kind, String originalValue) {}
}
