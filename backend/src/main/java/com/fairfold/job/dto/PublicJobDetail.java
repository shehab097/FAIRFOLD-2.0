package com.fairfold.job.dto;

import com.fairfold.job.ExperienceLevel;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PublicJobDetail(
        UUID id,
        String title,
        String description,
        String requirements,
        String city,
        String address,
        String country,
        boolean remote,
        ExperienceLevel experienceLevel,
        List<String> requiredSkills,
        List<String> niceToHaveSkills,
        Instant publishedAt,
        String companyName,
        String companyLogoUrl
) {
}
