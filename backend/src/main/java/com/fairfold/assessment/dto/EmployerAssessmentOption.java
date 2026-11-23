package com.fairfold.assessment.dto;

import java.util.UUID;

/** Active assessment an employer can require on a draft job (P23). */
public record EmployerAssessmentOption(
        UUID id,
        String title,
        String skillName,
        int passMark,
        int durationMinutes
) {
}
