package com.fairfold.assessment;

import com.fairfold.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Part 3.3: candidate certifications (FR-044). */
class CertificationIntegrationTest extends AbstractIntegrationTest {

    private Map<String, Object> body(String name, String issuer) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("issuer", issuer);
        body.put("issuedOn", "2025-03-01");
        body.put("expiresOn", "2028-03-01");
        body.put("credentialId", "AWS-123");
        return body;
    }

    @Test
    void Fr044_candidate_can_add_edit_list_and_delete_certifications() throws Exception {
        String token = registerAndGetToken("cert-candidate@example.com", "candidate");

        var created = mockMvc.perform(post("/api/candidate/certifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body("AWS Solutions Architect", "Amazon"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("AWS Solutions Architect"))
                .andExpect(jsonPath("$.issuer").value("Amazon"))
                .andExpect(jsonPath("$.issuedOn").value("2025-03-01"))
                .andExpect(jsonPath("$.expiresOn").value("2028-03-01"))
                .andExpect(jsonPath("$.credentialId").value("AWS-123"))
                .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("id").asText();

        mockMvc.perform(get("/api/candidate/certifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("AWS Solutions Architect"));

        Map<String, Object> update = body("AWS Solutions Architect - Professional", "Amazon Web Services");
        update.put("credentialId", null);
        mockMvc.perform(put("/api/candidate/certifications/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("AWS Solutions Architect - Professional"))
                .andExpect(jsonPath("$.credentialId").value(org.hamcrest.Matchers.nullValue()));

        mockMvc.perform(delete("/api/candidate/certifications/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/candidate/certifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void Fr044_rejects_an_expiry_before_the_issue_date() throws Exception {
        String token = registerAndGetToken("cert-candidate-2@example.com", "candidate");
        Map<String, Object> body = body("Cert", "Issuer");
        body.put("issuedOn", "2026-01-01");
        body.put("expiresOn", "2025-01-01");

        mockMvc.perform(post("/api/candidate/certifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
    }

    @Test
    void Fr044_certifications_are_private_to_their_owner() throws Exception {
        String owner = registerAndGetToken("cert-owner@example.com", "candidate");
        String other = registerAndGetToken("cert-other@example.com", "candidate");
        TestEmployer employer = registerEmployer("cert.hr@acme.com");

        var created = mockMvc.perform(post("/api/candidate/certifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body("Cert", "Issuer"))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID id = UUID.fromString(objectMapper.readTree(created.getResponse().getContentAsString())
                .path("id").asText());

        mockMvc.perform(put("/api/candidate/certifications/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body("Stolen", "Other"))))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/candidate/certifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isForbidden());
    }
}
