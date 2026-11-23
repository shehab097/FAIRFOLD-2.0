package com.fairfold.coaching;

import com.fairfold.ai.AiProviderException;
import com.fairfold.ai.AiResult;
import com.fairfold.ai.DeepSeekProvider;
import com.fairfold.ai.StarFeedback;
import com.fairfold.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Part 4.2: private interview practice and STAR feedback (FR-058/059/062). */
@TestPropertySource(properties = {
        "app.ai.provider=deepseek",
        "app.ai.api-key=test-key"
})
class PracticeIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private DeepSeekProvider deepSeekProvider;

    @BeforeEach
    void fakeAi() {
        when(deepSeekProvider.name()).thenReturn("deepseek");
        when(deepSeekProvider.available()).thenReturn(true);
        when(deepSeekProvider.gradeStar(anyString(), anyString())).thenReturn(
                new AiResult<>(new StarFeedback(
                        new StarFeedback.Component(9, java.util.List.of(
                                new StarFeedback.Point("Strong context", "[EMPLOYER_1]"),
                                new StarFeedback.Point("Invented evidence", "I led fifty engineers"))),
                        new StarFeedback.Component(0, java.util.List.of()),
                        new StarFeedback.Component(4, java.util.List.of(
                                new StarFeedback.Point("Clear actions", "worked on a migration"))),
                        new StarFeedback.Component(7, java.util.List.of(
                                new StarFeedback.Point("Measured result", "cut latency by 40 percent"))),
                        "Good STAR structure."),
                        "deepseek-flash", 120, 80));
    }

    private UUID createQuestion(String adminToken) throws Exception {
        var result = mockMvc.perform(post("/api/admin/practice-questions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Tell us about a difficult migration.\",\"topic\":\"Behavioral\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id").asText());
    }

    @Test
    void Fr058_Fr059_Fr062_practice_anonymises_validates_quotes_restores_and_rates() throws Exception {
        String admin = loginAdmin("practice-admin@example.com");
        UUID questionId = createQuestion(admin);
        String candidate = registerAndGetToken("practice.candidate@example.com", "candidate");

        mockMvc.perform(get("/api/practice/questions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].topic").value("Behavioral"));

        String answer = "At Acme Ltd I worked with First on a migration that cut latency by 40 percent.";
        var created = mockMvc.perform(post("/api/practice/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionId", questionId.toString(), "answer", answer))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.feedbackAvailable").value(true))
                .andExpect(jsonPath("$.rubricVersion").value("star-v1"))
                .andExpect(jsonPath("$.feedback.situation.rating").value(5))
                .andExpect(jsonPath("$.feedback.task.rating").value(1))
                .andExpect(jsonPath("$.feedback.action.rating").value(4))
                .andExpect(jsonPath("$.feedback.result.rating").value(5))
                .andExpect(jsonPath("$.feedback.situation.points.length()").value(1))
                .andExpect(jsonPath("$.feedback.situation.points[0].quote").value("At Acme Ltd"))
                .andReturn();
        JsonNode session = objectMapper.readTree(created.getResponse().getContentAsString());
        String sessionId = session.path("id").asText();

        ArgumentCaptor<String> questionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> answerCaptor = ArgumentCaptor.forClass(String.class);
        verify(deepSeekProvider).gradeStar(questionCaptor.capture(), answerCaptor.capture());
        String anonymised = answerCaptor.getValue();
        assertThat(anonymised).doesNotContain("First", "Acme Ltd");
        assertThat(anonymised).contains("[EMPLOYER_1]");
        assertThat(anonymised).contains("cut latency by 40 percent");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM ai_calls WHERE purpose = 'practice_feedback' AND status = 'ok'",
                Long.class)).isEqualTo(1);

        mockMvc.perform(post("/api/practice/sessions/" + sessionId + "/rating")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5));

        mockMvc.perform(post("/api/practice/sessions/" + sessionId + "/rating")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":6}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/practice/sessions?page=0")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].feedbackAvailable").value(true))
                .andExpect(jsonPath("$.items[0].rating").value(5));

        mockMvc.perform(get("/api/practice/sessions/" + sessionId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(answer));

        String other = registerAndGetToken("practice.other@example.com", "candidate");
        mockMvc.perform(get("/api/practice/sessions/" + sessionId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + other))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/admin/practice-questions/" + questionId + "/active")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/practice/questions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(post("/api/practice/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionId", questionId.toString(),
                                "answer", "A sufficiently long answer that should fail because inactive."))))
                .andExpect(status().isNotFound());
    }

    @Test
    void Fr059_unavailable_ai_saves_the_answer_without_feedback() throws Exception {
        String admin = loginAdmin("practice-admin-2@example.com");
        UUID questionId = createQuestion(admin);
        String candidate = registerAndGetToken("practice.candidate-2@example.com", "candidate");
        doThrow(new AiProviderException("down", null))
                .when(deepSeekProvider).gradeStar(anyString(), anyString());

        mockMvc.perform(post("/api/practice/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionId", questionId.toString(),
                                "answer", "Situation. Task. Action. Result with a measured outcome here."))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.feedbackAvailable").value(false))
                .andExpect(jsonPath("$.feedback").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.provider").value(org.hamcrest.Matchers.nullValue()));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM ai_calls WHERE purpose = 'practice_feedback' AND status = 'error'",
                Long.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM practice_sessions WHERE feedback IS NULL", Long.class)).isEqualTo(1);
    }

    @Test
    void Fr058_admin_can_add_edit_and_deactivate_practice_questions() throws Exception {
        String admin = loginAdmin("practice-admin-3@example.com");
        UUID id = createQuestion(admin);

        mockMvc.perform(put("/api/admin/practice-questions/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Updated question text.\",\"topic\":\"Leadership\",\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Updated question text."))
                .andExpect(jsonPath("$.topic").value("Leadership"));

        mockMvc.perform(patch("/api/admin/practice-questions/" + id + "/active")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/admin/practice-questions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].active").value(false));
    }
}
