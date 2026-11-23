package com.fairfold.message.dto;

import java.time.Instant;
import java.util.UUID;

public record MessageDto(
        UUID id,
        boolean mine,
        String senderName,
        String body,
        Instant createdAt
) {
}
