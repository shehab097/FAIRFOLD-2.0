package com.fairfold.admin;

import com.fairfold.admin.dto.AdminStatsResponse;
import com.fairfold.admin.dto.AdminUserItem;
import com.fairfold.admin.dto.RoleChangeRequest;
import com.fairfold.auth.AuthService;
import com.fairfold.common.audit.AuditService;
import com.fairfold.common.errors.ApiException;
import com.fairfold.common.paging.PageResponse;
import com.fairfold.user.Role;
import com.fairfold.user.User;
import com.fairfold.user.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Part 5.1: admin dashboard stats, user search and account actions. */
@Service
public class AdminService {

    private static final Set<Role> EDITABLE_ROLES = EnumSet.of(
            Role.EMPLOYER_HR, Role.EMPLOYER_MANAGER, Role.INTERVIEWER);
    private static final int MAX_PAGE_SIZE = 100;

    private final JdbcTemplate jdbc;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final AuditService auditService;
    private final Clock clock;

    public AdminService(JdbcTemplate jdbc, UserRepository userRepository,
                        AuthService authService, AuditService auditService, Clock clock) {
        this.jdbc = jdbc;
        this.userRepository = userRepository;
        this.authService = authService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse stats() {
        Map<String, Long> usersByRole = new LinkedHashMap<>();
        for (Role role : Role.values()) {
            usersByRole.put(role.name(), 0L);
        }
        jdbc.query("SELECT role, COUNT(*) AS total FROM users GROUP BY role",
                rs -> {
                    usersByRole.put(rs.getString("role").toUpperCase(Locale.ROOT),
                            rs.getLong("total"));
                });

        Map<String, Long> jobsByStatus = new LinkedHashMap<>();
        for (String status : List.of("draft", "active", "paused", "closed")) {
            jobsByStatus.put(status, 0L);
        }
        jdbc.query("SELECT status, COUNT(*) AS total FROM jobs GROUP BY status",
                rs -> {
                    jobsByStatus.put(rs.getString("status").toLowerCase(Locale.ROOT),
                            rs.getLong("total"));
                });

        Long applications = jdbc.queryForObject(
                "SELECT COUNT(*) FROM applications", Long.class);
        LocalDate firstOfMonth = LocalDate.now(clock.withZone(ZoneOffset.UTC)).withDayOfMonth(1);
        Timestamp since = Timestamp.from(firstOfMonth.atStartOfDay().toInstant(ZoneOffset.UTC));
        Long aiCalls = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ai_calls WHERE created_at >= ?", Long.class, since);
        BigDecimal aiCost = jdbc.queryForObject(
                "SELECT COALESCE(SUM(cost), 0) FROM ai_calls WHERE created_at >= ?",
                BigDecimal.class, since);

        return new AdminStatsResponse(usersByRole, jobsByStatus,
                applications == null ? 0 : applications,
                aiCalls == null ? 0 : aiCalls,
                aiCost == null ? BigDecimal.ZERO : aiCost);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserItem> users(String q, String role, Boolean locked,
                                             int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(MAX_PAGE_SIZE, size));
        List<Object> args = new ArrayList<>();
        String where = userWhere(q, role, locked, args);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM users " + where,
                Long.class, args.toArray());
        List<Object> listArgs = new ArrayList<>(args);
        listArgs.add(safeSize);
        listArgs.add(safePage * safeSize);
        List<AdminUserItem> items = jdbc.query(
                "SELECT id, first_name, last_name, email, role, company_id, locked_until, "
                        + "disabled_at, created_at FROM users " + where
                        + " ORDER BY created_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new AdminUserItem(
                        UUID.fromString(rs.getString("id")),
                        (rs.getString("first_name") + " " + rs.getString("last_name")).trim(),
                        rs.getString("email"),
                        rs.getString("role").toUpperCase(Locale.ROOT),
                        rs.getString("company_id") == null
                                ? null : UUID.fromString(rs.getString("company_id")),
                        isLocked(rs.getTimestamp("locked_until")),
                        rs.getTimestamp("disabled_at") != null,
                        false,
                        rs.getTimestamp("created_at").toInstant()),
                listArgs.toArray());
        return PageResponse.of(items, safePage, safeSize, total == null ? 0 : total);
    }

    private String userWhere(String q, String role, Boolean locked, List<Object> args) {
        StringBuilder where = new StringBuilder("WHERE 1=1");
        if (q != null && !q.isBlank()) {
            String like = "%" + q.strip().toLowerCase(Locale.ROOT) + "%";
            where.append(" AND (LOWER(CONCAT(first_name, ' ', last_name)) LIKE ? "
                    + "OR LOWER(email) LIKE ?)");
            args.add(like);
            args.add(like);
        }
        if (role != null && !role.isBlank()) {
            String normalized = role.strip().toUpperCase(Locale.ROOT);
            try {
                Role.valueOf(normalized);
            } catch (RuntimeException e) {
                throw ApiException.badRequest("INVALID_ROLE", "Unknown role filter.");
            }
            where.append(" AND role = ?");
            args.add(normalized);
        }
        if (locked != null) {
            if (locked) {
                where.append(" AND locked_until IS NOT NULL AND locked_until > ?");
            } else {
                where.append(" AND (locked_until IS NULL OR locked_until <= ?)");
            }
            args.add(Timestamp.from(clock.instant()));
        }
        return where.toString();
    }

    private boolean isLocked(Timestamp lockedUntil) {
        return lockedUntil != null && lockedUntil.toInstant().isAfter(clock.instant());
    }

    @Transactional
    public AdminUserItem changeRole(UUID actorId, UUID targetId, RoleChangeRequest request) {
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found."));
        if (target.getId().equals(actorId)) {
            throw ApiException.conflict("CANNOT_CHANGE_SELF", "You cannot change your own role.");
        }
        Role next = parseRole(request.role());
        if (!EDITABLE_ROLES.contains(target.getRole()) || !EDITABLE_ROLES.contains(next)) {
            throw ApiException.conflict("ROLE_CHANGE_NOT_ALLOWED",
                    "Only employer HR, manager and interviewer roles can be changed here.");
        }
        if (target.getRole() == Role.EMPLOYER_HR && next != Role.EMPLOYER_HR
                && target.getCompanyId() != null
                && userRepository.countByCompanyIdAndRoleAndDisabledAtIsNull(
                        target.getCompanyId(), Role.EMPLOYER_HR) <= 1) {
            throw ApiException.conflict("LAST_HR",
                    "A company must keep at least one employer HR member.");
        }
        Role previous = target.getRole();
        target.setRole(next);
        userRepository.save(target);
        auditService.record(actorId, "admin_role_changed", "user", targetId.toString(),
                "{\"from\":\"" + previous.name() + "\",\"to\":\"" + next.name() + "\"}");
        return item(target);
    }

    private Role parseRole(String value) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest("INVALID_ROLE", "Choose a role.");
        }
        try {
            return Role.valueOf(value.strip().toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            throw ApiException.badRequest("INVALID_ROLE", "Unknown role.");
        }
    }

    private AdminUserItem item(User user) {
        return new AdminUserItem(user.getId(),
                (user.getFirstName() + " " + user.getLastName()).trim(),
                user.getEmail(), user.getRole().name(), user.getCompanyId(),
                user.getLockedUntil() != null && user.getLockedUntil().isAfter(clock.instant()),
                user.getDisabledAt() != null, false, user.getCreatedAt());
    }

    @Transactional
    public AdminUserItem unlock(UUID actorId, UUID targetId) {
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found."));
        target.setFailedLoginCount(0);
        target.setLockedUntil(null);
        userRepository.save(target);
        auditService.record(actorId, "admin_unlock", "user", targetId.toString(), "{}");
        return item(target);
    }

    public void sendPasswordReset(UUID actorId, UUID targetId) {
        authService.adminSendPasswordReset(actorId, targetId);
    }
}
