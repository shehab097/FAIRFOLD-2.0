package com.fairfold.application;

import com.fairfold.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApplicationIntegrationTest extends AbstractIntegrationTest {

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private TestEmployer activeEmployer(String email) throws Exception {
        TestEmployer employer = registerEmployer(email);
        completeCompany(employer.token());
        createActiveJob(employer.token());
        return employer;
    }

    private UUID employerUserId(UUID companyId) {
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE company_id = ?", UUID.class,
                companyId.toString());
    }

    private long notificationsOfType(UUID userId, String type) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = ?", Long.class,
                userId.toString(), type);
    }

    @Test
    void Fr022_apply_success_and_notifications() throws Exception {
        TestEmployer employer = activeEmployer("hr@acme.com");
        UUID jobId = createActiveJob(employer.token());
        TestUser candidate = registerUser("cand@example.com", "candidate");
        insertResume(candidate.userId());

        mockMvc.perform(post("/api/jobs/" + jobId + "/applications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("applied"))
                .andExpect(jsonPath("$.jobTitle").value("Java Developer"));

        assertEquals(1, notificationsOfType(candidate.userId(), "application_submitted"));
        assertEquals(1, notificationsOfType(employerUserId(employer.companyId()), "application_received"));
    }

    @Test
    void Fr022_already_applied_returns_409() throws Exception {
        TestEmployer employer = activeEmployer("hr@acme.com");
        UUID jobId = createActiveJob(employer.token());
        TestUser candidate = registerUser("cand@example.com", "candidate");
        insertResume(candidate.userId());

        mockMvc.perform(post("/api/jobs/" + jobId + "/applications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/jobs/" + jobId + "/applications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_APPLIED"));
    }

    @Test
    void Fr022_job_not_active_returns_409() throws Exception {
        TestEmployer employer = registerEmployer("hr@acme.com");
        completeCompany(employer.token());
        UUID draftJob = createJob(employer.token());
        TestUser candidate = registerUser("cand@example.com", "candidate");
        insertResume(candidate.userId());

        mockMvc.perform(post("/api/jobs/" + draftJob + "/applications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("JOB_NOT_ACTIVE"));
    }

    @Test
    void Fr022_resume_required_returns_409() throws Exception {
        TestEmployer employer = activeEmployer("hr@acme.com");
        UUID jobId = createActiveJob(employer.token());
        TestUser candidate = registerUser("cand@example.com", "candidate");

        mockMvc.perform(post("/api/jobs/" + jobId + "/applications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESUME_REQUIRED"));
    }

    @Test
    void Fr023_status_history_and_employer_list() throws Exception {
        TestEmployer employer = activeEmployer("hr@acme.com");
        UUID jobId = createActiveJob(employer.token());
        TestUser candidate = registerUser("cand@example.com", "candidate");
        insertResume(candidate.userId());

        var result = mockMvc.perform(post("/api/jobs/" + jobId + "/applications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isCreated())
                .andReturn();
        UUID applicationId = UUID.fromString(objectMapper.readTree(
                result.getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(get("/api/candidate/applications/" + applicationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("applied"))
                .andExpect(jsonPath("$.history[0].status").value("applied"));

        mockMvc.perform(get("/api/candidate/applications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].jobTitle").value("Java Developer"));

        mockMvc.perform(get("/api/employer/jobs/" + jobId + "/applications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].label").value("Applicant 1"))
                .andExpect(jsonPath("$.items[0].status").value("applied"));
    }
}
