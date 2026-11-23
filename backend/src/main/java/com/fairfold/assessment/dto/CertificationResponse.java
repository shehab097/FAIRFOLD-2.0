package com.fairfold.assessment.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CertificationResponse(
        UUID id,
        String name,
        String issuer,
        LocalDate issuedOn,
        LocalDate expiresOn,
        String credentialId
) {
}
