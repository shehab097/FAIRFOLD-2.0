package com.fairfold.application;

import com.fairfold.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

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
}
