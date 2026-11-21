package com.fairfold.auth.dto;

import com.fairfold.user.User;

import java.util.UUID;

public record UserDto(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String role,
        UUID companyId,
        String avatarUrl
) {
    public static UserDto from(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.getCompanyId(),
                user.getAvatarUrl());
    }
}
