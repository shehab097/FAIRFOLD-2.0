package com.fairfold.assessment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ProctorEventRequest(
        @NotBlank String type,
        Instant occurredAt,
        @NotNull @Min(0) @Max(86_400) Integer durationSeconds
) {
    public ProctorEventRequest {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
        if (durationSeconds == null) {
            durationSeconds = 0;
        }
    }
}
