package com.fairfold.assessment.dto;

import jakarta.validation.constraints.NotNull;

public record ConsentRequest(@NotNull Boolean given) {
}
