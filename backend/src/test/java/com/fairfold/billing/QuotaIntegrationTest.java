package com.fairfold.billing;

import com.fairfold.ai.DeepSeekProvider;
import com.fairfold.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Part 5.2: AI screening quota enforcement and monthly counting. */
@TestPropertySource(properties = {
        "app.ai.provider=deepseek",
        "app.ai.api-key=test-key"
})
class QuotaIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private DeepSeekProvider deepSeekProvider;

    @Autowired
    private QuotaService quotaService;

    private UUID applicationFor(TestUser candidate, UUID jobId) throws Exception {
        UUID resumeId = insertResume(candidate.userId());
        jdbcTemplate.update("UPDATE resumes SET status = 'ready', stripped_text = ? WHERE id = ?",
                "Built services with Java.", resumeId.toString());
        var result = mockMvc.perform(post("/api/jobs/" + jobId + "/applications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate.token()))
                .andExpect(status().isCreated()).andReturn();
        return UUID.fromString(objectMapper.readTree(
                result.getResponse().getContentAsString()).path("id").asText());
    }

    private void doneItem(UUID jobId, UUID applicationId, Instant at) {
        UUID runId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO screening_runs (id, job_id, mode, total, done_count, status) "
                        + "VALUES (?, ?, 'ai', 1, 1, 'finished')",
                runId.toString(), jobId.toString());
        jdbcTemplate.update("INSERT INTO screening_items (id, run_id, application_id, status, updated_at) "
                        + "VALUES (?, ?, ?, 'done', ?)", UUID.randomUUID().toString(),
                runId.toString(), applicationId.toString(), Timestamp.from(at));
    }

    @Test
    void quota_exceeded_returns_used_and_quota() throws Exception {
        when(deepSeekProvider.name()).thenReturn("deepseek");
        when(deepSeekProvider.available()).thenReturn(true);

        TestEmployer employer = registerEmployer("quota.hr@example.com");
        completeCompany(employer.token());
        UUID jobId = createActiveJob(employer.token());
        TestUser first = registerUser("quota.first@example.com", "candidate");
        UUID applicationId = applicationFor(first, jobId);

        for (int i = 0; i < 20; i++) {
            doneItem(jobId, applicationId, Instant.now());
        }
        TestUser fresh = registerUser("quota.fresh@example.com", "candidate");
        applicationFor(fresh, jobId);

        mockMvc.perform(post("/api/employer/jobs/" + jobId + "/screening")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("QUOTA_EXCEEDED"))
                .andExpect(jsonPath("$.details.used").value(20))
                .andExpect(jsonPath("$.details.quota").value(20));
        assertThat(quotaService.usedThisMonth(employer.companyId())).isEqualTo(20);
    }

    @Test
    void previous_month_items_do_not_count() throws Exception {
        TestEmployer employer = registerEmployer("quota.reset@example.com");
        completeCompany(employer.token());
        UUID jobId = createActiveJob(employer.token());
        TestUser candidate = registerUser("quota.old@example.com", "candidate");
        UUID applicationId = applicationFor(candidate, jobId);

        Instant lastMonth = YearMonth.now().minusMonths(1).atDay(15)
                .atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        doneItem(jobId, applicationId, lastMonth);
        assertThat(quotaService.usedThisMonth(employer.companyId())).isZero();
    }
}
