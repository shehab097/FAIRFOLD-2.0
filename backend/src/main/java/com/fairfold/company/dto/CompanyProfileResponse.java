package com.fairfold.company.dto;

public record CompanyProfileResponse(
        String name,
        String industry,
        String size,
        boolean showNamePublicly,
        String tagline,
        String about,
        String website,
        String logoUrl,
        String linkedinUrl,
        Integer foundedYear,
        String headquarters,
        String contactEmail,
        String phone,
        String benefits
) {
}
