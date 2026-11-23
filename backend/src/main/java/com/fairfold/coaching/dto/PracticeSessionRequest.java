package com.fairfold.coaching.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record PracticeSessionRequest(
        @NotNull UUID questionId,
        @NotBlank @Size(min = 20, max = 3000) String answer
) {
}
