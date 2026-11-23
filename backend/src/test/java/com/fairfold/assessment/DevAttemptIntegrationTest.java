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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Development-only assessment shortcut; never part of the final build. */
class DevAttemptIntegrationTest extends AbstractIntegrationTest {

    private UUID createAssessment(String adminToken) throws Exception {
        List<Map<String, Object>> questions = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            questions.add(Map.of(
                    "text", "Question " + (i + 1),
                    "options", List.of(Map.of("id", "a", "text", "A"),
                            Map.of("id", "b", "text", "B")),
                    "correctOptionId", "b"));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", "Dev " + UUID.randomUUID().toString().substring(0, 6));
        body.put("skillId", skillId("Java"));
        body.put("durationMinutes", 20);
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

    @Test
    void dev_complete_chooses_the_requested_correct_count_and_submits() throws Exception {
        String admin = loginAdmin("dev-admin@example.com");
        UUID assessmentId = createAssessment(admin);
        String candidate = registerAndGetToken("dev-candidate@example.com", "candidate");

        var started = mockMvc.perform(post("/api/assessments/" + assessmentId + "/attempts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate))
                .andExpect(status().isOk())
                .andReturn();
        String attemptId = objectMapper.readTree(started.getResponse().getContentAsString())
                .path("attemptId").asText();

        mockMvc.perform(post("/api/dev/attempts/" + attemptId + "/complete")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correctCount\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("submitted"))
                .andExpect(jsonPath("$.score").value(67))
                .andExpect(jsonPath("$.correctCount").value(2))
                .andExpect(jsonPath("$.passed").value(true));

        mockMvc.perform(post("/api/dev/attempts/" + attemptId + "/complete")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correctCount\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ATTEMPT_CLOSED"));
    }

    @Test
    void dev_complete_defaults_to_all_correct() throws Exception {
        String admin = loginAdmin("dev-admin-2@example.com");
        UUID assessmentId = createAssessment(admin);
        String candidate = registerAndGetToken("dev-candidate-2@example.com", "candidate");

        var started = mockMvc.perform(post("/api/assessments/" + assessmentId + "/attempts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode view = objectMapper.readTree(started.getResponse().getContentAsString());

        mockMvc.perform(post("/api/dev/attempts/" + view.path("attemptId").asText() + "/complete")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(100))
                .andExpect(jsonPath("$.passed").value(true));
    }
}
