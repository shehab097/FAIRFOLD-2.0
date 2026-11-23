package com.fairfold.assessment;

import com.fairfold.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Part 3.3: light proctoring (FR-055). */
class ProctoringIntegrationTest extends AbstractIntegrationTest {

    private UUID createAssessment(String adminToken) throws Exception {
        List<Map<String, Object>> questions = List.of(
                Map.of("text", "Question 1",
                        "options", List.of(Map.of("id", "a", "text", "A"),
                                Map.of("id", "b", "text", "B")),
                        "correctOptionId", "b"),
                Map.of("text", "Question 2",
                        "options", List.of(Map.of("id", "a", "text", "A"),
                                Map.of("id", "b", "text", "B")),
                        "correctOptionId", "a"));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", "Proctor " + UUID.randomUUID().toString().substring(0, 6));
        body.put("skillId", skillId("Java"));
        body.put("durationMinutes", 30);
        body.put("passMark", 50);
        body.put("active", true);
        body.put("questions", questions);
        var result = mockMvc.perform(post("/api/admin/assessments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id").asText());
    }

    private String startAttempt(String token, UUID assessmentId) throws Exception {
        var result = mockMvc.perform(post("/api/assessments/" + assessmentId + "/attempts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("attemptId").asText();
    }

    private String eventsJson(int count) {
        List<Map<String, Object>> events = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            events.add(Map.of(
                    "type", i % 2 == 0 ? "hidden" : "blur",
                    "occurredAt", "2026-11-23T10:00:00Z",
                    "durationSeconds", 5));
        }
        try {
            return objectMapper.writeValueAsString(events);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Test
    void Fr055_events_are_ignored_without_consent() throws Exception {
        String admin = loginAdmin("proctor-admin-1@example.com");
        UUID assessmentId = createAssessment(admin);
        String token = registerAndGetToken("proctor-1@example.com", "candidate");
        String attemptId = startAttempt(token, assessmentId);

        mockMvc.perform(post("/api/attempts/" + attemptId + "/events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventsJson(3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recorded").value(0))
                .andExpect(jsonPath("$.total").value(0));

        mockMvc.perform(post("/api/attempts/" + attemptId + "/consent")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"given\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.given").value(false));

        mockMvc.perform(post("/api/attempts/" + attemptId + "/events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventsJson(3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recorded").value(0));
    }

    @Test
    void Fr055_consent_true_records_events_capped_at_500_and_never_changes_the_score() throws Exception {
        String admin = loginAdmin("proctor-admin-2@example.com");
        UUID assessmentId = createAssessment(admin);
        String token = registerAndGetToken("proctor-2@example.com", "candidate");
        String attemptId = startAttempt(token, assessmentId);

        mockMvc.perform(post("/api/attempts/" + attemptId + "/consent")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"given\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/attempts/" + attemptId + "/events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventsJson(600)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recorded").value(500))
                .andExpect(jsonPath("$.total").value(500));

        mockMvc.perform(post("/api/attempts/" + attemptId + "/events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventsJson(5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recorded").value(0))
                .andExpect(jsonPath("$.total").value(500));

        // No answers were given: the score is 0 even though 500 focus signals were recorded.
        mockMvc.perform(post("/api/attempts/" + attemptId + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(0))
                .andExpect(jsonPath("$.passed").value(false))
                .andExpect(jsonPath("$.proctoringConsent").value(true));
    }

    @Test
    void Fr055_explanation_requires_consent_and_is_limited_to_500_characters() throws Exception {
        String admin = loginAdmin("proctor-admin-3@example.com");
        UUID assessmentId = createAssessment(admin);
        String token = registerAndGetToken("proctor-3@example.com", "candidate");
        String attemptId = startAttempt(token, assessmentId);

        mockMvc.perform(put("/api/attempts/" + attemptId + "/explanation")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"My connection dropped.\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONSENT_REQUIRED"));

        mockMvc.perform(post("/api/attempts/" + attemptId + "/consent")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"given\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/attempts/" + attemptId + "/explanation")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("text", "x".repeat(501)))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/attempts/" + attemptId + "/explanation")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"My connection dropped for a minute.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("My connection dropped for a minute."));

        mockMvc.perform(post("/api/attempts/" + attemptId + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/attempts/" + attemptId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.explanation").value("My connection dropped for a minute."));
    }

    @Test
    void Fr055_consent_is_recorded_only_once() throws Exception {
        String admin = loginAdmin("proctor-admin-4@example.com");
        UUID assessmentId = createAssessment(admin);
        String token = registerAndGetToken("proctor-4@example.com", "candidate");
        String attemptId = startAttempt(token, assessmentId);

        mockMvc.perform(post("/api/attempts/" + attemptId + "/consent")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"given\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/attempts/" + attemptId + "/consent")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"given\":false}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONSENT_ALREADY_RECORDED"));
    }
}
