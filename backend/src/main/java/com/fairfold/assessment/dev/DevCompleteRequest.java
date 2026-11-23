package com.fairfold.assessment.dev;

import jakarta.validation.constraints.Min;

public record DevCompleteRequest(@Min(0) Integer correctCount) {
}
