package com.fairfold.assessment.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record SaveAnswersRequest(@NotNull Map<String, String> answers) {
}
