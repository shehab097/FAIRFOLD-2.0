package com.fairfold.screening;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ScreeningRetryTest extends ScreeningTestSupport {

    @MockitoBean
    private ScreeningWorker worker;

    @Test
    void Fr028_retry_requeues_only_failed_items() throws Exception {
        TestEmployer employer = registerEmployer("retry.hr@acme.com");
        completeCompany(employer.token());
        UUID jobId = createScreeningJob(employer.token());
        Candidate first = insertApplicant(jobId, "retry1@example.com", "Retry", "One",
                "Dhaka, Bangladesh", new BigDecimal("6"), true);
        Candidate second = insertApplicant(jobId, "retry2@example.com", "Retry", "Two",
                "Dhaka, Bangladesh", new BigDecimal("6"), true);

        UUID runId = UUID.randomUUID();
        jdbcTemplate.update("""
                        INSERT INTO screening_runs
                            (id, job_id, mode, total, done_count, failed_count, status, finished_at)
                        VALUES (?, ?, 'rule_based', 2, 1, 1, 'finished', CURRENT_TIMESTAMP(3))
                        """,
                runId.toString(), jobId.toString());
        UUID doneItem = UUID.randomUUID();
        UUID failedItem = UUID.randomUUID();
        jdbcTemplate.update("""
                        INSERT INTO screening_items (id, run_id, application_id, status, attempts)
                        VALUES (?, ?, ?, 'done', 1), (?, ?, ?, 'failed', 2)
                        """,
                doneItem.toString(), runId.toString(), first.applicationId().toString(),
                failedItem.toString(), runId.toString(), second.applicationId().toString());

        mockMvc.perform(post("/api/employer/jobs/" + jobId + "/screening/retry")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requeued").value(1));

        assertThat(itemStatus(doneItem)).isEqualTo("done");
        assertThat(itemStatus(failedItem)).isEqualTo("queued");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT failed_count FROM screening_runs WHERE id = ?", Integer.class, runId.toString()))
                .isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM screening_runs WHERE id = ?", String.class, runId.toString()))
                .isEqualTo("running");

        mockMvc.perform(post("/api/employer/jobs/" + jobId + "/screening/retry")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NOTHING_TO_RETRY"));
    }

    private String itemStatus(UUID id) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM screening_items WHERE id = ?", String.class, id.toString());
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
