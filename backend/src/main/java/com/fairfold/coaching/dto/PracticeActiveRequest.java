package com.fairfold.coaching.dto;

import jakarta.validation.constraints.NotNull;

public record PracticeActiveRequest(@NotNull Boolean active) {
}
