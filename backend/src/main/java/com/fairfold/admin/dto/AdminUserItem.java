package com.fairfold.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminUserItem(
        UUID id,
        String name,
        String email,
        String role,
        UUID companyId,
        boolean locked,
        boolean disabled,
        boolean mfaEnabled,
        Instant createdAt
) {
}
