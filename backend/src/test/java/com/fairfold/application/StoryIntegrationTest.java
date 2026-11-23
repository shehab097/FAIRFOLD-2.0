package com.fairfold.application;

import com.fairfold.ai.AiProviderException;
import com.fairfold.ai.AiResult;
import com.fairfold.ai.DeepSeekProvider;
import com.fairfold.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Part 4.3: AI job story, placeholder restoration and access rules (FR-018). */
@TestPropertySource(properties = {
        "app.ai.provider=deepseek",
        "app.ai.api-key=test-key"
})
class StoryIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private DeepSeekProvider deepSeekProvider;

    @BeforeEach
    void fakeAi() {
        when(deepSeekProvider.name()).thenReturn("deepseek");
        when(deepSeekProvider.available()).thenReturn(true);
        when(deepSeekProvider.writeStory(anyString(), any())).thenReturn(
                new AiResult<>(new AiResult.Story(
                        "At [EMPLOYER_1] I built and maintained backend services."),
                        "deepseek-flash", 60, 25));
    }

    private record StoryFixture(String candidate, UUID candidateId, UUID resumeId,
                                UUID applicationId) {}

    private StoryFixture fixture(String email) throws Exception {
        TestUser candidate = registerUser(email, "candidate");
        UUID resumeId = insertResume(candidate.userId());
        TestEmployer employer = registerEmployer("story.employer." + email);
        completeCompany(employer.token());
        UUID jobId = createActiveJob(employer.token());
        var applied = mockMvc.perform(post("/api/jobs/" + jobId + "/applications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate.token()))
                .andExpect(status().isCreated())
                .andReturn();
        UUID applicationId = UUID.fromString(objectMapper.readTree(
                applied.getResponse().getContentAsString()).path("id").asText());
        jdbcTemplate.update(
                "UPDATE resumes SET status = 'ready', stripped_text = ? WHERE id = ?",
                "Built backend services at [EMPLOYER_1].", resumeId.toString());
        return new StoryFixture(candidate.token(), candidate.userId(), resumeId, applicationId);
    }

    private void placeholder(UUID resumeId) {
        jdbcTemplate.update(
                "INSERT INTO resume_placeholder_map (id, resume_id, placeholder, kind, original_value) "
                        + "VALUES (?, ?, 'EMPLOYER_1', 'employer', 'Acme Ltd')",
                UUID.randomUUID().toString(), resumeId.toString());
    }

    @Test
    void Fr018_generates_story_restores_placeholders_and_stores_nothing() throws Exception {
        StoryFixture fixture = fixture("story.candidate@example.com");
        placeholder(fixture.resumeId());

        mockMvc.perform(post("/api/candidate/applications/" + fixture.applicationId() + "/story")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + fixture.candidate()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.story").value(
                        "At Acme Ltd I built and maintained backend services."))
                .andExpect(jsonPath("$.provider").value("ai"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM ai_calls WHERE purpose = 'story' AND status = 'ok'",
                Long.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_log WHERE action = 'ai_call_blocked'",
                Long.class)).isZero();
    }

    @Test
    void Fr018_is_candidate_owner_only_and_blocks_identifier_leaks() throws Exception {
        StoryFixture fixture = fixture("story.lease@example.com");
        placeholder(fixture.resumeId());
        String other = registerAndGetToken("story.other@example.com", "candidate");

        mockMvc.perform(post("/api/candidate/applications/" + fixture.applicationId() + "/story")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + other))
                .andExpect(status().isNotFound());

        jdbcTemplate.update("UPDATE resumes SET stripped_text = ? WHERE id = ?",
                "First Last\nstory.lease@example.com", fixture.resumeId().toString());
        mockMvc.perform(post("/api/candidate/applications/" + fixture.applicationId() + "/story")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + fixture.candidate()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDENTIFIER_LEAK"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM ai_calls WHERE purpose = 'story' AND status = 'blocked'",
                Long.class)).isEqualTo(1);
    }

    @Test
    void Fr018_returns_503_when_ai_is_unavailable() throws Exception {
        StoryFixture fixture = fixture("story.down@example.com");
        doThrow(new AiProviderException("DeepSeek is down", null))
                .when(deepSeekProvider).writeStory(anyString(), any());

        mockMvc.perform(post("/api/candidate/applications/" + fixture.applicationId() + "/story")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + fixture.candidate()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_UNAVAILABLE"));
    }

    @Test
    void story_requires_a_processed_resume() throws Exception {
        TestUser candidate = registerUser("story.noresume@example.com", "candidate");
        insertResume(candidate.userId());
        TestEmployer employer = registerEmployer("story.noresume.employer@example.com");
        completeCompany(employer.token());
        UUID jobId = createActiveJob(employer.token());
        var applied = mockMvc.perform(post("/api/jobs/" + jobId + "/applications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate.token()))
                .andExpect(status().isCreated())
                .andReturn();
        UUID applicationId = UUID.fromString(objectMapper.readTree(
                applied.getResponse().getContentAsString()).path("id").asText());

        mockMvc.perform(post("/api/candidate/applications/" + applicationId + "/story")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate.token()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESUME_REQUIRED"));
    }
}
