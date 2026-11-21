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
        insertResume(candidateId);

        mockMvc.perform(post("/api/jobs/" + jobId + "/applications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/api/employer/jobs/" + jobId + "/applications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains("Distinctive"));
        assertFalse(body.contains("distinctive.candidate@secret.com"));
        assertFalse(body.contains("+8801712345678"));
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
