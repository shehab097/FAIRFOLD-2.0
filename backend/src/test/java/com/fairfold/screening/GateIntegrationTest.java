package com.fairfold.screening;

import com.fairfold.assessment.GateService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Part 3.3: required assessments and the application gate (FR-051). */
class GateIntegrationTest extends ScreeningTestSupport {

    @Autowired
    private GateService gateService;

    private record Fixture(UUID assessmentId, List<String> correctOptionIds) {
    }

    private Fixture createAssessment(String adminToken) throws Exception {
        List<Map<String, Object>> questions = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            questions.add(Map.of(
                    "text", "Question " + (i + 1),
                    "options", List.of(
                            Map.of("id", "a", "text", "A"),
                            Map.of("id", "b", "text", "B"),
                            Map.of("id", "c", "text", "C")),
                    "correctOptionId", i == 1 ? "c" : "b"));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", "Gate " + UUID.randomUUID().toString().substring(0, 6));
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
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        List<String> correct = new ArrayList<>();
        for (JsonNode question : node.path("questions")) {
            correct.add(question.path("correctOptionId").asText());
        }
        return new Fixture(UUID.fromString(node.path("id").asText()), correct);
    }

    private void requireAssessment(UUID jobId, UUID assessmentId, int minScore) {
        jdbcTemplate.update("""
                        INSERT INTO job_required_assessments (id, job_id, assessment_id, min_score)
                        VALUES (?, ?, ?, ?)
                        """,
                UUID.randomUUID().toString(), jobId.toString(), assessmentId.toString(), minScore);
    }

    private void insertAttempt(UUID assessmentId, UUID userId, int score) {
        jdbcTemplate.update("""
                        INSERT INTO assessment_attempts
                            (id, assessment_id, user_id, started_at, ends_at, submitted_at,
                             answers, score, passed, status)
                        VALUES (?, ?, ?, DATE_SUB(NOW(3), INTERVAL 1 HOUR),
                                DATE_ADD(NOW(3), INTERVAL 30 MINUTE), CURRENT_TIMESTAMP(3),
                                JSON_OBJECT(), ?, ?, 'submitted')
                        """,
                UUID.randomUUID().toString(), assessmentId.toString(), userId.toString(),
                score, score >= 50);
    }

    private String gate(UUID applicationId) {
        return jdbcTemplate.queryForObject(
                "SELECT gate_status FROM applications WHERE id = ?", String.class,
                applicationId.toString());
    }

    @Test
    void Fr051_gate_truth_table_not_required_pending_failed_passed() throws Exception {
        String admin = loginAdmin("gate-admin@example.com");
        TestEmployer employer = registerEmployer("gate.hr@acme.com");
        completeCompany(employer.token());
        UUID jobId = createScreeningJob(employer.token());
        Candidate candidate = insertApplicant(jobId, "gate.candidate@example.com", "Gate", "Candidate",
                "Dhaka, Bangladesh", new BigDecimal("5"), true);

        gateService.recompute(candidate.applicationId());
        assertThat(gate(candidate.applicationId())).isEqualTo("not_required");

        Fixture fixture = createAssessment(admin);
        requireAssessment(jobId, fixture.assessmentId(), 70);
        gateService.recompute(candidate.applicationId());
        assertThat(gate(candidate.applicationId())).isEqualTo("pending");

        insertAttempt(fixture.assessmentId(), candidate.userId(), 50);
        gateService.recompute(candidate.applicationId());
        assertThat(gate(candidate.applicationId())).isEqualTo("failed");

        insertAttempt(fixture.assessmentId(), candidate.userId(), 80);
        gateService.recompute(candidate.applicationId());
        assertThat(gate(candidate.applicationId())).isEqualTo("passed");
    }

