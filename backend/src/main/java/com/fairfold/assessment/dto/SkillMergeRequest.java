package com.fairfold.assessment.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SkillMergeRequest(@NotNull UUID targetSkillId) {
}
