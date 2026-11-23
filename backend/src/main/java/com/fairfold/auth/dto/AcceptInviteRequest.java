package com.fairfold.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AcceptInviteRequest(
        @NotBlank String token,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Size(min = 10, message = "Password must be at least 10 characters") String password,
        @NotNull Boolean consent
) {
}
