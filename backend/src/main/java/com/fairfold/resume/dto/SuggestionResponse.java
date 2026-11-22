package com.fairfold.resume.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SuggestionResponse(
        String provider,
        List<Skill> skills,
        BigDecimal totalExperienceYears,
        String educationLevel,
        List<String> jobTitles,
        List<Work> workHistory
) {
    public record Skill(String name, UUID skillId, String level, BigDecimal years) {}

    public record Work(String title, String employer, String startMonth, String endMonth,
                       boolean isCurrent, List<String> skillsUsed) {}
}
