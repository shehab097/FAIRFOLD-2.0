package com.fairfold.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CertificationRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 255) String issuer,
        LocalDate issuedOn,
        LocalDate expiresOn,
        @Size(max = 255) String credentialId
) {
}
