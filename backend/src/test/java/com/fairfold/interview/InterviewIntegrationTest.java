package com.fairfold.interview;

import com.fairfold.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Part 4.1: scheduling, feedback and hiring (FR-032, FR-033). */
class InterviewIntegrationTest extends AbstractIntegrationTest {

    private record Shortlisted(UUID applicationId, UUID candidateId, String candidateToken) {
    }

    private record Staff(String token, UUID userId) {
    }

    private String login(String email) throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", "password123"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("accessToken").asText();
    }

    private Staff createStaff(UUID companyId, String email, String role) throws Exception {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                        INSERT INTO users (id, email, password_hash, first_name, last_name, role,
                                           company_id, email_verified, consent_at)
                        VALUES (?, ?, ?, 'Staff', 'Member', ?, ?, true, ?)
                        """,
                id.toString(), email, passwordEncoder.encode("password123"), role.toUpperCase(),
                companyId.toString(), new Timestamp(System.currentTimeMillis()));
        return new Staff(login(email), id);
    }

    private Shortlisted shortlist(UUID jobId, String email) throws Exception {
        rateLimitService.reset();
        TestUser candidate = registerUser(email, "candidate");
        UUID resumeId = insertResume(candidate.userId());
        var applied = mockMvc.perform(post("/api/jobs/" + jobId + "/applications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate.token()))
                .andExpect(status().isCreated())
                .andReturn();
        UUID applicationId = UUID.fromString(objectMapper.readTree(applied.getResponse()
                .getContentAsString()).path("id").asText());
        jdbcTemplate.update("""
                        UPDATE applications SET status = 'shortlisted', shortlisted_at = CURRENT_TIMESTAMP(3)
                        WHERE id = ?
                        """, applicationId.toString());
        assertThat(resumeId).isNotNull();
        return new Shortlisted(applicationId, candidate.userId(), candidate.token());
    }

    private void savePack(String token, UUID jobId) throws Exception {
        Map<String, Object> body = Map.of(
                "questions", List.of(
                        Map.of("id", "q1", "text", "Walk through a project."),
                        Map.of("id", "q2", "text", "How do you handle conflict?")),
                "rubric", List.of(
                        Map.of("id", "r1", "criterion", "Technical", "description", "Depth."),
                        Map.of("id", "r2", "criterion", "Communication", "description", "Clarity.")));
        mockMvc.perform(put("/api/employer/jobs/" + jobId + "/interview-pack")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    private UUID schedule(String token, UUID applicationId, UUID interviewerId, Instant when)
            throws Exception {
        Map<String, Object> body = Map.of(
                "scheduledAt", when.toString(),
                "durationMinutes", 45,
                "meetingLink", "https://meet.example.com/interview",
                "interviewerIds", List.of(interviewerId.toString()));
        var result = mockMvc.perform(post("/api/employer/applications/" + applicationId + "/interviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id").asText());
    }

    @Test
    void Fr032_schedule_moves_status_notifies_and_scopes_interviewers() throws Exception {
        TestEmployer employer = registerEmployer("sched.hr@acme.com");
        completeCompany(employer.token());
        UUID jobId = createActiveJob(employer.token());
        Shortlisted candidate = shortlist(jobId, "sched.candidate@example.com");
        Staff interviewer = createStaff(employer.companyId(), "sched.interviewer@acme.com",
                "interviewer");

        UUID interviewId = schedule(employer.token(), candidate.applicationId(),
                interviewer.userId(), Instant.now().plus(2, ChronoUnit.DAYS));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM applications WHERE id = ?", String.class,
                candidate.applicationId().toString())).isEqualTo("interview");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM interviews WHERE id = ?", Long.class,
                interviewId.toString())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM interview_interviewers WHERE interview_id = ?", Long.class,
                interviewId.toString())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM notifications WHERE type = 'interview_scheduled' AND user_id = ?",
                Long.class, candidate.candidateId().toString())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM notifications WHERE type = 'interview_scheduled' AND user_id = ?",
                Long.class, interviewer.userId().toString())).isEqualTo(1);

        mockMvc.perform(get("/api/candidate/applications/" + candidate.applicationId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate.candidateToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interviews.length()").value(1))
                .andExpect(jsonPath("$.interviews[0].durationMinutes").value(45))
                .andExpect(jsonPath("$.interviews[0].meetingLink")
                        .value("https://meet.example.com/interview"));

        mockMvc.perform(get("/api/employer/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interviewsThisWeek").value(1));

        mockMvc.perform(get("/api/interviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + interviewer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].candidateName").value("First Last"));

        Staff otherInterviewer = createStaff(employer.companyId(), "other.interviewer@acme.com",
                "interviewer");
        mockMvc.perform(get("/api/interviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherInterviewer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));

        mockMvc.perform(get("/api/interviews")
                        .param("tab", "past")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));

        Map<String, Object> past = Map.of(
                "scheduledAt", Instant.now().minus(1, ChronoUnit.DAYS).toString(),
                "durationMinutes", 30,
                "meetingLink", "https://meet.example.com/past",
                "interviewerIds", List.of(interviewer.userId().toString()));
        mockMvc.perform(post("/api/employer/applications/" + candidate.applicationId() + "/interviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(past)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SCHEDULE"));

        TestEmployer otherCompany = registerEmployer("other.sched@acme.com");
        Staff foreign = createStaff(otherCompany.companyId(), "foreign@other.com", "interviewer");
        Map<String, Object> foreignBody = Map.of(
                "scheduledAt", Instant.now().plus(1, ChronoUnit.DAYS).toString(),
                "durationMinutes", 30,
                "meetingLink", "https://meet.example.com/foreign",
                "interviewerIds", List.of(foreign.userId().toString()));
        mockMvc.perform(post("/api/employer/applications/" + candidate.applicationId() + "/interviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(foreignBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INTERVIEWER"));
    }

    @Test
    void Fr032_only_shortlisted_or_interview_applications_can_be_scheduled() throws Exception {
        TestEmployer employer = registerEmployer("sched2.hr@acme.com");
        completeCompany(employer.token());
        UUID jobId = createActiveJob(employer.token());
        TestUser candidate = registerUser("applied.candidate@example.com", "candidate");
        insertResume(candidate.userId());
        var applied = mockMvc.perform(post("/api/jobs/" + jobId + "/applications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate.token()))
                .andExpect(status().isCreated())
                .andReturn();
        UUID applicationId = UUID.fromString(objectMapper.readTree(applied.getResponse()
                .getContentAsString()).path("id").asText());
        Staff interviewer = createStaff(employer.companyId(), "sched2.interviewer@acme.com",
                "interviewer");

        Map<String, Object> body = Map.of(
                "scheduledAt", Instant.now().plus(1, ChronoUnit.DAYS).toString(),
                "durationMinutes", 30,
                "meetingLink", "https://meet.example.com/x",
                "interviewerIds", List.of(interviewer.userId().toString()));
        mockMvc.perform(post("/api/employer/applications/" + applicationId + "/interviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS"));
    }

    @Test
    void Fr033_feedback_is_upserted_and_interviewers_see_only_their_own() throws Exception {
        TestEmployer employer = registerEmployer("feedback.hr@acme.com");
        completeCompany(employer.token());
        UUID jobId = createActiveJob(employer.token());
        Shortlisted candidate = shortlist(jobId, "feedback.candidate@example.com");
        savePack(employer.token(), jobId);
        Staff interviewer = createStaff(employer.companyId(), "feedback.interviewer@acme.com",
                "interviewer");
        UUID interviewId = schedule(employer.token(), candidate.applicationId(),
                interviewer.userId(), Instant.now().plus(2, ChronoUnit.DAYS));

        Map<String, Object> first = Map.of(
                "scores", Map.of("r1", 4, "r2", 5),
                "comments", "Strong evidence.",
                "recommendation", "yes");
        mockMvc.perform(put("/api/interviews/" + interviewId + "/feedback")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + interviewer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendation").value("yes"));

        Map<String, Object> second = Map.of(
                "scores", Map.of("r1", 3, "r2", 4),
                "comments", "Updated after the call.",
                "recommendation", "strong_yes");
        mockMvc.perform(put("/api/interviews/" + interviewId + "/feedback")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + interviewer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM interview_feedback WHERE interview_id = ?", Long.class,
                interviewId.toString())).isEqualTo(1);

        mockMvc.perform(get("/api/interviews/" + interviewId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + interviewer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myFeedback.recommendation").value("strong_yes"))
                .andExpect(jsonPath("$.myFeedback.scores.r1").value(3))
                .andExpect(jsonPath("$.allFeedback")
                        .value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.pack.rubric.length()").value(2));

        mockMvc.perform(get("/api/interviews/" + interviewId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allFeedback.length()").value(1))
                .andExpect(jsonPath("$.allFeedback[0].interviewerName").value("Staff Member"))
                .andExpect(jsonPath("$.allFeedback[0].recommendation").value("strong_yes"));

        Map<String, Object> incomplete = Map.of(
                "scores", Map.of("r1", 4),
                "comments", "",
                "recommendation", "yes");
        mockMvc.perform(put("/api/interviews/" + interviewId + "/feedback")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + interviewer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incomplete)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SCORE"));

        mockMvc.perform(put("/api/interviews/" + interviewId + "/feedback")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isForbidden());

        TestEmployer otherCompany = registerEmployer("feedback.other@acme.com");
        mockMvc.perform(get("/api/interviews/" + interviewId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherCompany.token()))
                .andExpect(status().isNotFound());
    }

    @Test
    void Fr033_hire_requires_an_interview_and_reject_now_works_after_shortlist() throws Exception {
        TestEmployer employer = registerEmployer("hire.hr@acme.com");
        completeCompany(employer.token());
        UUID jobId = createActiveJob(employer.token());
        Shortlisted candidate = shortlist(jobId, "hire.candidate@example.com");
        Staff interviewer = createStaff(employer.companyId(), "hire.interviewer@acme.com",
                "interviewer");

        mockMvc.perform(post("/api/employer/applications/" + candidate.applicationId() + "/hire")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS"));

        schedule(employer.token(), candidate.applicationId(), interviewer.userId(),
                Instant.now().plus(1, ChronoUnit.DAYS));
        mockMvc.perform(post("/api/employer/applications/" + candidate.applicationId() + "/hire")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isNoContent());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM applications WHERE id = ?", String.class,
                candidate.applicationId().toString())).isEqualTo("hired");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM notifications WHERE type = 'hired' AND user_id = ?",
                Long.class, candidate.candidateId().toString())).isEqualTo(1);

        mockMvc.perform(post("/api/employer/applications/" + candidate.applicationId() + "/hire")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isConflict());

        Shortlisted rejected = shortlist(jobId, "reject.after.shortlist@example.com");
        mockMvc.perform(post("/api/employer/applications/" + rejected.applicationId() + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isNoContent());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM applications WHERE id = ?", String.class,
                rejected.applicationId().toString())).isEqualTo("rejected");
    }
}
