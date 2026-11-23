package com.fairfold.coaching.dto;

import java.util.UUID;

public record PracticeQuestionDto(
        UUID id,
        String text,
        String topic,
        boolean active
) {
}
