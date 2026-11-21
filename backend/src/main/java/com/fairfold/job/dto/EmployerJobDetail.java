package com.fairfold.job.dto;

import com.fairfold.job.ExperienceLevel;
import com.fairfold.job.JobStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EmployerJobDetail(
        UUID id,
        String title,
        String description,
        String requirements,
        ExperienceLevel experienceLevel,
        String city,
        String address,
        String country,
        boolean remote,
        List<SkillRef> requiredSkills,
        List<SkillRef> niceToHaveSkills,
        Integer hfMinYears,
        String hfCountry,
        Integer cutoffScore,
        JobStatus status,
        Instant publishedAt
) {
    public record SkillRef(UUID id, String name, boolean mustHave) {}
}
