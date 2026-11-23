package com.fairfold.assessment;

import com.fairfold.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CV queue, 30-question pool, random 10-question draws and admin approval. */
class AssessmentPoolIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AssessmentQueueService queueService;

    private UUID createOneQuestionAssessment(String adminToken, UUID skillId) throws Exception {
        return createAssessment(adminToken, skillId, 1);
    }

    private UUID createAssessment(String adminToken, UUID skillId, int questions) throws Exception {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < questions; i++) {
            list.add(Map.of(
                    "text", "Question " + (i + 1),
                    "options", List.of(Map.of("id", "a", "text", "A"),
                            Map.of("id", "b", "text", "B"),
                            Map.of("id", "c", "text", "C"),
                            Map.of("id", "d", "text", "D")),
                    "correctOptionId", "b"));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", "Pool " + UUID.randomUUID().toString().substring(0, 6));
        body.put("skillId", skillId);
        body.put("durationMinutes", 30);
        body.put("passMark", 70);
        body.put("active", true);
        body.put("questionsPerAttempt", 10);
        body.put("questions", list);
        var result = mockMvc.perform(post("/api/admin/assessments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id").asText());
    }

    private Map<String, String> correctAnswers(String adminToken, UUID assessmentId,
                                               JsonNode attemptQuestions) throws Exception {
        var detail = mockMvc.perform(get("/api/admin/assessments/" + assessmentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode questions = objectMapper.readTree(detail.getResponse().getContentAsString())
                .path("questions");
        Map<String, String> correct = new LinkedHashMap<>();
        for (JsonNode question : questions) {
            correct.put(question.path("id").asText(), question.path("correctOptionId").asText());
        }
        Map<String, String> drawn = new LinkedHashMap<>();
        for (JsonNode question : attemptQuestions) {
            String id = question.path("id").asText();
            if (correct.containsKey(id)) {
                drawn.put(id, correct.get(id));
            }
        }
        return drawn;
    }

    @Test
    void pool_generation_approval_and_random_ten_question_attempts() throws Exception {
        String admin = loginAdmin("pool-admin@example.com");
        UUID assessmentId = createOneQuestionAssessment(admin, skillId("Java"));

        mockMvc.perform(post("/api/admin/assessments/" + assessmentId + "/generate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewStatus").value("pending_review"))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.poolSize").value(30))
                .andExpect(jsonPath("$.questions.length()").value(30))
                .andExpect(jsonPath("$.questionsPerAttempt").value(10));

        mockMvc.perform(post("/api/admin/assessments/" + assessmentId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewStatus").value("approved"))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/api/admin/assessments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].reviewStatus").value("approved"))
                .andExpect(jsonPath("$.items[0].poolSize").value(30))
                .andExpect(jsonPath("$.items[0].questionsPerAttempt").value(10));

        String candidate = registerAndGetToken("pool-candidate@example.com", "candidate");
        var started = mockMvc.perform(post("/api/assessments/" + assessmentId + "/attempts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode first = objectMapper.readTree(started.getResponse().getContentAsString());
        String attemptId = first.path("attemptId").asText();
        assertThat(first.path("questions").size()).isEqualTo(10);
        assertThat(first.path("totalQuestions").asInt()).isEqualTo(10);
        UUID storedAttemptId = UUID.fromString(attemptId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT JSON_LENGTH(question_ids) FROM assessment_attempts WHERE id = ?",
                Integer.class, storedAttemptId.toString())).isEqualTo(10);
        List<String> firstDrawn = new ArrayList<>();
        first.path("questions").forEach(q -> firstDrawn.add(q.path("id").asText()));
        assertThat(firstDrawn).doesNotHaveDuplicates();

        // Resuming returns exactly the same drawn questions.
        var resumed = mockMvc.perform(post("/api/assessments/" + assessmentId + "/attempts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode resumedView = objectMapper.readTree(resumed.getResponse().getContentAsString());
        assertThat(resumedView.path("attemptId").asText()).isEqualTo(attemptId);
        List<String> resumedDrawn = new ArrayList<>();
        resumedView.path("questions").forEach(q -> resumedDrawn.add(q.path("id").asText()));
        assertThat(resumedDrawn).containsExactlyElementsOf(firstDrawn);

        Map<String, String> answers = correctAnswers(admin, assessmentId, first.path("questions"));
        mockMvc.perform(put("/api/attempts/" + attemptId + "/answers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("answers", answers))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/attempts/" + attemptId + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(100))
                .andExpect(jsonPath("$.correctCount").value(10))
                .andExpect(jsonPath("$.totalQuestions").value(10));

        var secondStarted = mockMvc.perform(post("/api/assessments/" + assessmentId + "/attempts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode second = objectMapper.readTree(secondStarted.getResponse().getContentAsString());
        assertThat(second.path("questions").size()).isEqualTo(10);
        Map<String, String> secondAnswers = correctAnswers(admin, assessmentId,
                second.path("questions"));
        String firstQuestion = secondAnswers.keySet().iterator().next();
        String correct = secondAnswers.get(firstQuestion);
        secondAnswers.put(firstQuestion, "a".equals(correct) ? "b" : "a");
        mockMvc.perform(put("/api/attempts/" + second.path("attemptId").asText() + "/answers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("answers", secondAnswers))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/attempts/" + second.path("attemptId").asText() + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(90))
                .andExpect(jsonPath("$.correctCount").value(9))
                .andExpect(jsonPath("$.totalQuestions").value(10));
    }

    @Test
    void generate_accepts_a_custom_pool_size() throws Exception {
        String admin = loginAdmin("pool-admin-2@example.com");
        UUID assessmentId = createOneQuestionAssessment(admin, skillId("Go"));

        mockMvc.perform(post("/api/admin/assessments/" + assessmentId + "/generate")
                        .param("count", "50")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.poolSize").value(50))
                .andExpect(jsonPath("$.questions.length()").value(50))
                .andExpect(jsonPath("$.reviewStatus").value("pending_review"));
    }

    @Test
    void candidate_skill_without_assessment_creates_one_pending_generation_topic() throws Exception {        String candidate = registerAndGetToken("queue-candidate@example.com", "candidate");
        UUID javaId = skillId("Java");

        mockMvc.perform(post("/api/candidate/skills")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "skillId", javaId.toString(), "level", "intermediate"))))
                .andExpect(status().isCreated());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM assessments WHERE skill_id = ?", Long.class,
                javaId.toString())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT review_status FROM assessments WHERE skill_id = ?", String.class,
                javaId.toString())).isEqualTo("pending_generation");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT active FROM assessments WHERE skill_id = ?", Boolean.class,
                javaId.toString())).isFalse();

        queueService.ensureTopicForSkill(javaId);
        queueService.ensureTopicForName("Java");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM assessments WHERE skill_id = ?", Long.class,
                javaId.toString())).isEqualTo(1);

        mockMvc.perform(get("/api/admin/assessments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginAdmin("queue-admin@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].reviewStatus").value("pending_generation"))
                .andExpect(jsonPath("$.items[0].poolSize").value(0))
                .andExpect(jsonPath("$.items[0].questionsPerAttempt").value(10))
                .andExpect(jsonPath("$.items[0].candidateDemandCount").value(1));
    }
}
