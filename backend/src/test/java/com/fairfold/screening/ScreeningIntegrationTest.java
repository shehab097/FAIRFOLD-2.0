package com.fairfold.screening;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ScreeningIntegrationTest extends ScreeningTestSupport {

    @Autowired
    private AliasService aliasService;

    @Autowired
    private ScreeningRecovery recovery;

    @Test
    void Fr028_estimate_start_progress_and_results_with_fallback() throws Exception {
        TestEmployer employer = registerEmployer("screen.hr@acme.com");
        completeCompany(employer.token());
        UUID jobId = createScreeningJob(employer.token());

        for (int i = 0; i < 9; i++) {
            insertApplicant(jobId, "cand" + i + "@example.com", "Candidate" + i, "Leak" + i,
                    "Dhaka, Bangladesh", new BigDecimal("6"), true);
        }
        Candidate notMatched = insertApplicant(jobId, "nocand@example.com", "No", "Java",
                "Dhaka, Bangladesh", new BigDecimal("6"), false);

        mockMvc.perform(post("/api/employer/jobs/" + jobId + "/screening/estimate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(10))
                .andExpect(jsonPath("$.mode").value("rule_based"))
                .andExpect(jsonPath("$.estimatedCostUsd").value(0));

        JsonNode progress = startAndAwait(employer.token(), jobId);
        assertThat(progress.path("total").asInt()).isEqualTo(10);
        assertThat(progress.path("done").asInt()).isEqualTo(10);
        assertThat(progress.path("failed").asInt()).isZero();
        assertThat(progress.path("mode").asText()).isEqualTo("rule_based");

        Integer screened = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM applications WHERE job_id = ? AND status = 'screened'",
                Integer.class, jobId.toString());
        Integer notMatchedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM applications WHERE job_id = ? AND status = 'not_matched'",
                Integer.class, jobId.toString());
        assertThat(screened).isEqualTo(9);
        assertThat(notMatchedCount).isEqualTo(1);

        Integer missingScores = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM applications WHERE job_id = ? AND status = 'screened' AND score IS NULL",
                Integer.class, jobId.toString());
        assertThat(missingScores).isZero();

        Integer wrongMode = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM applications WHERE job_id = ? AND status = 'screened' "
                        + "AND scoring_mode <> 'rule_based'",
                Integer.class, jobId.toString());
        assertThat(wrongMode).isZero();

        Integer claims = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM application_claims WHERE application_id = ?",
                Integer.class, notMatched.applicationId().toString());
        assertThat(claims).isZero();

        String rule = jdbcTemplate.queryForObject(
                "SELECT not_matched_rule FROM applications WHERE id = ?",
                String.class, notMatched.applicationId().toString());
        assertThat(rule).contains("Must-have skill: Java");

        Integer claimsForScreened = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM application_claims ac JOIN applications a ON a.id = ac.application_id "
                        + "WHERE a.job_id = ? AND a.status = 'screened'",
                Integer.class, jobId.toString());
        assertThat(claimsForScreened).isGreaterThan(0);

        MvcResult applicants = mockMvc.perform(get("/api/employer/jobs/" + jobId + "/applications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].alias").exists())
                .andExpect(jsonPath("$.items[0].label").exists())
                .andReturn();
        String body = applicants.getResponse().getContentAsString();
        assertThat(body).contains("C-").doesNotContain("Candidate", "Leak", "@example.com");
    }

    @Test
    void Fr028_only_one_run_at_a_time() throws Exception {
        TestEmployer employer = registerEmployer("running.hr@acme.com");
        completeCompany(employer.token());
        UUID jobId = createScreeningJob(employer.token());
        UUID runId = UUID.randomUUID();
        jdbcTemplate.update("""
                        INSERT INTO screening_runs (id, job_id, mode, total, status)
                        VALUES (?, ?, 'rule_based', 0, 'running')
                        """,
                runId.toString(), jobId.toString());

        mockMvc.perform(post("/api/employer/jobs/" + jobId + "/screening")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SCREENING_RUNNING"));
    }

    @Test
    void Fr054_aliases_are_stable_per_company_and_different_across_companies() throws Exception {
        TestEmployer first = registerEmployer("alias1.hr@acme.com");
        TestEmployer second = registerEmployer("alias2.hr@other.com");
        TestUser candidate = registerUser("alias.candidate@example.com", "candidate");

        String a1 = aliasService.aliasFor(first.companyId(), candidate.userId());
        String a2 = aliasService.aliasFor(first.companyId(), candidate.userId());
        String b1 = aliasService.aliasFor(second.companyId(), candidate.userId());

        assertThat(a1).matches("C-[A-HJ-NP-Z2-9]{5}");
        assertThat(a2).isEqualTo(a1);
        assertThat(b1).isNotEqualTo(a1);
    }

    @Test
    void screening_restart_recovery_processes_running_items() throws Exception {
        TestEmployer employer = registerEmployer("recover.hr@acme.com");
        completeCompany(employer.token());
        UUID jobId = createScreeningJob(employer.token());
        Candidate candidate = insertApplicant(jobId, "recover.candidate@example.com",
                "Recover", "Candidate", "Dhaka, Bangladesh", new BigDecimal("6"), true);

        UUID runId = UUID.randomUUID();
        jdbcTemplate.update("""
                        INSERT INTO screening_runs (id, job_id, mode, total, status)
                        VALUES (?, ?, 'rule_based', 1, 'running')
                        """,
                runId.toString(), jobId.toString());
        jdbcTemplate.update("""
                        INSERT INTO screening_items (id, run_id, application_id, status, attempts)
                        VALUES (?, ?, ?, 'running', 1)
                        """,
                UUID.randomUUID().toString(), runId.toString(), candidate.applicationId().toString());
        jdbcTemplate.update("UPDATE applications SET status = 'screening' WHERE id = ?",
                candidate.applicationId().toString());

        recovery.recover();

        for (int i = 0; i < 200; i++) {
            String status = jdbcTemplate.queryForObject(
                    "SELECT status FROM screening_runs WHERE id = ?", String.class, runId.toString());
            if ("finished".equals(status)) {
                break;
            }
            Thread.sleep(50);
        }
        String runStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM screening_runs WHERE id = ?", String.class, runId.toString());
        String applicationStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM applications WHERE id = ?", String.class,
                candidate.applicationId().toString());
        assertThat(runStatus).isEqualTo("finished");
        assertThat(applicationStatus).isEqualTo("screened");
    }

    @Test
    void progress_is_empty_before_the_first_run() throws Exception {
        TestEmployer employer = registerEmployer("norun.hr@acme.com");
        completeCompany(employer.token());
        UUID jobId = createScreeningJob(employer.token());
        mockMvc.perform(get("/api/employer/jobs/" + jobId + "/screening")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void estimate_and_start_use_the_current_ready_resume_for_legacy_applications() throws Exception {
        TestEmployer employer = registerEmployer("legacy.hr@acme.com");
        completeCompany(employer.token());
        UUID jobId = createScreeningJob(employer.token());
        TestUser candidate = registerUser("legacy.candidate@example.com", "candidate");
        jdbcTemplate.update("""
                        INSERT INTO candidate_profiles
                            (user_id, phone, title, location, remote_preference, total_experience_years,
                             education_level, previous_job_titles)
                        VALUES (?, '+8801712345678', 'Backend Engineer', 'Dhaka, Bangladesh', 'any', 6,
                                'bachelor', JSON_ARRAY())
                        """,
                candidate.userId().toString());
        UUID oldResume = UUID.randomUUID();
        jdbcTemplate.update("""
                        INSERT INTO resumes (id, user_id, original_filename, storage_key, size_bytes,
                                             status, is_current, current_user_key)
                        VALUES (?, ?, 'old.pdf', ?, 1024, 'uploaded', false, NULL)
                        """,
                oldResume.toString(), candidate.userId().toString(), UUID.randomUUID().toString());
        UUID currentResume = UUID.randomUUID();
        jdbcTemplate.update("""
                        INSERT INTO resumes (id, user_id, original_filename, storage_key, size_bytes,
                                             status, stripped_text, is_current, current_user_key)
                        VALUES (?, ?, 'new.pdf', ?, 1024, 'ready',
                                'Built Java and Spring Boot services with PostgreSQL and Docker.', true, ?)
                        """,
                currentResume.toString(), candidate.userId().toString(), UUID.randomUUID().toString(),
                candidate.userId().toString());
        UUID applicationId = UUID.randomUUID();
        jdbcTemplate.update("""
                        INSERT INTO applications (id, job_id, candidate_id, resume_id, status)
                        VALUES (?, ?, ?, ?, 'applied')
                        """,
                applicationId.toString(), jobId.toString(), candidate.userId().toString(),
                oldResume.toString());

        mockMvc.perform(post("/api/employer/jobs/" + jobId + "/screening/estimate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        startAndAwait(employer.token(), jobId);

        String linkedResume = jdbcTemplate.queryForObject(
                "SELECT resume_id FROM applications WHERE id = ?", String.class, applicationId.toString());
        assertThat(linkedResume).isEqualTo(currentResume.toString());
        String applicationStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM applications WHERE id = ?", String.class, applicationId.toString());
        assertThat(applicationStatus).isEqualTo("screened");
    }

    @Test
    void estimate_and_start_reject_another_company_job() throws Exception {
        TestEmployer owner = registerEmployer("owner.hr@acme.com");
        completeCompany(owner.token());
        UUID jobId = createScreeningJob(owner.token());
        TestEmployer other = registerEmployer("other.hr@other.com");
        completeCompany(other.token());

        mockMvc.perform(post("/api/employer/jobs/" + jobId + "/screening/estimate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(other.token())))
                .andExpect(status().isNotFound());
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
