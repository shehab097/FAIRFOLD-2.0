package com.fairfold.application;

import com.fairfold.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MyApplicationStatusTest extends AbstractIntegrationTest {

    @Test
    void candidate_job_page_can_see_an_existing_application_and_status() throws Exception {
        TestEmployer employer = registerEmployer("mine.hr@acme.com");
        completeCompany(employer.token());
        UUID jobId = createActiveJob(employer.token());
        TestUser candidate = registerUser("mine.candidate@example.com", "candidate");
        insertResume(candidate.userId());

        mockMvc.perform(get("/api/jobs/" + jobId + "/applications/mine")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied").value(false))
                .andExpect(jsonPath("$.applicationId").value(org.hamcrest.Matchers.nullValue()));

        MvcResult applied = mockMvc.perform(post("/api/jobs/" + jobId + "/applications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isCreated())
                .andReturn();
        String applicationId = objectMapper.readTree(applied.getResponse().getContentAsString())
                .path("id").asText();

        mockMvc.perform(get("/api/jobs/" + jobId + "/applications/mine")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied").value(true))
                .andExpect(jsonPath("$.applicationId").value(applicationId))
                .andExpect(jsonPath("$.status").value("applied"));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
