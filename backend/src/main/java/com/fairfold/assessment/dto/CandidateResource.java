package com.fairfold.assessment.dto;

import java.util.UUID;

public record CandidateResource(
        UUID id,
        String title,
        String url,
        String cost,
        String language,
        String level
) {
}