    @Test
    void Fr051_shortlist_is_blocked_until_the_gate_passes() throws Exception {
        String admin = loginAdmin("gate-admin-2@example.com");
        TestEmployer employer = registerEmployer("gate2.hr@acme.com");
        completeCompany(employer.token());
        UUID jobId = createScreeningJob(employer.token());
        Candidate candidate = insertApplicant(jobId, "gate2.candidate@example.com", "Gate", "Two",
                "Dhaka, Bangladesh", new BigDecimal("5"), true);
        Fixture fixture = createAssessment(admin);
        requireAssessment(jobId, fixture.assessmentId(), 70);

        setResult(candidate.applicationId(), "screened", 80, "clear", "pending");
        mockMvc.perform(post("/api/employer/applications/" + candidate.applicationId() + "/shortlist")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Strong evidence from the screening review.\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GATE_NOT_PASSED"));

        setResult(candidate.applicationId(), "screened", 80, "clear", "failed");
        mockMvc.perform(post("/api/employer/applications/" + candidate.applicationId() + "/shortlist")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Strong evidence from the screening review.\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GATE_NOT_PASSED"));

        insertAttempt(fixture.assessmentId(), candidate.userId(), 90);
        gateService.recompute(candidate.applicationId());
        mockMvc.perform(post("/api/employer/applications/" + candidate.applicationId() + "/shortlist")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Strong evidence from the screening review.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gateStatus").value("passed"));
    }

    @Test
    void Fr051_submitting_a_passing_attempt_recomputes_the_gate() throws Exception {
        String admin = loginAdmin("gate-admin-3@example.com");
        TestEmployer employer = registerEmployer("gate3.hr@acme.com");
        completeCompany(employer.token());
        UUID jobId = createScreeningJob(employer.token());
        Candidate candidate = insertApplicant(jobId, "gate3.candidate@example.com", "Gate", "Three",
                "Dhaka, Bangladesh", new BigDecimal("5"), true);
        Fixture fixture = createAssessment(admin);
        requireAssessment(jobId, fixture.assessmentId(), 70);
        gateService.recompute(candidate.applicationId());
        assertThat(gate(candidate.applicationId())).isEqualTo("pending");

        var started = mockMvc.perform(post("/api/assessments/" + fixture.assessmentId() + "/attempts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate.token()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode view = objectMapper.readTree(started.getResponse().getContentAsString());
        String attemptId = view.path("attemptId").asText();
        mockMvc.perform(post("/api/attempts/" + attemptId + "/consent")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"given\":true}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/attempts/" + attemptId + "/events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"type":"hidden","occurredAt":"2026-11-23T10:00:00Z","durationSeconds":40},
                                 {"type":"blur","occurredAt":"2026-11-23T10:02:00Z","durationSeconds":35}]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recorded").value(2));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/attempts/" + attemptId + "/explanation")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"The power went out for a minute.\"}"))
                .andExpect(status().isOk());

        Map<String, String> answers = new LinkedHashMap<>();
        for (int i = 0; i < view.path("questions").size(); i++) {
            answers.put(view.path("questions").get(i).path("id").asText(),
                    fixture.correctOptionIds().get(i));
        }
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/attempts/" + attemptId + "/answers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("answers", answers))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/attempts/" + attemptId + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(true));

        assertThat(gate(candidate.applicationId())).isEqualTo("passed");

        mockMvc.perform(get("/api/employer/applications/" + candidate.applicationId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gateStatus").value("passed"))
                .andExpect(jsonPath("$.assessments.length()").value(1))
                .andExpect(jsonPath("$.assessments[0].minScore").value(70))
                .andExpect(jsonPath("$.assessments[0].bestScore").value(100))
                .andExpect(jsonPath("$.assessments[0].passed").value(true))
                .andExpect(jsonPath("$.assessments[0].focus.recorded").value(true))
                .andExpect(jsonPath("$.assessments[0].focus.leftCount").value(2))
                .andExpect(jsonPath("$.assessments[0].focus.totalSeconds").value(75))
                .andExpect(jsonPath("$.assessments[0].focus.explanation")
                        .value("The power went out for a minute."))
                .andExpect(jsonPath("$.verifiedSkills[0]").value("Java"));
    }

    @Test
    void Fr051_duplicate_copies_required_assessments() throws Exception {
        String admin = loginAdmin("gate-admin-4@example.com");
        TestEmployer employer = registerEmployer("gate4.hr@acme.com");
        completeCompany(employer.token());
        Fixture fixture = createAssessment(admin);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", "Job With Requirement");
        body.put("description", "Build services.");
        body.put("requirements", "");
        body.put("experienceLevel", "senior");
        body.put("city", "Dhaka");
        body.put("country", "Bangladesh");
        body.put("remote", false);
        body.put("requiredSkillIds", List.of());
        body.put("niceToHaveSkillIds", List.of());
        body.put("mustHaveSkillIds", List.of());
        body.put("requiredAssessments", List.of(
                Map.of("assessmentId", fixture.assessmentId().toString(), "minScore", 75)));
        var created = mockMvc.perform(post("/api/employer/jobs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requiredAssessments[0].minScore").value(75))
                .andReturn();
        UUID sourceId = UUID.fromString(objectMapper.readTree(created.getResponse().getContentAsString())
                .path("id").asText());

        mockMvc.perform(post("/api/employer/jobs/" + sourceId + "/duplicate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("draft"))
                .andExpect(jsonPath("$.requiredAssessments.length()").value(1))
                .andExpect(jsonPath("$.requiredAssessments[0].assessmentId")
                        .value(fixture.assessmentId().toString()))
                .andExpect(jsonPath("$.requiredAssessments[0].minScore").value(75));
    }
}
