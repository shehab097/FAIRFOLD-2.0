package com.fairfold.assessment;

import com.fairfold.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Part 3.1: admin content (FR-049, FR-061). */
class AdminContentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private LearningResourceRepository resourceRepository;

    private String assessmentBody(UUID skillId, String title, String questionText) {
        return """
                {
                  "title": "%s",
                  "skillId": "%s",
                  "durationMinutes": 30,
                  "passMark": 70,
                  "active": true,
                  "questions": [
                    {
                      "text": "%s",
                      "options": [
                        {"id": "a", "text": "Wrong"},
                        {"id": "b", "text": "Right"},
                        {"id": "c", "text": "Also wrong"}
                      ],
                      "correctOptionId": "b"
                    },
                    {
                      "text": "Second question",
                      "options": [
                        {"id": "a", "text": "Yes"},
                        {"id": "b", "text": "No"}
                      ],
                      "correctOptionId": "a"
                    }
                  ]
                }
                """.formatted(title, skillId, questionText);
    }

    private UUID createAssessment(String token, String title) throws Exception {
        var result = mockMvc.perform(post("/api/admin/assessments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assessmentBody(skillId("Java"), title, "What is Java?")))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id").asText());
    }

    private UUID createResource(String token, String url) throws Exception {
        String body = """
                {"skillId":"%s","title":"Java Guide","url":"%s",
                 "cost":"free","language":"English","level":"beginner","active":true}
                """.formatted(skillId("Java"), url);
        var result = mockMvc.perform(post("/api/admin/resources")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id").asText());
    }

    @Test
    void Fr049_admin_creates_assessment_with_questions_and_lists_it() throws Exception {
        String token = loginAdmin("assess-admin@example.com");

        UUID id = createAssessment(token, "Java Screening");

        mockMvc.perform(get("/api/admin/assessments").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(id.toString()))
                .andExpect(jsonPath("$.items[0].title").value("Java Screening"))
                .andExpect(jsonPath("$.items[0].skillName").value("Java"))
                .andExpect(jsonPath("$.items[0].questionCount").value(2))
                .andExpect(jsonPath("$.items[0].attemptCount").value(0));

        mockMvc.perform(get("/api/admin/assessments/{id}", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].position").value(0))
                .andExpect(jsonPath("$.questions[0].correctOptionId").value("b"))
                .andExpect(jsonPath("$.questions[1].position").value(1))
                .andExpect(jsonPath("$.questions[1].options[0].id").value("a"));
    }

    @Test
    void Fr049_rejects_question_without_exactly_one_correct_option() throws Exception {
        String token = loginAdmin("assess-admin-2@example.com");
        String body = assessmentBody(skillId("Java"), "Broken", "Pick one")
                .replace("\"correctOptionId\": \"b\"", "\"correctOptionId\": \"z\"");

        mockMvc.perform(post("/api/admin/assessments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_QUESTION"));
    }

    @Test
    void Fr049_rejects_one_option_and_duplicate_option_ids() throws Exception {
        String token = loginAdmin("assess-admin-3@example.com");
        String oneOption = """
                {"title":"Too small","skillId":"%s","durationMinutes":30,"passMark":50,
                 "questions":[{"text":"Q","options":[{"id":"a","text":"Only"}],"correctOptionId":"a"}]}
                """.formatted(skillId("Java"));
        mockMvc.perform(post("/api/admin/assessments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(oneOption))
                .andExpect(status().isBadRequest());

        String duplicates = """
                {"title":"Duplicates","skillId":"%s","durationMinutes":30,"passMark":50,
                 "questions":[{"text":"Q","options":[{"id":"a","text":"One"},{"id":"a","text":"Two"}],
                 "correctOptionId":"a"}]}
                """.formatted(skillId("Java"));
        mockMvc.perform(post("/api/admin/assessments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicates))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_QUESTION"));
    }

    @Test
    void Fr049_deactivates_and_reactivates_instead_of_deleting() throws Exception {
        String token = loginAdmin("assess-admin-4@example.com");
        UUID id = createAssessment(token, "Lifecycle");

        mockMvc.perform(patch("/api/admin/assessments/{id}/active", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/admin/assessments/{id}", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(patch("/api/admin/assessments/{id}/active", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void Fr049_questions_lock_once_an_attempt_exists() throws Exception {
        String token = loginAdmin("assess-admin-5@example.com");
        UUID id = createAssessment(token, "Locked");
        TestUser candidate = registerUser("lock-candidate@example.com", "candidate");
        insertAttempt(id, candidate.userId());

        String changed = assessmentBody(skillId("Java"), "Locked", "A changed question?");
        mockMvc.perform(put("/api/admin/assessments/{id}", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changed))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASSESSMENT_HAS_ATTEMPTS"));

        String sameQuestionsNewMeta = assessmentBody(skillId("Java"), "Renamed", "What is Java?")
                .replace("\"durationMinutes\": 30", "\"durationMinutes\": 45");
        mockMvc.perform(put("/api/admin/assessments/{id}", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sameQuestionsNewMeta))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Renamed"))
                .andExpect(jsonPath("$.durationMinutes").value(45))
                .andExpect(jsonPath("$.attemptCount").value(1));
    }

    @Test
    void Fr049_non_admins_get_403() throws Exception {
        String candidate = registerAndGetToken("not-admin@example.com", "candidate");
        mockMvc.perform(get("/api/admin/assessments")
                        .header("Authorization", "Bearer " + candidate))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/assessments")
                        .header("Authorization", "Bearer " + candidate)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assessmentBody(skillId("Java"), "Nope", "Q")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/resources")
                        .header("Authorization", "Bearer " + candidate))
                .andExpect(status().isForbidden());
    }

    @Test
    void Fr061_checker_marks_failing_url_broken_and_recovers() throws Exception {
        String token = loginAdmin("link-admin@example.com");
        UUID id = createResource(token, "https://example.com/java");

        ResourceLinkChecker failing = new ResourceLinkChecker(
                resourceRepository, Clock.systemUTC(), url -> 500);
        assertThat(failing.checkAll().broken()).isEqualTo(1);

        var broken = resourceRepository.findById(id).orElseThrow();
        assertThat(broken.isBroken()).isTrue();
        assertThat(broken.getLastCheckedAt()).isNotNull();

        ResourceLinkChecker healthy = new ResourceLinkChecker(
                resourceRepository, Clock.systemUTC(), url -> 200);
        assertThat(healthy.checkAll().broken()).isZero();
        assertThat(resourceRepository.findById(id).orElseThrow().isBroken()).isFalse();
    }

    @Test
    void Fr061_refuses_localhost_and_private_addresses() throws Exception {
        String token = loginAdmin("ssrf-admin@example.com");
        UUID id = createResource(token, "http://localhost:8080/internal");
        createResource(token, "https://example.com/ok");

        ResourceLinkChecker checker = new ResourceLinkChecker(resourceRepository, Clock.systemUTC(), url -> {
            if (url.contains("localhost")) {
                throw new AssertionError("No request may be sent to localhost");
            }
            return 200;
        });
        var result = checker.checkAll();

        assertThat(result.checked()).isEqualTo(2);
        assertThat(result.broken()).isEqualTo(1);
        assertThat(resourceRepository.findById(id).orElseThrow().isBroken()).isTrue();
        assertThat(checker.isPublicHttpUrl("http://127.0.0.1/x")).isFalse();
        assertThat(checker.isPublicHttpUrl("http://10.1.2.3/x")).isFalse();
        assertThat(checker.isPublicHttpUrl("ftp://example.com/x")).isFalse();
        assertThat(checker.isPublicHttpUrl("https://example.com/x")).isTrue();
    }

    @Test
    void Fr061_manual_check_endpoint_reports_checked_and_broken() throws Exception {
        String token = loginAdmin("manual-link-admin@example.com");
        createResource(token, "http://127.0.0.1/private");

        mockMvc.perform(post("/api/admin/resources/check")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checked").value(1))
                .andExpect(jsonPath("$.broken").value(1));
    }

    @Test
    void Fr061_resources_are_deactivated_not_deleted() throws Exception {
        String token = loginAdmin("resource-admin@example.com");
        UUID id = createResource(token, "https://example.com/r");

        mockMvc.perform(patch("/api/admin/resources/{id}/active", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/admin/resources")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(id.toString()))
                .andExpect(jsonPath("$.items[0].active").value(false));
    }

    private void insertAttempt(UUID assessmentId, UUID userId) {
        jdbcTemplate.update("""
                        INSERT INTO assessment_attempts
                            (id, assessment_id, user_id, started_at, ends_at, status)
                        VALUES (?, ?, ?, ?, ?, 'in_progress')
                        """,
                UUID.randomUUID().toString(), assessmentId.toString(), userId.toString(),
                Timestamp.from(Instant.now()), Timestamp.from(Instant.now().plusSeconds(1800)));
    }
}
