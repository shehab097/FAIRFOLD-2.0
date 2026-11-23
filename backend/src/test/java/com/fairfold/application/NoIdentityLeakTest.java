package com.fairfold.application;

import com.fairfold.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NoIdentityLeakTest extends AbstractIntegrationTest {

    @Test
    void employer_applicant_list_never_contains_identity() throws Exception {
        TestEmployer employer = registerEmployer("hr@acme.com");
        completeCompany(employer.token());
        UUID jobId = createActiveJob(employer.token());

        // Distinctive identity, applied before shortlist.
        String token = registerDistinctiveCandidate();
        UUID candidateId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = 'distinctive.candidate@secret.com'", UUID.class);
        UUID resumeId = insertResume(candidateId);
        jdbcTemplate.update("UPDATE resumes SET status = 'ready', stripped_text = ? WHERE id = ?",
                "Built backend services and reviewed code.", resumeId.toString());

        MvcResult applied = mockMvc.perform(post("/api/jobs/" + jobId + "/applications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();
        String applicationId = objectMapper.readTree(applied.getResponse().getContentAsString())
                .path("id").asText();

        mockMvc.perform(post("/api/employer/jobs/" + jobId + "/screening")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isAccepted());

        for (int i = 0; i < 200; i++) {
            MvcResult progress = mockMvc.perform(get("/api/employer/jobs/" + jobId + "/screening")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                    .andExpect(status().isOk())
                    .andReturn();
            if ("finished".equals(objectMapper.readTree(progress.getResponse().getContentAsString())
                    .path("status").asText())) {
                break;
            }
            Thread.sleep(50);
        }

        MvcResult result = mockMvc.perform(get("/api/employer/jobs/" + jobId + "/applications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains("Distinctive"));
        assertFalse(body.contains("distinctive.candidate@secret.com"));
        assertFalse(body.contains("+8801712345678"));
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("C-"));

        MvcResult review = mockMvc.perform(get("/api/employer/applications/" + applicationId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isOk())
                .andReturn();
        String reviewBody = review.getResponse().getContentAsString();
        assertFalse(reviewBody.contains("Distinctive"));
        assertFalse(reviewBody.contains("distinctive.candidate@secret.com"));
        assertFalse(reviewBody.contains("+8801712345678"));
        assertFalse(reviewBody.contains("\"identity\":{"));

        // Part 3.3 fields: required-assessment score and proctoring summary must not leak identity.
        String admin = loginAdmin("leak-admin@example.com");
        String assessmentTitle = "Leak assessment " + UUID.randomUUID().toString().substring(0, 5);
        UUID assessmentId = createAssessment(admin, assessmentTitle);
        jdbcTemplate.update("""
                        INSERT INTO job_required_assessments (id, job_id, assessment_id, min_score)
                        VALUES (?, ?, ?, 70)
                        """,
                UUID.randomUUID().toString(), jobId.toString(), assessmentId.toString());
        portalAssessmentAttempt(token, assessmentId);

        MvcResult reviewWithAssessments = mockMvc.perform(get("/api/employer/applications/" + applicationId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.assessments[0].assessmentTitle").value(assessmentTitle))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.assessments[0].focus.recorded").value(true))
                .andReturn();
        String assessmentBody = reviewWithAssessments.getResponse().getContentAsString();
        assertFalse(assessmentBody.contains("Distinctive"));
        assertFalse(assessmentBody.contains("distinctive.candidate@secret.com"));
        assertFalse(assessmentBody.contains("+8801712345678"));
        assertFalse(assessmentBody.contains("\"identity\":{"));

        MvcResult dashboard = mockMvc.perform(get("/api/employer/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isOk())
                .andReturn();
        String dashboardBody = dashboard.getResponse().getContentAsString();
        assertFalse(dashboardBody.contains("Distinctive"));
        assertFalse(dashboardBody.contains("distinctive.candidate@secret.com"));
    }

    private String registerDistinctiveCandidate() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"distinctive.candidate@secret.com","password":"password123",
                                 "firstName":"Distinctive","lastName":"Candidate","role":"candidate","consent":true}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private UUID createAssessment(String adminToken, String title) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", title);
        body.put("skillId", skillId("Java"));
        body.put("durationMinutes", 20);
        body.put("passMark", 70);
        body.put("active", true);
        body.put("questions", List.of(Map.of(
                "text", "One question",
                "options", List.of(Map.of("id", "a", "text", "A"),
                        Map.of("id", "b", "text", "B")),
                "correctOptionId", "b")));
        MvcResult result = mockMvc.perform(post("/api/admin/assessments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id").asText());
    }

    private void portalAssessmentAttempt(String token, UUID assessmentId) throws Exception {
        MvcResult started = mockMvc.perform(post("/api/assessments/" + assessmentId + "/attempts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String attemptId = objectMapper.readTree(started.getResponse().getContentAsString())
                .path("attemptId").asText();
        mockMvc.perform(post("/api/attempts/" + attemptId + "/consent")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"given\":true}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/attempts/" + attemptId + "/events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"type\":\"hidden\",\"durationSeconds\":30}]"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/attempts/" + attemptId + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }
}
