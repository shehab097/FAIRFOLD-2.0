package com.fairfold.assessment.dto;

import java.time.Instant;
import java.util.UUID;

public record LearningResourceDto(
        UUID id,
        UUID skillId,
        String skillName,
        String title,
        String url,
        String cost,
        String language,
        String level,
        boolean active,
        boolean broken,
        Instant lastCheckedAt
) {
}
