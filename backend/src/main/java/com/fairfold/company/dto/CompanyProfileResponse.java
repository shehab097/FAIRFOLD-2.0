package com.fairfold.company.dto;

public record CompanyProfileResponse(
        String name,
        String industry,
        String size,
        boolean showNamePublicly
) {
}
