package com.fairfold.interview;

import com.fairfold.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Part 4.1: interview pack generation and saving (FR-031). */
class InterviewPackIntegrationTest extends AbstractIntegrationTest {

    @Test
    void Fr031_generate_is_a_proposal_and_save_persists_the_edited_pack() throws Exception {
        TestEmployer employer = registerEmployer("pack.hr@acme.com");
        completeCompany(employer.token());
        UUID jobId = createJob(employer.token());

        mockMvc.perform(get("/api/employer/jobs/" + jobId + "/interview-pack")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(0))
                .andExpect(jsonPath("$.rubric.length()").value(0));

        var generated = mockMvc.perform(post("/api/employer/jobs/" + jobId + "/interview-pack/generate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("rule_based"))
                .andReturn();
        JsonNode proposal = objectMapper.readTree(generated.getResponse().getContentAsString());
        assertThat(proposal.path("questions").size()).isBetween(6, 10);
        assertThat(proposal.path("rubric").size()).isEqualTo(4);
        assertThat(proposal.path("questions").get(0).path("id").asText()).isNotBlank();

        mockMvc.perform(get("/api/employer/jobs/" + jobId + "/interview-pack")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(0));

        Map<String, Object> save = Map.of(
                "questions", List.of(
                        Map.of("id", "q1", "text", "Tell us about a recent backend project."),
                        Map.of("id", "q2", "text", "How do you review a design?")),
                "rubric", List.of(
                        Map.of("id", "r1", "criterion", "Technical depth",
                                "description", "Depth and accuracy of answers."),
                        Map.of("id", "r2", "criterion", "Communication",
                                "description", "Clear structure.")));
        mockMvc.perform(put("/api/employer/jobs/" + jobId + "/interview-pack")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(save)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(2))
                .andExpect(jsonPath("$.rubric[1].criterion").value("Communication"));

        mockMvc.perform(get("/api/employer/jobs/" + jobId + "/interview-pack")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].text")
                        .value("Tell us about a recent backend project."))
                .andExpect(jsonPath("$.rubric.length()").value(2));

        TestEmployer other = registerEmployer("pack.other@acme.com");
        mockMvc.perform(get("/api/employer/jobs/" + jobId + "/interview-pack")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + other.token()))
                .andExpect(status().isNotFound());
    }

    @Test
    void Fr031_an_empty_pack_is_rejected() throws Exception {
        TestEmployer employer = registerEmployer("pack2.hr@acme.com");
        completeCompany(employer.token());
        UUID jobId = createJob(employer.token());

        mockMvc.perform(put("/api/employer/jobs/" + jobId + "/interview-pack")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questions\":[],\"rubric\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PACK"));
    }
}
