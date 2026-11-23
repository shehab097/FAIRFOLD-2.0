package com.fairfold.assessment.dto;

import java.util.List;

/** A question as shown during an attempt: never includes the correct option. */
public record AttemptQuestion(
        String id,
        int position,
        String text,
        List<QuestionOption> options
) {
}
