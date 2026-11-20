package com.fairfold.company.dto;

import jakarta.validation.constraints.NotBlank;

public record CompanyProfileRequest(
        @NotBlank String name,
        @NotBlank String industry,
        @NotBlank String size,
        boolean showNamePublicly
) {
}
