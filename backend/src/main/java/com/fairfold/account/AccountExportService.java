package com.fairfold.account;

import com.fairfold.user.Role;
import com.fairfold.user.User;
import com.fairfold.user.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Part 5.1: FR-040 immediate JSON data export. */
@Service
public class AccountExportService {

    private final JdbcTemplate jdbc;
    private final UserRepository userRepository;

    public AccountExportService(JdbcTemplate jdbc, UserRepository userRepository) {
        this.jdbc = jdbc;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> export(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> com.fairfold.common.errors.ApiException
                        .unauthorized("UNAUTHORIZED", "Authentication required."));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user", jdbc.queryForList(
                "SELECT id, email, first_name, last_name, role, company_id, avatar_url, "
                        + "email_verified, consent_at, disabled_at, created_at, updated_at "
                        + "FROM users WHERE id = ?", userId.toString()));
        data.put("candidateProfile", jdbc.queryForList(
                "SELECT * FROM candidate_profiles WHERE user_id = ?", userId.toString()));
        data.put("skills", jdbc.queryForList(
                "SELECT cs.id, s.name, cs.level, cs.years, cs.verified, cs.verified_at, cs.created_at "
                        + "FROM candidate_skills cs JOIN skills s ON s.id = cs.skill_id "
                        + "WHERE cs.user_id = ?", userId.toString()));
        data.put("workHistory", jdbc.queryForList(
                "SELECT * FROM work_history WHERE user_id = ?", userId.toString()));
        data.put("certifications", jdbc.queryForList(
                "SELECT * FROM certifications WHERE user_id = ?", userId.toString()));
        data.put("resumes", jdbc.queryForList(
                "SELECT id, original_filename, size_bytes, status, failure_reason, stripped_text, "
                        + "is_current, created_at, updated_at FROM resumes WHERE user_id = ?",
                userId.toString()));
        data.put("resumePlaceholders", jdbc.queryForList(
                "SELECT p.resume_id, p.placeholder, p.kind, p.original_value, p.start_month, "
                        + "p.end_month, p.is_current FROM resume_placeholder_map p "
                        + "JOIN resumes r ON r.id = p.resume_id WHERE r.user_id = ?", userId.toString()));
        data.put("applications", jdbc.queryForList(
                "SELECT a.id, a.job_id, j.title AS job_title, a.status, a.score, a.scoring_mode, "
                        + "a.bias_status, a.bias_note, a.missing_skills, a.pulled_back_at, "
                        + "a.override_direction, a.decision_reason, a.gate_status, "
                        + "a.created_at, a.updated_at FROM applications a "
                        + "JOIN jobs j ON j.id = a.job_id WHERE a.candidate_id = ?", userId.toString()));
        data.put("applicationClaims", jdbc.queryForList(
                "SELECT c.* FROM application_claims c JOIN applications a "
                        + "ON a.id = c.application_id WHERE a.candidate_id = ?", userId.toString()));
        data.put("assessmentAttempts", jdbc.queryForList(
                "SELECT * FROM assessment_attempts WHERE user_id = ?", userId.toString()));
        data.put("practiceSessions", jdbc.queryForList(
                "SELECT * FROM practice_sessions WHERE user_id = ?", userId.toString()));
        data.put("messagesSent", jdbc.queryForList(
                "SELECT id, application_id, body, read_at, created_at FROM messages "
                        + "WHERE sender_id = ?", userId.toString()));
        data.put("notifications", jdbc.queryForList(
                "SELECT * FROM notifications WHERE user_id = ?", userId.toString()));

        if (user.getRole() == Role.EMPLOYER_HR || user.getRole() == Role.EMPLOYER_MANAGER
                || user.getRole() == Role.INTERVIEWER) {
            data.put("company", user.getCompanyId() == null ? List.of()
                    : jdbc.queryForList("SELECT * FROM companies WHERE id = ?",
                    user.getCompanyId().toString()));
            data.put("jobsCreated", jdbc.queryForList(
                    "SELECT * FROM jobs WHERE created_by = ?", userId.toString()));
        }
        return data;
    }
}
