package com.fairfold.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ResourceRequest(
        @NotNull UUID skillId,
        @NotBlank @Size(max = 500) String title,
        @NotBlank @Size(max = 2048) String url,
        @NotBlank String cost,
        @Size(max = 50) String language,
        @NotBlank String level,
        Boolean active
) {
}
