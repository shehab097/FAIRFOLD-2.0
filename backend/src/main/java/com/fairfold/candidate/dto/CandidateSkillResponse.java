package com.fairfold.candidate.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CandidateSkillResponse(
        UUID id,
        UUID skillId,
        String name,
        String level,
        BigDecimal years,
        boolean verified
) {
}
