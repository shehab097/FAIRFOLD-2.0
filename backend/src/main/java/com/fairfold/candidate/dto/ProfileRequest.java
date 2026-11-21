package com.fairfold.candidate.dto;

import java.math.BigDecimal;

public record ProfileRequest(
        String phone,
        String title,
        String bio,
        String location,
        String remotePreference,
        BigDecimal totalExperienceYears,
        String educationLevel
) {
}
