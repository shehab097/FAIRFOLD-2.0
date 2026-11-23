package com.fairfold.team;

import com.fairfold.user.Role;

public enum TeamRole {
    employer_hr,
    employer_manager,
    interviewer;

    public static TeamRole fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("role is required");
        }
        return TeamRole.valueOf(value.trim().toLowerCase());
    }

    public Role toUserRole() {
        return Role.valueOf(name().toUpperCase());
    }
}
