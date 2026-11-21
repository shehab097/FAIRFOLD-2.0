package com.fairfold.job;

import com.fairfold.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

class CompanyJobIntegrationTest extends AbstractIntegrationTest {

    @Test
    void Fr045_get_empty_company_profile() throws Exception {
        TestEmployer employer = registerEmployer("hr@acme.com");
        mockMvc.perform(get("/api/employer/company").header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").isEmpty())
                .andExpect(jsonPath("$.industry").isEmpty());
    }

    @Test
    void Fr045_update_company_profile() throws Exception {
        TestEmployer employer = registerEmployer("hr@acme.com");
        String body = """
                {"name":"Acme Corp","industry":"Software","size":"11-50","showNamePublicly":true}
                """;
        mockMvc.perform(put("/api/employer/company")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token()))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme Corp"))
                .andExpect(jsonPath("$.size").value("11-50"))
                .andExpect(jsonPath("$.showNamePublicly").value(true));
    }

    @Test
    void Fr045_update_company_invalid_size_400() throws Exception {
        TestEmployer employer = registerEmployer("hr@acme.com");
        String body = """
                {"name":"Acme","industry":"Software","size":"huge","showNamePublicly":false}
                """;
        mockMvc.perform(put("/api/employer/company")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token()))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SIZE"));
    }

    @Test
    void Fr025_create_draft_job() throws Exception {
        TestEmployer employer = registerEmployer("hr@acme.com");
        UUID java = skillId("Java");
        JsonNode job = createJob(employer.token(), Map.of(
                "requiredSkillIds", List.of(java.toString()),
                "niceToHaveSkillIds", List.of(),
                "mustHaveSkillIds", List.of(java.toString())));
        org.junit.jupiter.api.Assertions.assertEquals("draft", job.get("status").asText());
        org.junit.jupiter.api.Assertions.assertTrue(job.get("requiredSkills").toString().contains(java.toString()));
    }

    @Test
    void Fr025_create_job_missing_country_400() throws Exception {
        TestEmployer employer = registerEmployer("hr@acme.com");
        mockMvc.perform(post("/api/employer/jobs")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobBody(Map.of("country", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void Fr025_create_job_invalid_salary_range_400() throws Exception {
        TestEmployer employer = registerEmployer("hr@acme.com");
        mockMvc.perform(post("/api/employer/jobs")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobBody(Map.of(
                                "salaryType", "range",
                                "salaryMin", 70000,
                                "salaryMax", 50000))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SALARY"));
    }

    @Test
    void Fr025_create_job_must_have_not_required_400() throws Exception {
        TestEmployer employer = registerEmployer("hr@acme.com");
        UUID react = skillId("React");
        mockMvc.perform(post("/api/employer/jobs")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobBody(Map.of("mustHaveSkillIds", List.of(react.toString())))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MUST_HAVE_NOT_REQUIRED"));
    }

    @Test
    void Fr025_create_job_invalid_cutoff_400() throws Exception {
        TestEmployer employer = registerEmployer("hr@acme.com");
        mockMvc.perform(post("/api/employer/jobs")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobBody(Map.of("cutoffScore", 150))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void Fr026_edit_only_in_draft_returns_409() throws Exception {
        TestEmployer employer = registerEmployer("hr@acme.com");
        putCompany(employer);
        UUID jobId = createJobId(employer.token());
        activate(employer, jobId);

        mockMvc.perform(put("/api/employer/jobs/" + jobId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobBody(Map.of("title", "Changed"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("JOB_NOT_DRAFT"));
    }

    @Test
    void Fr027_activate_requires_complete_company_409() throws Exception {
        TestEmployer employer = registerEmployer("hr@acme.com");
        UUID jobId = createJobId(employer.token());
        mockMvc.perform(post("/api/employer/jobs/" + jobId + "/activate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMPANY_INCOMPLETE"));
    }

    @Test
    void Fr027_full_lifecycle() throws Exception {
        TestEmployer employer = registerEmployer("hr@acme.com");
        putCompany(employer);
        UUID jobId = createJobId(employer.token());

        activate(employer, jobId);
        pause(employer, jobId);
        resume(employer, jobId);
        close(employer, jobId);

        mockMvc.perform(get("/api/employer/jobs/" + jobId).header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("closed"));
    }

    @Test
    void Fr027_illegal_transition_409() throws Exception {
        TestEmployer employer = registerEmployer("hr@acme.com");
        putCompany(employer);
        UUID jobId = createJobId(employer.token());
        activate(employer, jobId);
        // activating again is illegal
        mockMvc.perform(post("/api/employer/jobs/" + jobId + "/activate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("JOB_ILLEGAL_TRANSITION"));
    }

    @Test
    void company_scope_other_company_job_returns_404() throws Exception {
        TestEmployer a = registerEmployer("hr@a.com");
        TestEmployer b = registerEmployer("hr@b.com");
        UUID jobId = createJobId(a.token());

        mockMvc.perform(get("/api/employer/jobs/" + jobId).header(HttpHeaders.AUTHORIZATION, bearer(b.token())))
                .andExpect(status().isNotFound());
    }

    // --- helpers -----------------------------------------------------------

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void putCompany(TestEmployer employer) throws Exception {
        mockMvc.perform(put("/api/employer/company")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Acme Corp","industry":"Software","size":"11-50","showNamePublicly":false}
                                """))
                .andExpect(status().isOk());
    }

    private UUID createJobId(String token) throws Exception {
        JsonNode job = createJob(token, Map.of());
        return UUID.fromString(job.get("id").asText());
    }

    private JsonNode createJob(String token, Map<String, Object> overrides) throws Exception {
        var result = mockMvc.perform(post("/api/employer/jobs")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobBody(overrides)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String jobBody(Map<String, Object> overrides) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "Java Developer");
        body.put("description", "Build backend services in Java.");
        body.put("requirements", "");
        body.put("experienceLevel", "senior");
        body.put("city", "Dhaka");
        body.put("country", "Bangladesh");
        body.put("remote", false);
        body.put("requiredSkillIds", List.of());
        body.put("niceToHaveSkillIds", List.of());
        body.put("mustHaveSkillIds", List.of());
        body.putAll(overrides);
        return objectMapper.writeValueAsString(body);
    }

    private void activate(TestEmployer employer, UUID jobId) throws Exception {
        mockMvc.perform(post("/api/employer/jobs/" + jobId + "/activate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("active"));
    }

    private void pause(TestEmployer employer, UUID jobId) throws Exception {
        mockMvc.perform(post("/api/employer/jobs/" + jobId + "/pause")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("paused"));
    }

    private void resume(TestEmployer employer, UUID jobId) throws Exception {
        mockMvc.perform(post("/api/employer/jobs/" + jobId + "/resume")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("active"));
    }

    private void close(TestEmployer employer, UUID jobId) throws Exception {
        mockMvc.perform(post("/api/employer/jobs/" + jobId + "/close")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("closed"));
    }

    @Test
    void upload_and_delete_company_logo() throws Exception {
        TestEmployer employer = registerEmployer("hr_logo@acme.com");
        byte[] pngBytes = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", pngBytes);

        MvcResult uploadResult = mockMvc.perform(multipart("/api/employer/company/logo")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").isNotEmpty())
                .andReturn();

        JsonNode node = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        String logoUrl = node.get("logoUrl").asText();
        assertTrue(logoUrl.startsWith("/api/companies/logo/"));

        // Public retrieval of logo by storage key
        mockMvc.perform(get(logoUrl))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"))
                .andExpect(content().bytes(pngBytes));

        // Delete logo
        mockMvc.perform(delete("/api/employer/company/logo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").isEmpty());
    }

    @Test
    void upload_company_logo_invalid_type_returns_400() throws Exception {
        TestEmployer employer = registerEmployer("hr_invalid@acme.com");
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "not an image".getBytes());

        mockMvc.perform(multipart("/api/employer/company/logo")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_IMAGE"));
    }
}
