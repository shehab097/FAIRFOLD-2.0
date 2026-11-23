package com.fairfold.assessment.dto;

import java.time.Instant;
import java.util.Map;

public record SaveAnswersResponse(Instant savedAt, Map<String, String> answers) {
}
