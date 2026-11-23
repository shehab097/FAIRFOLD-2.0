package com.fairfold.candidate.dto;

import java.util.List;
import java.util.UUID;

public record JourneyResponse(
        List<TimelineItem> timeline,
        List<SkillEvolution> skills
) {
    public record TimelineItem(
            UUID id,
            String title,
            String employer,
            String startMonth,
            String endMonth,
            boolean isCurrent
    ) {
    }

    public record SkillEvolution(
            UUID skillId,
            String name,
            String level,
            boolean verified,
            String firstUsedMonth,
            String lastUsedMonth,
            List<String> roleTitles
    ) {
    }
}
