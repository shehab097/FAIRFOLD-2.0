package com.fairfold.job.dto;

import com.fairfold.job.ExperienceLevel;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PublicJobListItem(
        UUID id,
        String title,
        String city,
        String country,
        boolean remote,
        ExperienceLevel experienceLevel,
        List<String> topSkills,
        Instant publishedAt,
        String companyName,
        String companyLogoUrl
) {
}
