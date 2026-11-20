package com.fairfold.auth.dto;

public record AuthResponse(String accessToken, UserDto user) {
}
