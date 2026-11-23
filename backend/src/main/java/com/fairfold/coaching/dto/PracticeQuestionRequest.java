package com.fairfold.coaching.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PracticeQuestionRequest(
        @NotBlank @Size(max = 1000) String text,
        @NotBlank @Size(max = 100) String topic,
        Boolean active
) {
}
