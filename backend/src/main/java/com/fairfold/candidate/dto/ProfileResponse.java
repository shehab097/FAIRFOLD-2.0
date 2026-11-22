package com.fairfold.candidate.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProfileResponse(
        String phone,
        String title,
        String bio,
        String location,
        String remotePreference,
        BigDecimal totalExperienceYears,
        String educationLevel,
        List<String> previousJobTitles
) {
}
