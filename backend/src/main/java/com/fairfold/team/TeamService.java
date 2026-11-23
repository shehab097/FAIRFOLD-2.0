package com.fairfold.team;

import com.fairfold.common.audit.AuditService;
import com.fairfold.common.errors.ApiException;
import com.fairfold.common.mail.MailService;
import com.fairfold.auth.RefreshTokenRepository;
import com.fairfold.config.AppProperties;
import com.fairfold.team.dto.TeamResponse;
import com.fairfold.user.Role;
import com.fairfold.user.User;
import com.fairfold.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class TeamService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final TeamInviteRepository inviteRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MailService mailService;
    private final AuditService auditService;
    private final AppProperties properties;
    private final Clock clock;

    public TeamService(UserRepository userRepository,
                       TeamInviteRepository inviteRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       MailService mailService,
                       AuditService auditService,
                       AppProperties properties,
                       Clock clock) {
        this.userRepository = userRepository;
        this.inviteRepository = inviteRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.mailService = mailService;
        this.auditService = auditService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public TeamResponse list(UUID companyId) {
        List<TeamResponse.Member> members = userRepository
                .findByCompanyIdAndDisabledAtIsNullOrderByFirstNameAscLastNameAsc(companyId).stream()
                .map(this::toMember).toList();
        List<TeamResponse.Invite> invites = inviteRepository
                .findByCompanyIdAndAcceptedAtIsNullOrderByEmailAsc(companyId).stream()
                .map(invite -> new TeamResponse.Invite(
                        invite.getId(), invite.getEmail(), invite.getRole().name(),
                        invite.getExpiresAt()))
                .toList();
        return new TeamResponse(members, invites);
    }

    @Transactional(readOnly = true)
    public List<TeamResponse.Member> members(UUID companyId) {
        return userRepository.findByCompanyIdAndDisabledAtIsNullOrderByFirstNameAscLastNameAsc(companyId)
                .stream().map(this::toMember).toList();
    }

    @Transactional
    public TeamResponse.Invite invite(UUID companyId, UUID actorId, String email, String roleValue) {
        TeamRole role = parseRole(roleValue);
        String normalized = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalized)) {
            throw ApiException.conflict("EMAIL_TAKEN", "An account with this email already exists.");
        }
        for (TeamInvite old : inviteRepository.findByCompanyIdAndEmailAndAcceptedAtIsNull(
                companyId, normalized)) {
            inviteRepository.delete(old);
        }

        String raw = generateToken();
        TeamInvite invite = new TeamInvite();
        invite.setCompanyId(companyId);
        invite.setEmail(normalized);
        invite.setRole(role);
        invite.setTokenHash(hash(raw));
        invite.setInvitedBy(actorId);
        invite.setExpiresAt(clock.instant().plus(Duration.ofDays(validDays())));
        inviteRepository.save(invite);

        String link = properties.baseUrl() + "/accept-invite?token=" + raw;
        mailService.send(normalized, "You are invited to a FairFold company",
                "Accept your invite (valid for " + validDays() + " days): " + link);
        auditService.record(actorId, "team_invited", "team_invite",
                invite.getId().toString(), "{\"role\":\"" + role.name() + "\"}");
        return new TeamResponse.Invite(invite.getId(), invite.getEmail(), role.name(),
                invite.getExpiresAt());
    }

    @Transactional
    public void cancelInvite(UUID companyId, UUID inviteId) {
        TeamInvite invite = inviteRepository.findByIdAndCompanyId(inviteId, companyId)
                .orElseThrow(() -> ApiException.notFound("INVITE_NOT_FOUND", "Invite not found."));
        inviteRepository.delete(invite);
    }

    @Transactional
    public TeamResponse.Member changeRole(UUID companyId, UUID actorId, UUID memberId,
                                          String roleValue) {
        TeamRole role = parseRole(roleValue);
        User member = requireMember(companyId, memberId);
        if (member.getRole() == Role.EMPLOYER_HR && role != TeamRole.employer_hr && isLastHr(companyId)) {
            throw ApiException.conflict("LAST_HR", "The last HR member cannot be demoted.");
        }
        member.setRole(role.toUserRole());
        userRepository.save(member);
        auditService.record(actorId, "team_role_changed", "user", member.getId().toString(),
                "{\"role\":\"" + role.name() + "\"}");
        return toMember(member);
    }

    @Transactional
    public void remove(UUID companyId, UUID actorId, UUID memberId) {
        if (actorId.equals(memberId)) {
            throw ApiException.conflict("CANNOT_REMOVE_SELF", "You cannot remove yourself.");
        }
        User member = requireMember(companyId, memberId);
        if (member.getRole() == Role.EMPLOYER_HR && isLastHr(companyId)) {
            throw ApiException.conflict("LAST_HR", "The last HR member cannot be removed.");
        }
        member.setDisabledAt(clock.instant());
        userRepository.save(member);
        refreshTokenRepository.revokeAllByUserId(member.getId(), clock.instant());
        auditService.record(actorId, "team_member_removed", "user", member.getId().toString(), "{}");
    }

    private User requireMember(UUID companyId, UUID memberId) {
        return userRepository.findByIdAndCompanyIdAndDisabledAtIsNull(memberId, companyId)
                .orElseThrow(() -> ApiException.notFound("MEMBER_NOT_FOUND", "Team member not found."));
    }

    private boolean isLastHr(UUID companyId) {
        return userRepository.countByCompanyIdAndRoleAndDisabledAtIsNull(
                companyId, Role.EMPLOYER_HR) <= 1;
    }

    private TeamResponse.Member toMember(User user) {
        return new TeamResponse.Member(
                user.getId(),
                (user.getFirstName() + " " + user.getLastName()).trim(),
                user.getEmail(),
                user.getRole().name().toLowerCase());
    }

    private TeamRole parseRole(String value) {
        try {
            return TeamRole.fromString(value);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("INVALID_ROLE",
                    "Role must be employer_hr, employer_manager or interviewer.");
        }
    }

    private int validDays() {
        return properties.invite() == null ? 7 : Math.max(1, properties.invite().validDays());
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
}
