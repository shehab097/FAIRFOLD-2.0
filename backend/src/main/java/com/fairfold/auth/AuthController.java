package com.fairfold.auth;

import com.fairfold.auth.dto.AuthResponse;
import com.fairfold.auth.dto.AcceptInviteRequest;
import com.fairfold.auth.dto.ForgotPasswordRequest;
import com.fairfold.auth.dto.InviteInfo;
import com.fairfold.auth.dto.LoginRequest;
import com.fairfold.auth.dto.RegisterRequest;
import com.fairfold.auth.dto.ResetPasswordRequest;
import com.fairfold.auth.dto.UserDto;
import com.fairfold.common.ratelimit.RateLimitService;
import com.fairfold.common.security.CurrentUser;
import com.fairfold.config.AppProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Registration, login, tokens and password recovery")
public class AuthController {

    private static final String REFRESH_COOKIE = "refresh_token";
    private static final Duration REFRESH_COOKIE_TTL = Duration.ofDays(7);

    private final AuthService authService;
    private final RateLimitService rateLimitService;
    private final AppProperties properties;

    public AuthController(AuthService authService, RateLimitService rateLimitService, AppProperties properties) {
        this.authService = authService;
        this.rateLimitService = rateLimitService;
        this.properties = properties;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new account")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        rateLimitService.check("register:" + ip(http), 10, Duration.ofMinutes(1), 60);
        AuthSession session = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(session.refreshToken()))
                .body(new AuthResponse(session.accessToken(), session.user()));
    }

    @PostMapping("/login")
    @Operation(summary = "Log in")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        rateLimitService.check("login:" + ip(http), 10, Duration.ofMinutes(1), 60);
        AuthSession session = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(session.refreshToken()))
                .body(new AuthResponse(session.accessToken(), session.user()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh cookie for a new access token")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        AuthSession session = authService.refresh(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(session.refreshToken()))
                .body(new AuthResponse(session.accessToken(), session.user()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the current refresh token")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie())
                .build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset link (always 202)")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest http) {
        rateLimitService.check("forgot:" + ip(http), 10, Duration.ofMinutes(1), 60);
        authService.forgotPassword(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset a password with a one-time token")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Current signed-in user")
    public UserDto me(@AuthenticationPrincipal CurrentUser currentUser) {
        return authService.me(currentUser.id());
    }

    @GetMapping("/invites/{token}")
    @Operation(summary = "Look up a team invite by token")
    public InviteInfo inviteInfo(@PathVariable String token) {
        return authService.inviteInfo(token);
    }

    @PostMapping("/accept-invite")
    @Operation(summary = "Accept a team invite and sign in")
    public ResponseEntity<AuthResponse> acceptInvite(@Valid @RequestBody AcceptInviteRequest request,
                                                     HttpServletRequest http) {
        rateLimitService.check("accept-invite:" + ip(http), 10, Duration.ofMinutes(1), 60);
        AuthSession session = authService.acceptInvite(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(session.refreshToken()))
                .body(new AuthResponse(session.accessToken(), session.user()));
    }

    // --- helpers ----------------------------------------------------------

    private String buildRefreshCookie(String token) {
        return ResponseCookie.from(REFRESH_COOKIE, token)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(REFRESH_COOKIE_TTL)
                .build()
                .toString();
    }

    private String clearRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(0)
                .build()
                .toString();
    }

    private String ip(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
