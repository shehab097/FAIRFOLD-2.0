package com.fairfold.assessment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record AssessmentRequest(
        @NotBlank @Size(max = 255) String title,
        @NotNull UUID skillId,
        @NotNull @Min(5) @Max(180) Integer durationMinutes,
        @NotNull @Min(1) @Max(100) Integer passMark,
        Boolean active,
        @NotEmpty List<@Valid QuestionRequest> questions
) {
}
