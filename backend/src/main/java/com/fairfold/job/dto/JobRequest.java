package com.fairfold.job.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record JobRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String description,
        String requirements,
        @NotBlank String experienceLevel,
        String city,
        String address,
        @NotBlank String country,
        boolean remote,
        List<UUID> requiredSkillIds,
        List<UUID> niceToHaveSkillIds,
        List<UUID> mustHaveSkillIds,
        Integer hfMinYears,
        String hfCountry,
        @Min(0) @Max(100) Integer cutoffScore,
        String salaryType,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String salaryCurrency,
        String salaryPeriod,
        Boolean salaryNegotiable,
        List<@Valid RequiredAssessmentRequest> requiredAssessments
) {
}
