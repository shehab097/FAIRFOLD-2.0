package com.fairfold.assessment.dto;

import jakarta.validation.constraints.Size;

public record ExplanationRequest(@Size(max = 500) String text) {
}
