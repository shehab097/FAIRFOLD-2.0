package com.fairfold.account;

import com.fairfold.common.errors.ApiException;
import com.fairfold.common.storage.StorageService;
import com.fairfold.user.Role;
import com.fairfold.user.User;
import com.fairfold.user.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Part 5.1: FR-041 account deletion with guards and file cleanup. */
@Service
public class AccountErasureService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StorageService storageService;
    private final JdbcTemplate jdbc;

    public AccountErasureService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                 StorageService storageService, JdbcTemplate jdbc) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.storageService = storageService;
        this.jdbc = jdbc;
    }

    @Transactional
    public void delete(UUID userId, DeleteAccountRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("UNAUTHORIZED", "Authentication required."));
        if (request.confirmation() == null || !"DELETE".equals(request.confirmation())) {
            throw ApiException.badRequest("CONFIRMATION_REQUIRED",
                    "Type DELETE exactly to confirm account deletion.");
        }
        if (request.password() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.badRequest("INVALID_PASSWORD", "Password is incorrect.");
        }
        if (user.getRole() == Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw ApiException.conflict("LAST_ADMIN", "The last admin account cannot be deleted.");
        }
        if (user.getRole() == Role.EMPLOYER_HR && user.getCompanyId() != null
                && userRepository.countByCompanyIdAndRoleAndDisabledAtIsNull(
                        user.getCompanyId(), Role.EMPLOYER_HR) <= 1) {
            throw ApiException.conflict("LAST_HR",
                    "The last HR of a company cannot delete their account.");
        }

        List<String> storageKeys = jdbc.queryForList(
                "SELECT storage_key FROM resumes WHERE user_id = ?", String.class, userId.toString());
        for (String storageKey : storageKeys) {
            storageService.delete(storageKey);
        }

        jdbc.update("UPDATE messages SET body = NULL WHERE sender_id = ?", userId.toString());
        jdbc.update("UPDATE audit_log SET actor_id = NULL WHERE actor_id = ?", userId.toString());
        jdbc.update("DELETE FROM users WHERE id = ?", userId.toString());
        jdbc.update("INSERT INTO audit_log (id, actor_id, action, resource_type, resource_id, details) "
                        + "VALUES (?, NULL, 'account_deleted', 'user', ?, ?)",
                UUID.randomUUID().toString(), userId.toString(),
                "{\"role\":\"" + user.getRole().name() + "\"}");
    }
}
