package com.fairfold.company.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

/** {@code about} and {@code benefits} are Markdown. Optional fields may be null or blank. */
public record CompanyProfileRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 255) String industry,
        @NotBlank String size,
        boolean showNamePublicly,
        @Size(max = 160) String tagline,
        @Size(max = 20000) String about,
        @URL(regexp = "^(https?://.*)?$") @Size(max = 2048) String website,
        @URL(regexp = "^(https?://.*)?$") @Size(max = 2048) String logoUrl,
        @URL(regexp = "^(https?://.*)?$") @Size(max = 2048) String linkedinUrl,
        @Min(1800) @Max(2100) Integer foundedYear,
        @Size(max = 255) String headquarters,
        @Email @Size(max = 254) String contactEmail,
        @Size(max = 40) String phone,
        @Size(max = 20000) String benefits
) {
}
