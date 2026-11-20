package com.fairfold.auth;

import com.fairfold.auth.dto.UserDto;

/**
 * Internal result of a successful login/register/refresh: the JSON response
 * plus the raw refresh token so the controller can set the HttpOnly cookie.
 */
public record AuthSession(String accessToken, String refreshToken, UserDto user) {
}
