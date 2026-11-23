package com.fairfold.admin.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record AuditLogItem(
        UUID id,
        Instant createdAt,
        UUID actorId,
        String actorEmail,
        String action,
        String resourceType,
        String resourceId,
        JsonNode details
) {
}
