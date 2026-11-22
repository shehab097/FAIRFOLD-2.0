package com.fairfold.resume.dto;

import java.util.List;
import java.util.UUID;

public record WorkHistoryResponse(
        UUID id,
        String title,
        String employer,
        String startMonth,
        String endMonth,
        boolean isCurrent,
        List<String> skillsUsed
) {
}
