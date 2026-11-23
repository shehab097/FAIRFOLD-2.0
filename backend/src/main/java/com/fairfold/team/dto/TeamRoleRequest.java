package com.fairfold.team.dto;

import jakarta.validation.constraints.NotBlank;

public record TeamRoleRequest(@NotBlank String role) {
}
