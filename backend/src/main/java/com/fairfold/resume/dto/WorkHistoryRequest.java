package com.fairfold.resume.dto;

import java.util.List;

public record WorkHistoryRequest(
        String title,
        String employer,
        String startMonth,
        String endMonth,
        boolean isCurrent,
        List<String> skillsUsed
) {
}
