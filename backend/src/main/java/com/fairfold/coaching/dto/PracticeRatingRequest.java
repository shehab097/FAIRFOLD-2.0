package com.fairfold.coaching.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PracticeRatingRequest(@NotNull @Min(1) @Max(5) Integer rating) {
}
