package com.fairfold.candidate.dto;

import java.util.UUID;

public record AddSkillRequest(UUID skillId, String level, java.math.BigDecimal years) {
}
