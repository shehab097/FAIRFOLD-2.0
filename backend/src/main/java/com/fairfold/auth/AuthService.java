package com.fairfold.auth;

import com.fairfold.auth.dto.AcceptInviteRequest;
import com.fairfold.auth.dto.AuthResponse;
import com.fairfold.auth.dto.ChangePasswordRequest;
import com.fairfold.auth.dto.ForgotPasswordRequest;
import com.fairfold.auth.dto.InviteInfo;
import com.fairfold.auth.dto.LoginRequest;
import com.fairfold.auth.dto.RegisterRequest;
import com.fairfold.auth.dto.ResetPasswordRequest;
import com.fairfold.auth.dto.UserDto;
import com.fairfold.common.audit.AuditService;
import com.fairfold.common.errors.ApiException;
import com.fairfold.common.mail.MailService;
import com.fairfold.common.security.JwtService;
import com.fairfold.company.Company;
import com.fairfold.company.CompanyRepository;
import com.fairfold.config.AppProperties;
import com.fairfold.team.TeamInvite;
import com.fairfold.team.TeamInviteRepository;
import com.fairfold.user.Role;
import com.fairfold.user.User;
import com.fairfold.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthService {

    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);
    private static final Duration RESET_TOKEN_TTL = Duration.ofHours(1);
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MailService mailService;
    private final AuditService auditService;
    private final AppProperties properties;
    private final TeamInviteRepository teamInviteRepository;
    private final Clock clock;

    public AuthService(
            UserRepository userRepository,
            CompanyRepository companyRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            MailService mailService,
            AuditService auditService,
            AppProperties properties,
            TeamInviteRepository teamInviteRepository,
            Clock clock) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mailService = mailService;
        this.auditService = auditService;
        this.properties = properties;
        this.teamInviteRepository = teamInviteRepository;
        this.clock = clock;
    }

    @Transactional
    public AuthSession register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw ApiException.conflict("EMAIL_TAKEN", "An account with this email already exists.");
        }
        if (request.consent() == null || !request.consent()) {
            throw ApiException.badRequest("CONSENT_REQUIRED", "You must agree to the terms to continue.");
        }
        Role role = resolveRole(request.role());

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setRole(role);
        user.setEmailVerified(true);
        user.setConsentAt(clock.instant());

        if (role == Role.EMPLOYER_HR) {
            Company company = companyRepository.save(new Company());
            user.setCompanyId(company.getId());
        }

        userRepository.save(user);
        auditService.record(user.getId(), "user_registered", "user", user.getId().toString(), "{}");

        String refreshToken = issueRefreshToken(user);
        return new AuthSession(jwtService.generateAccessToken(user), refreshToken, UserDto.from(user));
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AuthSession login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.unauthorized("INVALID_CREDENTIALS", "Invalid email or password."));

        if (user.getDisabledAt() != null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED",
                    "This account has been disabled.");
        }

        // Lock is checked before the password.
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(clock.instant())) {
            throw ApiException.locked(minutesUntil(user.getLockedUntil()));
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            int count = user.getFailedLoginCount() + 1;
            if (count >= MAX_FAILED_ATTEMPTS) {
                user.setFailedLoginCount(0);
                user.setLockedUntil(clock.instant().plus(LOCKOUT_DURATION));
            } else {
                user.setFailedLoginCount(count);
            }
            userRepository.save(user);
            throw ApiException.unauthorized("INVALID_CREDENTIALS", "Invalid email or password.");
        }

        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        String refreshToken = issueRefreshToken(user);
        return new AuthSession(jwtService.generateAccessToken(user), refreshToken, UserDto.from(user));
    }

    @Transactional
    public AuthSession refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Authentication required.");
        }
        try {
            RefreshToken token = refreshTokenRepository.findByTokenHashForUpdate(hash(rawToken))
                    .orElseThrow(() -> ApiException.unauthorized("UNAUTHORIZED", "Authentication required."));
            if (token.getRevokedAt() != null || token.getExpiresAt().isBefore(clock.instant())) {
                throw ApiException.unauthorized("UNAUTHORIZED", "Authentication required.");
            }
            User user = userRepository.findById(token.getUserId())
                    .orElseThrow(() -> ApiException.unauthorized("UNAUTHORIZED", "Authentication required."));
            if (user.getDisabledAt() != null) {
                throw ApiException.unauthorized("UNAUTHORIZED", "Authentication required.");
            }

            token.setRevokedAt(clock.instant());
            refreshTokenRepository.save(token);

            String newRefreshToken = issueRefreshToken(user);
            return new AuthSession(jwtService.generateAccessToken(user), newRefreshToken, UserDto.from(user));
        } catch (org.springframework.dao.DataAccessException e) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Authentication required.");
        }
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(clock.instant());
                refreshTokenRepository.save(token);
            }
        });
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.email().trim().toLowerCase();
        userRepository.findByEmail(email).ifPresent(user -> {
            String raw = generateToken();
            PasswordResetToken token = new PasswordResetToken();
            token.setUserId(user.getId());
            token.setTokenHash(hash(raw));
            token.setExpiresAt(clock.instant().plus(RESET_TOKEN_TTL));
            passwordResetTokenRepository.save(token);

            String link = properties.baseUrl() + "/reset-password?token=" + raw;
            mailService.send(user.getEmail(), "Reset your FairFold password",
                    "Use this link to reset your password (valid for 1 hour): " + link);
        });
        // Always return 202; never reveal whether the email exists.
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken reset = passwordResetTokenRepository.findByTokenHash(hash(request.token()))
                .orElseThrow(() -> ApiException.badRequest("TOKEN_INVALID", "The reset link is invalid or has expired."));
        if (reset.getUsedAt() != null || reset.getExpiresAt().isBefore(clock.instant())) {
            throw ApiException.badRequest("TOKEN_INVALID", "The reset link is invalid or has expired.");
        }
        User user = userRepository.findById(reset.getUserId())
                .orElseThrow(() -> ApiException.badRequest("TOKEN_INVALID", "The reset link is invalid or has expired."));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        reset.setUsedAt(clock.instant());

        refreshTokenRepository.revokeAllByUserId(user.getId(), clock.instant());
        auditService.record(user.getId(), "password_reset", "user", user.getId().toString(), "{}");
    }

    @Transactional(readOnly = true)
    public InviteInfo inviteInfo(String rawToken) {
        TeamInvite invite = requireInvite(rawToken);
        Company company = companyRepository.findById(invite.getCompanyId()).orElse(null);
        return new InviteInfo(company == null ? null : company.getName(),
                invite.getRole().name(), invite.getEmail());
    }

    @Transactional
    public AuthSession acceptInvite(AcceptInviteRequest request) {
        if (request.consent() == null || !request.consent()) {
            throw ApiException.badRequest("CONSENT_REQUIRED", "You must agree to the terms to continue.");
        }
        TeamInvite invite = requireInvite(request.token());
        if (userRepository.existsByEmail(invite.getEmail())) {
            throw ApiException.conflict("EMAIL_TAKEN", "An account with this email already exists.");
        }

        User user = new User();
        user.setEmail(invite.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setRole(invite.getRole().toUserRole());
        user.setCompanyId(invite.getCompanyId());
        user.setEmailVerified(true);
        user.setConsentAt(clock.instant());
        userRepository.save(user);

        invite.setAcceptedAt(clock.instant());
        teamInviteRepository.save(invite);
        auditService.record(user.getId(), "user_registered", "user", user.getId().toString(), "{}");

        String refreshToken = issueRefreshToken(user);
        return new AuthSession(jwtService.generateAccessToken(user), refreshToken, UserDto.from(user));
    }

    private TeamInvite requireInvite(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw ApiException.notFound("INVITE_INVALID", "This invite link is invalid or has expired.");
        }
        return teamInviteRepository
                .findByTokenHashAndAcceptedAtIsNullAndExpiresAtAfter(hash(rawToken), clock.instant())
                .orElseThrow(() -> ApiException.notFound("INVITE_INVALID",
                        "This invite link is invalid or has expired."));
    }

    @Transactional(readOnly = true)
    public UserDto me(java.util.UUID userId) {        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("UNAUTHORIZED", "Authentication required."));
        return UserDto.from(user);
    }

    @Transactional
    public void changePassword(java.util.UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("UNAUTHORIZED", "Authentication required."));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("INVALID_PASSWORD", "Current password is incorrect.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        auditService.record(user.getId(), "password_changed", "user", user.getId().toString(), "{}");
    }

    @Transactional
    public void adminSendPasswordReset(java.util.UUID actorId, java.util.UUID targetId) {
        User user = userRepository.findById(targetId)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found."));
        String raw = generateToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(user.getId());
        token.setTokenHash(hash(raw));
        token.setExpiresAt(clock.instant().plus(RESET_TOKEN_TTL));
        passwordResetTokenRepository.save(token);

        String link = properties.baseUrl() + "/reset-password?token=" + raw;
        mailService.send(user.getEmail(), "Reset your FairFold password",
                "Use this link to reset your password (valid for 1 hour): " + link);
        auditService.record(actorId, "admin_password_reset", "user", targetId.toString(), "{}");
    }

    // --- helpers ----------------------------------------------------------

    private Role resolveRole(String role) {
        if ("candidate".equalsIgnoreCase(role)) {
            return Role.CANDIDATE;
        }
        if ("employer".equalsIgnoreCase(role)) {
            return Role.EMPLOYER_HR;
        }
        throw ApiException.badRequest("INVALID_ROLE", "Role must be 'candidate' or 'employer'.");
    }

    private String issueRefreshToken(User user) {
        String raw = generateToken();
        RefreshToken token = new RefreshToken();
        token.setUserId(user.getId());
        token.setTokenHash(hash(raw));
        token.setExpiresAt(clock.instant().plus(REFRESH_TOKEN_TTL));
        refreshTokenRepository.save(token);
        return raw;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private long minutesUntil(Instant until) {
        long millis = Duration.between(clock.instant(), until).toMillis();
        return Math.max(1, (long) Math.ceil(millis / 60000.0));
    }
}
