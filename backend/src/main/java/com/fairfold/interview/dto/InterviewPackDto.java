package com.fairfold.interview.dto;

import java.util.List;

public record InterviewPackDto(
        List<Question> questions,
        List<RubricItem> rubric,
        List<String> screeningQuestions,
        String provider
) {
    public record Question(String id, String text) {
    }

    public record RubricItem(String id, String criterion, String description) {
    }

    public record SaveRequest(List<Question> questions, List<RubricItem> rubric) {
    }
}
