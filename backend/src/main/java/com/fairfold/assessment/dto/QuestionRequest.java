package com.fairfold.assessment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record QuestionRequest(
        @NotBlank @Size(max = 2000) String text,
        @NotNull @Size(min = 2, max = 6) List<@Valid QuestionOption> options,
        @NotBlank @Size(max = 20) String correctOptionId
) {
}
