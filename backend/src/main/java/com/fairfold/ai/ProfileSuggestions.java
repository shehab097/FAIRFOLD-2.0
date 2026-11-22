package com.fairfold.ai;

import java.math.BigDecimal;
import java.util.List;

/**
 * Structured profile data extracted from a stripped resume. Employer names are
 * still placeholders; the resume placeholder map restores them for the
 * candidate.
 */
public record ProfileSuggestions(
        List<SuggestedSkill> skills,
        BigDecimal totalExperienceYears,
        String educationLevel,
        List<String> jobTitles,
        List<SuggestedWork> workHistory
) {
    public ProfileSuggestions {
        skills = skills == null ? List.of() : skills;
        jobTitles = jobTitles == null ? List.of() : jobTitles;
        workHistory = workHistory == null ? List.of() : workHistory;
    }

    public static ProfileSuggestions empty() {
        return new ProfileSuggestions(List.of(), null, null, List.of(), List.of());
    }

    public record SuggestedSkill(String name, String level, BigDecimal years) {}

    public record SuggestedWork(String title, String employerPlaceholder, List<String> skillsUsed) {
        public SuggestedWork {
            skillsUsed = skillsUsed == null ? List.of() : skillsUsed;
        }
    }
}
