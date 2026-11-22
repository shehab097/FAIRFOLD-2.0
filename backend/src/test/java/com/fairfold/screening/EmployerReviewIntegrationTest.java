package com.fairfold.screening;

import com.fairfold.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EmployerReviewIntegrationTest extends ScreeningTestSupport {

    @Test
    void Fr029_ranked_list_orders_filters_and_hides_not_matched_by_default() throws Exception {
        TestEmployer employer = registerEmployer("list.hr@acme.com");
        completeCompany(employer.token());
        UUID jobId = createScreeningJob(employer.token());
        UUID high = insertApplicant(jobId, "high@example.com", "High", "Score",
                "Dhaka, Bangladesh", new BigDecimal("6"), true).applicationId();
        UUID flagged = insertApplicant(jobId, "flagged@example.com", "Flagged", "Score",
                "Dhaka, Bangladesh", new BigDecimal("6"), true).applicationId();
        UUID notMatched = insertApplicant(jobId, "nomatch@example.com", "No", "Match",
                "Dhaka, Bangladesh", new BigDecimal("6"), false).applicationId();
        UUID applied = insertApplicant(jobId, "applied@example.com", "Just", "Applied",
                "Dhaka, Bangladesh", new BigDecimal("6"), true).applicationId();

        setResult(high, "screened", 90, "clear", "not_required");
        setResult(flagged, "screened", 70, "flagged", "passed");
        setResult(notMatched, "not_matched", null, "clear", "not_required");
        setResult(applied, "applied", null, null, "not_required");
        jdbcTemplate.update("UPDATE applications SET not_matched_rule = 'Must-have skill: Java' WHERE id = ?",
                notMatched.toString());
        jdbcTemplate.update("UPDATE applications SET created_at = DATE_SUB(NOW(3), INTERVAL 1 SECOND) WHERE id = ?",
                high.toString());
        jdbcTemplate.update("UPDATE applications SET created_at = DATE_SUB(NOW(3), INTERVAL 2 SECOND) WHERE id = ?",
                flagged.toString());
        jdbcTemplate.update("UPDATE applications SET created_at = DATE_SUB(NOW(3), INTERVAL 3 SECOND) WHERE id = ?",
                notMatched.toString());
        jdbcTemplate.update("UPDATE applications SET created_at = DATE_SUB(NOW(3), INTERVAL 4 SECOND) WHERE id = ?",
                applied.toString());

        mockMvc.perform(get("/api/employer/jobs/" + jobId + "/applications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].score").value(90))
                .andExpect(jsonPath("$.items[1].score").value(70))
                .andExpect(jsonPath("$.items[2].score").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.items[2].status").value("applied"));

        mockMvc.perform(get("/api/employer/jobs/" + jobId + "/applications")
                        .param("includeNotMatched", "true")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(4))
                .andExpect(jsonPath("$.items[3].notMatchedRule").value("Must-have skill: Java"));

        mockMvc.perform(get("/api/employer/jobs/" + jobId + "/applications")
                        .param("bias", "flagged")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].biasStatus").value("flagged"));

        mockMvc.perform(get("/api/employer/jobs/" + jobId + "/applications")
                        .param("gate", "passed")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].gateStatus").value("passed"));

        mockMvc.perform(get("/api/employer/jobs/" + jobId + "/applications")
                        .param("status", "applied")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].status").value("applied"));

        mockMvc.perform(get("/api/employer/jobs/" + jobId + "/applications")
                        .param("page", "1").param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void Fr030_identity_appears_only_after_shortlist_and_is_audited() throws Exception {
        TestEmployer employer = registerEmployer("identity.hr@acme.com");
        completeCompany(employer.token());
        UUID jobId = createScreeningJob(employer.token());
        Candidate candidate = insertApplicant(jobId, "secret.candidate@example.com",
                "Secret", "Person", "Dhaka, Bangladesh", new BigDecimal("6"), true);
        setResult(candidate.applicationId(), "screened", 80, "clear", "not_required");
        insertClaim(candidate.applicationId());

        MvcResult before = mockMvc.perform(get("/api/employer/applications/" + candidate.applicationId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identity").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.alias").value(org.hamcrest.Matchers.matchesPattern("C-[A-HJ-NP-Z2-9]{5}")))
                .andExpect(jsonPath("$.claims.length()").value(1))
                .andReturn();
        String beforeBody = before.getResponse().getContentAsString();
        assertThat(beforeBody).doesNotContain("Secret Person", "secret.candidate@example.com", "+8801712345678");

        MvcResult after = mockMvc.perform(post("/api/employer/applications/" + candidate.applicationId() + "/shortlist")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identity.firstName").value("Secret"))
                .andExpect(jsonPath("$.identity.lastName").value("Person"))
                .andExpect(jsonPath("$.identity.email").value("secret.candidate@example.com"))
                .andExpect(jsonPath("$.status").value("shortlisted"))
                .andReturn();
        assertThat(after.getResponse().getContentAsString()).contains("+8801712345678");

        Integer reveals = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = 'identity_reveal'", Integer.class);
        assertThat(reveals).isEqualTo(1);
    }

    @Test
    void Fr052_shortlist_below_cutoff_needs_a_reason_and_override_is_audited() throws Exception {
        TestEmployer employer = registerEmployer("below.hr@acme.com");
        completeCompany(employer.token());
        UUID jobId = createScreeningJob(employer.token());
        Candidate candidate = insertApplicant(jobId, "below@example.com", "Below", "Cutoff",
                "Dhaka, Bangladesh", new BigDecimal("6"), true);
        setResult(candidate.applicationId(), "screened", 40, "clear", "not_required");

        mockMvc.perform(post("/api/employer/applications/" + candidate.applicationId() + "/shortlist")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token()))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("REASON_REQUIRED"));

        mockMvc.perform(post("/api/employer/applications/" + candidate.applicationId() + "/shortlist")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"short\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("REASON_REQUIRED"));

        mockMvc.perform(post("/api/employer/applications/" + candidate.applicationId() + "/shortlist")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Strong portfolio and relevant systems work.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identity.email").value("below@example.com"));

        String direction = jdbcTemplate.queryForObject(
                "SELECT override_direction FROM applications WHERE id = ?", String.class,
                candidate.applicationId().toString());
        assertThat(direction).isEqualTo("shortlisted_below_cutoff");
        Integer aiDecisions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = 'ai_decision'", Integer.class);
        assertThat(aiDecisions).isEqualTo(1);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE applications SET decision_reason = 'short', "
                        + "override_direction = 'shortlisted_below_cutoff' WHERE id = ?",
                candidate.applicationId().toString()))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void Fr052_reject_above_cutoff_needs_a_reason_and_override_is_audited() throws Exception {
        TestEmployer employer = registerEmployer("above.hr@acme.com");
        completeCompany(employer.token());
        UUID jobId = createScreeningJob(employer.token());
        Candidate candidate = insertApplicant(jobId, "above@example.com", "Above", "Cutoff",
                "Dhaka, Bangladesh", new BigDecimal("6"), true);
        setResult(candidate.applicationId(), "screened", 80, "clear", "not_required");

        mockMvc.perform(post("/api/employer/applications/" + candidate.applicationId() + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token()))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("REASON_REQUIRED"));

        mockMvc.perform(post("/api/employer/applications/" + candidate.applicationId() + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"We need a stronger distributed systems background.\"}"))
                .andExpect(status().isNoContent());

        String direction = jdbcTemplate.queryForObject(
                "SELECT override_direction FROM applications WHERE id = ?", String.class,
                candidate.applicationId().toString());
        String statusValue = jdbcTemplate.queryForObject(
                "SELECT status FROM applications WHERE id = ?", String.class,
                candidate.applicationId().toString());
        assertThat(direction).isEqualTo("rejected_above_cutoff");
        assertThat(statusValue).isEqualTo("rejected");
        Integer aiDecisions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = 'ai_decision'", Integer.class);
        assertThat(aiDecisions).isEqualTo(1);
    }

    @Test
    void gate_pending_or_failed_blocks_shortlist() throws Exception {
        TestEmployer employer = registerEmployer("gate.hr@acme.com");
        completeCompany(employer.token());
        UUID jobId = createScreeningJob(employer.token());
        Candidate failed = insertApplicant(jobId, "gate.failed@example.com", "Gate", "Failed",
                "Dhaka, Bangladesh", new BigDecimal("6"), true);
        Candidate pending = insertApplicant(jobId, "gate.pending@example.com", "Gate", "Pending",
                "Dhaka, Bangladesh", new BigDecimal("6"), true);
        setResult(failed.applicationId(), "screened", 80, "clear", "failed");
        setResult(pending.applicationId(), "screened", 80, "clear", "pending");

        for (Candidate candidate : new Candidate[]{failed, pending}) {
            mockMvc.perform(post("/api/employer/applications/" + candidate.applicationId() + "/shortlist")
                            .header(HttpHeaders.AUTHORIZATION, bearer(employer.token()))
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("GATE_NOT_PASSED"));
        }
    }

    @Test
    void pull_back_requeues_a_not_matched_candidate_and_skips_filters() throws Exception {
        TestEmployer employer = registerEmployer("pull.hr@acme.com");
        completeCompany(employer.token());
        UUID jobId = createScreeningJob(employer.token());
        Candidate candidate = insertApplicant(jobId, "pull@example.com", "Pull", "Back",
                "Dhaka, Bangladesh", new BigDecimal("6"), false);
        setResult(candidate.applicationId(), "not_matched", null, "clear", "not_required");
        jdbcTemplate.update("UPDATE applications SET not_matched_rule = 'Must-have skill: Java' WHERE id = ?",
                candidate.applicationId().toString());

        MvcResult result = mockMvc.perform(post("/api/employer/applications/" + candidate.applicationId() + "/pull-back")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isAccepted())
                .andReturn();
        UUID runId = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .path("runId").asText());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM applications WHERE id = ?", String.class,
                candidate.applicationId().toString())).isIn("screening", "screened");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT pulled_back_at IS NOT NULL FROM applications WHERE id = ?", Boolean.class,
                candidate.applicationId().toString())).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT skip_filters FROM screening_items WHERE run_id = ?", Boolean.class, runId.toString()))
                .isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = 'pull_back'", Integer.class)).isEqualTo(1);

        for (int i = 0; i < 200; i++) {
            String statusValue = jdbcTemplate.queryForObject(
                    "SELECT status FROM screening_runs WHERE id = ?", String.class, runId.toString());
            if ("finished".equals(statusValue)) {
                break;
            }
            Thread.sleep(50);
        }
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM applications WHERE id = ?", String.class,
                candidate.applicationId().toString())).isEqualTo("screened");
    }

    @Test
    void Fr024_candidate_sees_score_and_claims_but_not_bias_or_stripped_text() throws Exception {
        TestEmployer employer = registerEmployer("candidateview.hr@acme.com");
        completeCompany(employer.token());
        UUID jobId = createScreeningJob(employer.token());
        Candidate candidate = insertApplicant(jobId, "candview@example.com", "Candidate", "View",
                "Dhaka, Bangladesh", new BigDecimal("6"), true);
        setResult(candidate.applicationId(), "screened", 55, "flagged", "not_required");
        insertClaim(candidate.applicationId());

        MvcResult result = mockMvc.perform(get("/api/candidate/applications/" + candidate.applicationId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(55))
                .andExpect(jsonPath("$.scoringMode").value("rule_based"))
                .andExpect(jsonPath("$.claims.length()").value(1))
                .andExpect(jsonPath("$.claims[0].requirement").value("Java"))
                .andExpect(jsonPath("$.claims[0].verdict").value("met"))
                .andExpect(jsonPath("$.gateStatus").value("not_required"))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("biasStatus", "biasNote", "strippedText", "flagged");
    }

    @Test
    void Fr046_employer_dashboard_counts() throws Exception {
        TestEmployer employer = registerEmployer("dash.hr@acme.com");
        completeCompany(employer.token());
        UUID jobId = createScreeningJob(employer.token());
        insertApplicant(jobId, "dash.applied@example.com", "Dash", "Applied",
                "Dhaka, Bangladesh", new BigDecimal("6"), true);
        Candidate flagged = insertApplicant(jobId, "dash.flagged@example.com", "Dash", "Flagged",
                "Dhaka, Bangladesh", new BigDecimal("6"), true);
        Candidate old = insertApplicant(jobId, "dash.old@example.com", "Dash", "Old",
                "Dhaka, Bangladesh", new BigDecimal("6"), true);
        setResult(flagged.applicationId(), "screened", 70, "flagged", "not_required");
        setResult(old.applicationId(), "screened", 60, "clear", "not_required");
        jdbcTemplate.update("UPDATE applications SET created_at = DATE_SUB(NOW(3), INTERVAL 10 DAY) WHERE id = ?",
                old.applicationId().toString());

        mockMvc.perform(get("/api/employer/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openJobs").value(1))
                .andExpect(jsonPath("$.newApplications").value(2))
                .andExpect(jsonPath("$.awaitingScreening").value(1))
                .andExpect(jsonPath("$.biasFlags").value(1))
                .andExpect(jsonPath("$.interviewsThisWeek").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.quota").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void screening_questions_can_be_suggested_and_saved_on_a_draft() throws Exception {
        TestEmployer employer = registerEmployer("questions.hr@acme.com");
        completeCompany(employer.token());
        UUID jobId = createScreeningJob(employer.token(), false);

        MvcResult suggested = mockMvc.perform(post("/api/employer/jobs/" + jobId + "/suggest-questions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions").isNotEmpty())
                .andReturn();
        JsonNode questions = objectMapper.readTree(suggested.getResponse().getContentAsString())
                .path("questions");

        String body = "{\"questions\":[\"How do you design resilient services?\",\"Tell us about a migration.\"]}";
        mockMvc.perform(put("/api/employer/jobs/" + jobId + "/screening-questions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token()))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(2));

        mockMvc.perform(get("/api/employer/jobs/" + jobId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.screeningQuestions.length()").value(2))
                .andExpect(jsonPath("$.screeningQuestions[0]").value("How do you design resilient services?"));

        mockMvc.perform(post("/api/employer/jobs/" + jobId + "/activate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/employer/jobs/" + jobId + "/screening-questions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token()))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("JOB_NOT_DRAFT"));
        assertThat(questions.size()).isGreaterThanOrEqualTo(2);
    }

    private void insertClaim(UUID applicationId) {
        jdbcTemplate.update("""
                        INSERT INTO application_claims
                            (id, application_id, requirement_key, requirement_text, kind, verdict, quote,
                             weight, source)
                        VALUES (?, ?, 'skill:java', 'Java', 'required', 'met',
                                'Built Java services for payments.', 3, 'rule_based')
                        """,
                UUID.randomUUID().toString(), applicationId.toString());
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
