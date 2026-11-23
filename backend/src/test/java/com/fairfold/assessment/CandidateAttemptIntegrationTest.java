package com.fairfold.assessment;

import com.fairfold.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Part 3.2: taking assessments, scoring, cooldown and the Verified badge. */
class CandidateAttemptIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AutoSubmitSweeper sweeper;

    private record QuestionData(UUID id, String correctOptionId) {
    }

    private record Fixture(UUID assessmentId, UUID skillId, List<QuestionData> questions) {
    }

    // --- fixtures ----------------------------------------------------------

    private Fixture createAssessment(String adminToken, int durationMinutes, int passMark, int count)
            throws Exception {
        List<Map<String, Object>> questions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            questions.add(Map.of(
                    "text", "Question " + (i + 1),
                    "options", List.of(
                            Map.of("id", "a", "text", "Option A"),
                            Map.of("id", "b", "text", "Option B"),
                            Map.of("id", "c", "text", "Option C")),
                    "correctOptionId", "b"));
        }
        UUID javaId = skillId("Java");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", "Java Test " + UUID.randomUUID().toString().substring(0, 6));
        body.put("skillId", javaId);
        body.put("durationMinutes", durationMinutes);
        body.put("passMark", passMark);
        body.put("active", true);
        body.put("questions", questions);

        var result = mockMvc.perform(post("/api/admin/assessments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        List<QuestionData> data = new ArrayList<>();
        for (JsonNode question : node.path("questions")) {
            data.add(new QuestionData(UUID.fromString(question.path("id").asText()),
                    question.path("correctOptionId").asText()));
        }
        return new Fixture(UUID.fromString(node.path("id").asText()), javaId, data);
    }

    private JsonNode start(String token, UUID assessmentId) throws Exception {
        var result = mockMvc.perform(post("/api/assessments/{id}/attempts", assessmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private Map<String, String> answers(Fixture fixture, JsonNode view, int correctCount) {
        Map<String, String> answers = new LinkedHashMap<>();
        JsonNode questions = view.path("questions");
        for (int i = 0; i < questions.size(); i++) {
            String questionId = questions.get(i).path("id").asText();
            String correct = fixture.questions().get(i).correctOptionId();
            answers.put(questionId, i < correctCount ? correct : ("b".equals(correct) ? "a" : "b"));
        }
        return answers;
    }

    private void save(String token, UUID attemptId, Map<String, String> answers) throws Exception {
        mockMvc.perform(put("/api/attempts/{id}/answers", attemptId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("answers", answers))))
                .andExpect(status().isOk());
    }

    private JsonNode submit(String token, UUID attemptId) throws Exception {
        var result = mockMvc.perform(post("/api/attempts/{id}/submit", attemptId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode takeAssessment(String adminToken, String candidateToken,
                                    int passMark, int questionCount, int correctCount) throws Exception {
        Fixture fixture = createAssessment(adminToken, 30, passMark, questionCount);
        JsonNode view = start(candidateToken, fixture.assessmentId());
        save(candidateToken, UUID.fromString(view.path("attemptId").asText()),
                answers(fixture, view, correctCount));
        return submit(candidateToken, UUID.fromString(view.path("attemptId").asText()));
    }

    // --- FR-019 ------------------------------------------------------------

    @Test
    void Fr019_start_returns_no_correct_ids_and_resumes_the_same_attempt() throws Exception {
        String admin = loginAdmin("attempt-admin-1@example.com");
        Fixture fixture = createAssessment(admin, 30, 70, 3);
        String candidate = registerAndGetToken("attempt-candidate-1@example.com", "candidate");

        JsonNode view = start(candidate, fixture.assessmentId());
        assertThat(view.path("attemptId").asText()).isNotBlank();
        assertThat(view.path("serverTime").asText()).isNotBlank();
        assertThat(view.path("endsAt").asText()).isNotBlank();
        assertThat(view.path("questions")).hasSize(3);
        assertThat(view.path("questions").get(0).path("text").asText()).isEqualTo("Question 1");
        assertThat(view.path("questions").get(0).has("correctOptionId")).isFalse();
        assertThat(view.path("questions").get(0).path("options").get(0).has("correct")).isFalse();
        assertThat(view.has("score")).isFalse();

        JsonNode resumed = start(candidate, fixture.assessmentId());
        assertThat(resumed.path("attemptId").asText()).isEqualTo(view.path("attemptId").asText());
    }

    @Test
    void Fr019_autosave_merges_answers() throws Exception {
        String admin = loginAdmin("attempt-admin-2@example.com");
        Fixture fixture = createAssessment(admin, 30, 70, 3);
        String candidate = registerAndGetToken("attempt-candidate-2@example.com", "candidate");
        JsonNode view = start(candidate, fixture.assessmentId());
        UUID attemptId = UUID.fromString(view.path("attemptId").asText());

        save(candidate, attemptId, Map.of(fixture.questions().get(0).id().toString(), "a"));
        save(candidate, attemptId, Map.of(fixture.questions().get(1).id().toString(), "c"));

        var result = mockMvc.perform(get("/api/attempts/{id}", attemptId)
                        .header("Authorization", "Bearer " + candidate))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode saved = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(saved.path("answers").path(fixture.questions().get(0).id().toString()).asText())
                .isEqualTo("a");
        assertThat(saved.path("answers").path(fixture.questions().get(1).id().toString()).asText())
                .isEqualTo("c");
    }

    @Test
    void Fr019_rejects_answers_after_the_grace_period() throws Exception {
        String admin = loginAdmin("attempt-admin-3@example.com");
        Fixture fixture = createAssessment(admin, 30, 70, 3);
        String candidate = registerAndGetToken("attempt-candidate-3@example.com", "candidate");
        UUID attemptId = UUID.fromString(start(candidate, fixture.assessmentId())
                .path("attemptId").asText());

        jdbcTemplate.update("UPDATE assessment_attempts SET ends_at = DATE_SUB(NOW(3), INTERVAL 30 SECOND) WHERE id = ?",
                attemptId.toString());

        mockMvc.perform(put("/api/attempts/{id}/answers", attemptId)
                        .header("Authorization", "Bearer " + candidate)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("answers",
                                Map.of(fixture.questions().get(0).id().toString(), "a")))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ATTEMPT_CLOSED"));
    }

    @Test
    void Fr019_sweeper_auto_submits_expired_attempts() throws Exception {
        String admin = loginAdmin("attempt-admin-4@example.com");
        Fixture fixture = createAssessment(admin, 30, 70, 4);
        String candidate = registerAndGetToken("attempt-candidate-4@example.com", "candidate");
        JsonNode view = start(candidate, fixture.assessmentId());
        UUID attemptId = UUID.fromString(view.path("attemptId").asText());
        save(candidate, attemptId, answers(fixture, view, 4));

        jdbcTemplate.update("UPDATE assessment_attempts SET ends_at = DATE_SUB(NOW(3), INTERVAL 30 SECOND) WHERE id = ?",
                attemptId.toString());

        assertThat(sweeper.sweep()).isBetween(0, 1);

        mockMvc.perform(get("/api/attempts/{id}", attemptId)
                        .header("Authorization", "Bearer " + candidate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("auto_submitted"))
                .andExpect(jsonPath("$.score").value(100))
                .andExpect(jsonPath("$.passed").value(true));
    }

    // --- FR-020 ------------------------------------------------------------

    @Test
    void Fr020_scores_correct_answers_and_counts_unanswered_as_wrong() throws Exception {
        String admin = loginAdmin("attempt-admin-5@example.com");
        String candidate = registerAndGetToken("attempt-candidate-5@example.com", "candidate");

        JsonNode passed = takeAssessment(admin, candidate, 70, 4, 3);
        assertThat(passed.path("score").asInt()).isEqualTo(75);
        assertThat(passed.path("correctCount").asInt()).isEqualTo(3);
        assertThat(passed.path("totalQuestions").asInt()).isEqualTo(4);
        assertThat(passed.path("passed").asBoolean()).isTrue();
        assertThat(passed.path("status").asText()).isEqualTo("submitted");

        JsonNode failed = takeAssessment(admin, candidate, 70, 4, 1);
        assertThat(failed.path("score").asInt()).isEqualTo(25);
        assertThat(failed.path("passed").asBoolean()).isFalse();
    }

    @Test
    void Fr020_pass_mark_boundary_is_inclusive() throws Exception {
        String admin = loginAdmin("attempt-admin-6@example.com");
        String candidate = registerAndGetToken("attempt-candidate-6@example.com", "candidate");

        JsonNode atBoundary = takeAssessment(admin, candidate, 50, 4, 2);
        assertThat(atBoundary.path("score").asInt()).isEqualTo(50);
        assertThat(atBoundary.path("passed").asBoolean()).isTrue();

        JsonNode belowBoundary = takeAssessment(admin, candidate, 60, 4, 2);
        assertThat(belowBoundary.path("score").asInt()).isEqualTo(50);
        assertThat(belowBoundary.path("passed").asBoolean()).isFalse();
    }

    @Test
    void Fr020_rounds_the_percentage() throws Exception {
        String admin = loginAdmin("attempt-admin-7@example.com");
        String candidate = registerAndGetToken("attempt-candidate-7@example.com", "candidate");
        JsonNode result = takeAssessment(admin, candidate, 50, 3, 1);
        assertThat(result.path("score").asInt()).isEqualTo(33);
    }

    // --- FR-063 ------------------------------------------------------------

    @Test
    void Fr063_cooldown_blocks_a_retake_after_a_fail_and_reports_retry_time() throws Exception {
        String admin = loginAdmin("attempt-admin-8@example.com");
        Fixture fixture = createAssessment(admin, 30, 70, 4);
        String candidate = registerAndGetToken("attempt-candidate-8@example.com", "candidate");

        JsonNode view = start(candidate, fixture.assessmentId());
        save(candidate, UUID.fromString(view.path("attemptId").asText()),
                answers(fixture, view, 0));
        JsonNode failed = submit(candidate, UUID.fromString(view.path("attemptId").asText()));
        assertThat(failed.path("passed").asBoolean()).isFalse();
        assertThat(failed.path("retryAvailableAt").asText()).isNotBlank();

        var blocked = mockMvc.perform(post("/api/assessments/{id}/attempts", fixture.assessmentId())
                        .header("Authorization", "Bearer " + candidate))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COOLDOWN"))
                .andExpect(jsonPath("$.retryAvailableAt").isNotEmpty())
                .andReturn();
        assertThat(blocked.getResponse().getContentAsString()).contains("retryAvailableAt");

        mockMvc.perform(get("/api/assessments").header("Authorization", "Bearer " + candidate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].retryAvailableAt").isNotEmpty())
                .andExpect(jsonPath("$[0].passed").value(false));

        jdbcTemplate.update("UPDATE assessment_attempts SET submitted_at = DATE_SUB(NOW(3), INTERVAL 49 HOUR) WHERE id = ?",
                view.path("attemptId").asText());

        JsonNode retake = start(candidate, fixture.assessmentId());
        assertThat(retake.path("attemptId").asText()).isNotEqualTo(view.path("attemptId").asText());
    }

    @Test
    void Fr063_pass_creates_a_verified_skill_and_still_allows_a_retake() throws Exception {
        String admin = loginAdmin("attempt-admin-9@example.com");
        Fixture fixture = createAssessment(admin, 30, 50, 4);
        String candidate = registerAndGetToken("attempt-candidate-9@example.com", "candidate");

        JsonNode view = start(candidate, fixture.assessmentId());
        save(candidate, UUID.fromString(view.path("attemptId").asText()),
                answers(fixture, view, 4));
        JsonNode result = submit(candidate, UUID.fromString(view.path("attemptId").asText()));
        assertThat(result.path("passed").asBoolean()).isTrue();
        assertThat(result.path("skillVerified").asBoolean()).isTrue();

        mockMvc.perform(get("/api/candidate/skills")
                        .header("Authorization", "Bearer " + candidate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Java"))
                .andExpect(jsonPath("$[0].verified").value(true))
                .andExpect(jsonPath("$[0].level").value("beginner"))
                .andExpect(jsonPath("$[0].years").value(org.hamcrest.Matchers.nullValue()));

        JsonNode retake = start(candidate, fixture.assessmentId());
        assertThat(retake.path("attemptId").asText()).isNotEqualTo(view.path("attemptId").asText());
    }

    @Test
    void Fr063_failed_result_lists_only_active_working_resources() throws Exception {
        String admin = loginAdmin("attempt-admin-10@example.com");
        Fixture fixture = createAssessment(admin, 30, 70, 3);
        String candidate = registerAndGetToken("attempt-candidate-10@example.com", "candidate");

        UUID good = createResource(admin, fixture.skillId(), "Good guide");
        UUID broken = createResource(admin, fixture.skillId(), "Broken guide");
        createResource(admin, fixture.skillId(), "Inactive guide");
        jdbcTemplate.update("UPDATE resources SET broken = true WHERE id = ?", broken.toString());
        jdbcTemplate.update("UPDATE resources SET active = true WHERE id = ?", good.toString());
        jdbcTemplate.update("UPDATE resources SET active = false WHERE title = 'Inactive guide'");

        mockMvc.perform(get("/api/resources")
                        .param("skillId", fixture.skillId().toString())
                        .header("Authorization", "Bearer " + candidate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Good guide"));

        JsonNode view = start(candidate, fixture.assessmentId());
        save(candidate, UUID.fromString(view.path("attemptId").asText()),
                answers(fixture, view, 0));
        JsonNode failed = submit(candidate, UUID.fromString(view.path("attemptId").asText()));
        assertThat(failed.path("resources")).hasSize(1);
        assertThat(failed.path("resources").get(0).path("title").asText()).isEqualTo("Good guide");

        JsonNode passed = takeAssessment(admin, candidate, 50, 3, 3);
        assertThat(passed.has("resources")).isFalse();
    }

    @Test
    void Fr019_inactive_assessment_cannot_be_started() throws Exception {
        String admin = loginAdmin("attempt-admin-11@example.com");
        Fixture fixture = createAssessment(admin, 30, 70, 3);
        String candidate = registerAndGetToken("attempt-candidate-11@example.com", "candidate");
        mockMvc.perform(patch("/api/admin/assessments/{id}/active", fixture.assessmentId())
                        .header("Authorization", "Bearer " + admin)
                        .contentType("application/json")
                        .content("{\"active\":false}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/assessments/{id}/attempts", fixture.assessmentId())
                        .header("Authorization", "Bearer " + candidate))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASSESSMENT_INACTIVE"));
    }

    @Test
    void Fr019_another_candidate_cannot_read_an_attempt() throws Exception {
        String admin = loginAdmin("attempt-admin-12@example.com");
        Fixture fixture = createAssessment(admin, 30, 70, 3);
        String owner = registerAndGetToken("attempt-owner@example.com", "candidate");
        String other = registerAndGetToken("attempt-other@example.com", "candidate");
        UUID attemptId = UUID.fromString(start(owner, fixture.assessmentId())
                .path("attemptId").asText());

        mockMvc.perform(get("/api/attempts/{id}", attemptId)
                        .header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/assessments")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isForbidden());
    }

    private UUID createResource(String adminToken, UUID skillId, String title) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("skillId", skillId);
        body.put("title", title);
        body.put("url", "https://example.com/" + UUID.randomUUID());
        body.put("cost", "free");
        body.put("language", "English");
        body.put("level", "beginner");
        body.put("active", true);
        var result = mockMvc.perform(post("/api/admin/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id").asText());
    }
}
