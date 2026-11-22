package com.fairfold.screening;

import com.fairfold.ai.AiResult;
import com.fairfold.ai.DeepSeekProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "app.ai.provider=deepseek",
        "app.ai.api-key=test-key"
})
class ScreeningAiQuoteTest extends ScreeningTestSupport {

    @MockitoBean
    private DeepSeekProvider deepSeekProvider;

    @BeforeEach
    void fakeAi() {
        when(deepSeekProvider.name()).thenReturn("deepseek");
        when(deepSeekProvider.available()).thenReturn(true);
        when(deepSeekProvider.screen(any())).thenAnswer(invocation -> {
            AiResult.ScreeningRequest request = invocation.getArgument(0);
            List<AiResult.Verdict> verdicts = new ArrayList<>();
            for (AiResult.Requirement requirement : request.requirements()) {
                verdicts.add(new AiResult.Verdict(
                        requirement.key(),
                        "met",
                        "This evidence was invented by the model and is absent from the resume text."));
            }
            return new AiResult<>(verdicts, "deepseek-flash", 100, 50);
        });
    }

    @Test
    void Fr028_invented_ai_quote_is_downgraded_and_recorded() throws Exception {
        TestEmployer employer = registerEmployer("ai.hr@acme.com");
        completeCompany(employer.token());
        UUID jobId = createScreeningJob(employer.token());
        Candidate candidate = insertApplicant(jobId, "ai.candidate@example.com",
                "Ai", "Candidate", "Dhaka, Bangladesh", new BigDecimal("6"), true);

        mockMvc.perform(post("/api/employer/jobs/" + jobId + "/screening/estimate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("ai"));

        startAndAwait(employer.token(), jobId);

        Integer score = jdbcTemplate.queryForObject(
                "SELECT score FROM applications WHERE id = ?", Integer.class,
                candidate.applicationId().toString());
        String mode = jdbcTemplate.queryForObject(
                "SELECT scoring_mode FROM applications WHERE id = ?", String.class,
                candidate.applicationId().toString());
        Integer quoted = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM application_claims WHERE application_id = ? AND quote IS NOT NULL",
                Integer.class, candidate.applicationId().toString());
        Integer claims = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM application_claims WHERE application_id = ? AND verdict <> 'not_met'",
                Integer.class, candidate.applicationId().toString());
        assertThat(score).isZero();
        assertThat(mode).isEqualTo("ai");
        assertThat(quoted).isZero();
        assertThat(claims).isZero();

        Integer calls = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_calls WHERE purpose = 'screening' AND status = 'ok' "
                        + "AND model = 'deepseek-flash'", Integer.class);
        assertThat(calls).isEqualTo(1);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
