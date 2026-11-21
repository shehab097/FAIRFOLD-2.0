package com.fairfold.company.dto;

import java.util.UUID;

/** Public company profile shown on the candidate-facing company page. */
public record PublicCompanyProfile(
        UUID id,
        String name,
        String industry,
        String size,
        String tagline,
        String about,
        String website,
        String logoUrl,
        String linkedinUrl,
        Integer foundedYear,
        String headquarters,
        String benefits
) {
}
