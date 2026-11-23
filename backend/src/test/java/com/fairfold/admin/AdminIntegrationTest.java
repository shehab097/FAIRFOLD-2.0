package com.fairfold.admin;

import com.fairfold.common.storage.StorageService;
import com.fairfold.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Part 5.1: admin dashboard, users, audit CSV, export and account erasure. */
class AdminIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private StorageService storageService;

    private UUID idByEmail(String email) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", UUID.class, email);
    }

    private void insertTeamMember(String email, String role, UUID companyId) {
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role, company_id, "
                        + "consent_at) VALUES (?, ?, ?, 'Second', 'Hr', ?, ?, ?)",
                UUID.randomUUID().toString(), email, passwordEncoder.encode("password123"),
                role, companyId.toString(), Timestamp.from(Instant.now()));
    }

    private UUID applyCandidate(String candidateToken, String employerToken, UUID resumeId)
            throws Exception {
        completeCompany(employerToken);
        UUID jobId = createActiveJob(employerToken);
        var result = mockMvc.perform(post("/api/jobs/" + jobId + "/applications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidateToken))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id").asText());
    }

    @Test
    void Fr037_stats_count_users_jobs_applications_and_ai_usage() throws Exception {
        String admin = loginAdmin("stats.admin@example.com");
        registerUser("stats.candidate@example.com", "candidate");
        TestEmployer employer = registerEmployer("stats.employer@example.com");
        completeCompany(employer.token());
        createActiveJob(employer.token());
        createJob(employer.token());

        Timestamp now = Timestamp.from(Instant.now());
        Timestamp lastMonth = Timestamp.from(Instant.now().minusSeconds(40L * 24 * 3600));
        jdbcTemplate.update("INSERT INTO ai_calls (id, purpose, model, prompt_tokens, completion_tokens, "
                        + "cost, status, created_at) VALUES (?, 'screening', 'deepseek-flash', 10, 5, 0.25, 'ok', ?)",
                UUID.randomUUID().toString(), now);
        jdbcTemplate.update("INSERT INTO ai_calls (id, purpose, model, prompt_tokens, completion_tokens, "
                        + "cost, status, created_at) VALUES (?, 'screening', 'deepseek-flash', 10, 5, 0.50, 'ok', ?)",
                UUID.randomUUID().toString(), now);
        jdbcTemplate.update("INSERT INTO ai_calls (id, purpose, model, prompt_tokens, completion_tokens, "
                        + "cost, status, created_at) VALUES (?, 'screening', 'deepseek-flash', 10, 5, 9.00, 'ok', ?)",
                UUID.randomUUID().toString(), lastMonth);

        mockMvc.perform(get("/api/admin/stats")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usersByRole.CANDIDATE").value(1))
                .andExpect(jsonPath("$.usersByRole.EMPLOYER_HR").value(1))
                .andExpect(jsonPath("$.usersByRole.ADMIN").value(1))
                .andExpect(jsonPath("$.jobsByStatus.active").value(1))
                .andExpect(jsonPath("$.jobsByStatus.draft").value(1))
                .andExpect(jsonPath("$.applicationsTotal").value(0))
                .andExpect(jsonPath("$.aiCallsThisMonth").value(2))
                .andExpect(jsonPath("$.aiCostThisMonthUsd").value(0.75));
    }

    @Test
    void Fr038_user_search_role_rules_unlock_and_password_reset() throws Exception {
        String admin = loginAdmin("users.admin@example.com");
        TestUser candidate = registerUser("users.candidate@example.com", "candidate");
        TestEmployer hr = registerEmployer("users.hr@example.com");
        insertTeamMember("users.second-hr@example.com", "EMPLOYER_HR", hr.companyId());
        UUID secondHr = idByEmail("users.second-hr@example.com");
        UUID hrId = idByEmail("users.hr@example.com");
        UUID adminId = idByEmail("users.admin@example.com");

        mockMvc.perform(get("/api/admin/users")
                        .param("q", "hr")
                        .param("role", "EMPLOYER_HR")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items[0].mfaEnabled").value(false));

        mockMvc.perform(patch("/api/admin/users/" + candidate.userId() + "/role")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"EMPLOYER_MANAGER\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROLE_CHANGE_NOT_ALLOWED"));

        mockMvc.perform(patch("/api/admin/users/" + adminId + "/role")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"EMPLOYER_MANAGER\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CANNOT_CHANGE_SELF"));

        mockMvc.perform(patch("/api/admin/users/" + hrId + "/role")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"EMPLOYER_MANAGER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("EMPLOYER_MANAGER"));

        mockMvc.perform(patch("/api/admin/users/" + secondHr + "/role")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"INTERVIEWER\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LAST_HR"));

        jdbcTemplate.update("UPDATE users SET failed_login_count = 5, locked_until = ? WHERE id = ?",
                Timestamp.from(Instant.now().plusSeconds(3600)), candidate.userId().toString());
        mockMvc.perform(get("/api/admin/users")
                        .param("locked", "true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1));

        mockMvc.perform(post("/api/admin/users/" + candidate.userId() + "/unlock")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked").value(false));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT failed_login_count FROM users WHERE id = ?", Integer.class,
                candidate.userId().toString())).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT locked_until FROM users WHERE id = ?", Timestamp.class,
                candidate.userId().toString())).isNull();

        mockMvc.perform(post("/api/admin/users/" + candidate.userId() + "/password-reset")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                .andExpect(status().isAccepted());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM password_reset_tokens WHERE user_id = ?", Long.class,
                candidate.userId().toString())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action IN "
                        + "('admin_role_changed','admin_unlock','admin_password_reset')", Long.class))
                .isEqualTo(3);
    }

    @Test
    void Fr039_audit_filters_csv_and_no_personal_details() throws Exception {
        String admin = loginAdmin("audit.admin@example.com");
        TestUser candidate = registerUser("audit.candidate@example.com", "candidate");
        TestEmployer employer = registerEmployer("audit.employer@example.com");
        UUID resumeId = insertResume(candidate.userId());
        UUID applicationId = applyCandidate(candidate.token(), employer.token(), resumeId);
        jdbcTemplate.update("INSERT INTO messages (id, application_id, sender_id, body) "
                        + "VALUES (?, ?, ?, ?)", UUID.randomUUID().toString(),
                applicationId.toString(), candidate.userId().toString(), "private audit text");

        mockMvc.perform(get("/api/admin/audit")
                        .param("action", "user_registered")
                        .param("resourceType", "user")
                        .param("actorId", candidate.userId().toString())
                        .param("from", LocalDate.now().toString())
                        .param("to", LocalDate.now().toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].action").value("user_registered"))
                .andExpect(jsonPath("$.items[0].actorEmail").value("audit.candidate@example.com"));

        var csv = mockMvc.perform(get("/api/admin/audit")
                        .param("format", "csv")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,
                        org.hamcrest.Matchers.containsString("text/csv")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("attachment;")))
                .andReturn().getResponse().getContentAsString();
        assertThat(csv).startsWith("id,createdAt,actorId,actorEmail,action,resourceType,resourceId,details");
        assertThat(csv).contains("user_registered");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = 'audit_exported'", Long.class))
                .isEqualTo(1);

        List<String> details = jdbcTemplate.queryForList(
                "SELECT details FROM audit_log", String.class);
        for (String row : details) {
            assertThat(row).doesNotContain("audit.candidate@example.com", "First Last",
                    "private audit text");
        }
    }

    @Test
    void Fr040_export_contains_own_data_and_no_secrets() throws Exception {
        TestUser candidate = registerUser("export.candidate@example.com", "candidate");
        jdbcTemplate.update("INSERT INTO candidate_profiles (user_id, phone, title, location) "
                        + "VALUES (?, '01700000000', 'Backend Engineer', 'Dhaka')",
                candidate.userId().toString());
        jdbcTemplate.update("INSERT INTO candidate_skills (id, user_id, skill_id, level) "
                        + "VALUES (?, ?, ?, 'expert')", UUID.randomUUID().toString(),
                candidate.userId().toString(), skillId("Java").toString());
        jdbcTemplate.update("INSERT INTO work_history (id, user_id, title, employer, start_month, "
                        + "skills_used) VALUES (?, ?, 'Engineer', 'Acme', '2020-01-01', ?)",
                UUID.randomUUID().toString(), candidate.userId().toString(), "[\"Java\"]");
        jdbcTemplate.update("INSERT INTO certifications (id, user_id, name, issuer) "
                        + "VALUES (?, ?, 'AWS', 'Amazon')", UUID.randomUUID().toString(),
                candidate.userId().toString());
        UUID resumeId = insertResume(candidate.userId());
        jdbcTemplate.update("UPDATE resumes SET status = 'ready', stripped_text = ? WHERE id = ?",
                "Stripped [EMPLOYER_1] resume", resumeId.toString());
        jdbcTemplate.update("INSERT INTO resume_placeholder_map (id, resume_id, placeholder, kind, "
                        + "original_value) VALUES (?, ?, 'EMPLOYER_1', 'employer', 'Acme')",
                UUID.randomUUID().toString(), resumeId.toString());

        TestEmployer employer = registerEmployer("export.employer@example.com");
        UUID applicationId = applyCandidate(candidate.token(), employer.token(), resumeId);
        jdbcTemplate.update("INSERT INTO messages (id, application_id, sender_id, body) "
                        + "VALUES (?, ?, ?, 'my export message')", UUID.randomUUID().toString(),
                applicationId.toString(), candidate.userId().toString());
        jdbcTemplate.update("INSERT INTO notifications (id, user_id, type, title, body) "
                        + "VALUES (?, ?, 'test', 'Hello', 'Notification body')",
                UUID.randomUUID().toString(), candidate.userId().toString());

        UUID assessmentId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO assessments (id, title, skill_id, duration_minutes, pass_mark) "
                        + "VALUES (?, 'Java Basics', ?, 20, 70)", assessmentId.toString(),
                skillId("Java").toString());
        jdbcTemplate.update("INSERT INTO assessment_attempts (id, assessment_id, user_id, started_at, "
                        + "ends_at, score, passed, status) VALUES (?, ?, ?, ?, ?, 80, true, 'submitted')",
                UUID.randomUUID().toString(), assessmentId.toString(), candidate.userId().toString(),
                Timestamp.from(Instant.now()), Timestamp.from(Instant.now().plusSeconds(600)));
        UUID practiceQuestion = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO practice_questions (id, text, topic) "
                + "VALUES (?, 'Tell me about a project.', 'Behavioral')", practiceQuestion.toString());
        jdbcTemplate.update("INSERT INTO practice_sessions (id, user_id, question_id, answer) "
                        + "VALUES (?, ?, ?, 'I built a service.')", UUID.randomUUID().toString(),
                candidate.userId().toString(), practiceQuestion.toString());

        var response = mockMvc.perform(get("/api/account/export")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate.token()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("fairfold-data-")))
                .andReturn();
        String body = response.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        for (String section : List.of("user", "candidateProfile", "skills", "workHistory",
                "certifications", "resumes", "resumePlaceholders", "applications",
                "applicationClaims", "assessmentAttempts", "practiceSessions", "messagesSent",
                "notifications")) {
            assertThat(json.has(section)).as(section).isTrue();
        }
        assertThat(json.path("user").get(0).path("email").asText())
                .isEqualTo("export.candidate@example.com");
        assertThat(json.path("candidateProfile").get(0).path("phone").asText())
                .isEqualTo("01700000000");
        assertThat(json.path("skills").get(0).path("name").asText()).isEqualTo("Java");
        assertThat(json.path("messagesSent").get(0).path("body").asText())
                .isEqualTo("my export message");
        assertThat(body).doesNotContain("password_hash", "passwordHash", "refresh_token",
                "mfa_secret", "password123");

        var employerExport = mockMvc.perform(get("/api/account/export")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode employerJson = objectMapper.readTree(employerExport);
        assertThat(employerJson.path("company")).hasSize(1);
        assertThat(employerJson.path("jobsCreated")).hasSize(1);
    }

    @Test
    void Fr041_deletes_candidate_data_but_keeps_blank_audit_rows() throws Exception {
        TestUser candidate = registerUser("erase.candidate@example.com", "candidate");
        TestEmployer employer = registerEmployer("erase.employer@example.com");
        UUID resumeId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO resumes (id, user_id, original_filename, storage_key, "
                        + "size_bytes, status, is_current, current_user_key) VALUES (?, ?, 'cv.pdf', "
                        + "'delete-me.pdf', 10, 'ready', true, ?)", resumeId.toString(),
                candidate.userId().toString(), candidate.userId().toString());
        UUID applicationId = applyCandidate(candidate.token(), employer.token(), resumeId);
        UUID messageId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO messages (id, application_id, sender_id, body) "
                        + "VALUES (?, ?, ?, 'erase this body')", messageId.toString(),
                applicationId.toString(), candidate.userId().toString());

        mockMvc.perform(delete("/api/account")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "password", "wrong-password", "confirmation", "DELETE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD"));

        mockMvc.perform(delete("/api/account")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "password", "password123", "confirmation", "delete"))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/account")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "password", "password123", "confirmation", "DELETE"))))
                .andExpect(status().isNoContent());

        verify(storageService).delete("delete-me.pdf");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ?", Long.class,
                candidate.userId().toString())).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM applications WHERE id = ?", Long.class,
                applicationId.toString())).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM messages WHERE id = ?", Long.class,
                messageId.toString())).as("candidate application threads cascade").isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE actor_id = ?", Long.class,
                candidate.userId().toString())).isZero();
        Map<String, Object> deleted = jdbcTemplate.queryForMap(
                "SELECT actor_id, details FROM audit_log WHERE action = 'account_deleted'");
        assertThat(deleted.get("actor_id")).isNull();
        assertThat(objectMapper.readTree(deleted.get("details").toString()).path("role").asText())
                .isEqualTo("CANDIDATE");
        assertThat(deleted.get("details").toString()).doesNotContain("erase.candidate",
                "password", "First", "Last");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "erase.candidate@example.com", "password", "password123"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void Fr041_employer_deletion_blanks_sent_messages_but_keeps_them() throws Exception {
        TestUser candidate = registerUser("erase.message.candidate@example.com", "candidate");
        TestEmployer employer = registerEmployer("erase.message.employer@example.com");
        UUID resumeId = insertResume(candidate.userId());
        UUID applicationId = applyCandidate(candidate.token(), employer.token(), resumeId);

        UUID managerId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO users (id, email, password_hash, first_name, last_name, "
                        + "role, company_id, consent_at) VALUES (?, 'erase.manager@example.com', ?, "
                        + "'Erase', 'Manager', 'EMPLOYER_MANAGER', ?, ?)",
                managerId.toString(), passwordEncoder.encode("password123"),
                employer.companyId().toString(), Timestamp.from(Instant.now()));
        var login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "erase.manager@example.com", "password", "password123"))))
                .andExpect(status().isOk())
                .andReturn();
        String managerToken = objectMapper.readTree(login.getResponse().getContentAsString())
                .path("accessToken").asText();

        UUID messageId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO messages (id, application_id, sender_id, body) "
                        + "VALUES (?, ?, ?, 'employer message')", messageId.toString(),
                applicationId.toString(), managerId.toString());
        jdbcTemplate.update("INSERT INTO audit_log (id, actor_id, action, resource_type, details) "
                        + "VALUES (?, ?, 'identity_reveal', 'application', '{}')",
                UUID.randomUUID().toString(), managerId.toString());

        mockMvc.perform(delete("/api/account")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "password", "password123", "confirmation", "DELETE"))))
                .andExpect(status().isNoContent());

        Map<String, Object> message = jdbcTemplate.queryForMap(
                "SELECT body, sender_id FROM messages WHERE id = ?", messageId.toString());
        assertThat(message.get("body")).isNull();
        assertThat(message.get("sender_id")).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE actor_id = ?", Long.class,
                managerId.toString())).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = 'account_deleted' "
                        + "AND details LIKE '%EMPLOYER_MANAGER%'", Long.class)).isEqualTo(1);
    }

    @Test
    void Fr041_guards_last_admin_and_last_hr() throws Exception {
        String admin = loginAdmin("erase.admin@example.com");
        mockMvc.perform(delete("/api/account")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "password", "password123", "confirmation", "DELETE"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LAST_ADMIN"));

        TestEmployer hr = registerEmployer("erase.hr@example.com");
        completeCompany(hr.token());
        mockMvc.perform(delete("/api/account")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + hr.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "password", "password123", "confirmation", "DELETE"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LAST_HR"));
    }
}
