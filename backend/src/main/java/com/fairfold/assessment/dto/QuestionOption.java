package com.fairfold.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** One multiple-choice option. Ids are short strings: a, b, c, ... */
public record QuestionOption(
        @NotBlank @Size(max = 20) String id,
        @NotBlank @Size(max = 1000) String text
) {
}
