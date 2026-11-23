package com.fairfold.candidate;

import com.fairfold.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Part 4.3: career timeline and skill evolution (FR-016/FR-017). */
class JourneyIntegrationTest extends AbstractIntegrationTest {

    private void addSkill(String token, String name, String level) throws Exception {
        mockMvc.perform(post("/api/candidate/skills")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "skillId", skillId(name).toString(), "level", level))))
                .andExpect(status().isCreated());
    }

    private void addWork(String token, String title, String employer, String start,
                         String end, boolean current, String... skills) throws Exception {
        mockMvc.perform(post("/api/candidate/work-history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", title, "employer", employer,
                                "startMonth", start,
                                "endMonth", end == null ? "" : end,
                                "isCurrent", current,
                                "skillsUsed", java.util.List.of(skills)))))
                .andExpect(status().isCreated());
    }

    @Test
    void Fr016_Fr017_timeline_order_skill_use_and_aliases() throws Exception {
        String candidate = registerAndGetToken("journey.candidate@example.com", "candidate");
        addSkill(candidate, "Java", "expert");
        addSkill(candidate, "Python", "beginner");

        addWork(candidate, "Backend Engineer", "Acme Ltd", "2019-01", "2021-06",
                false, "Java", "React.js");
        addWork(candidate, "Lead Engineer", "Beta Corp", "2022-03", null,
                true, "Java", "Kubernetes");

        String currentMonth = YearMonth.now().toString();
        var result = mockMvc.perform(get("/api/candidate/journey")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeline.length()").value(2))
                .andExpect(jsonPath("$.timeline[0].title").value("Backend Engineer"))
                .andExpect(jsonPath("$.timeline[0].employer").value("Acme Ltd"))
                .andExpect(jsonPath("$.timeline[0].startMonth").value("2019-01"))
                .andExpect(jsonPath("$.timeline[0].isCurrent").value(false))
                .andExpect(jsonPath("$.timeline[1].title").value("Lead Engineer"))
                .andExpect(jsonPath("$.timeline[1].isCurrent").value(true))
                .andReturn();

        JsonNode skills = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("skills");
        Map<String, JsonNode> byName = new java.util.LinkedHashMap<>();
        for (JsonNode skill : skills) {
            byName.put(skill.path("name").asText(), skill);
        }

        JsonNode java = byName.get("Java");
        assertThat(java).isNotNull();
        assertThat(java.path("firstUsedMonth").asText()).isEqualTo("2019-01");
        assertThat(java.path("lastUsedMonth").asText()).isEqualTo(currentMonth);
        assertThat(java.path("level").asText()).isEqualTo("expert");
        assertThat(java.path("roleTitles")).hasSize(2);

        JsonNode react = byName.get("React");
        assertThat(react).as("React.js must resolve to the canonical React skill").isNotNull();
        assertThat(react.path("firstUsedMonth").asText()).isEqualTo("2019-01");
        assertThat(react.path("lastUsedMonth").asText()).isEqualTo("2021-06");
        assertThat(react.path("level").isNull()).isTrue();
        assertThat(react.path("verified").asBoolean()).isFalse();

        JsonNode python = byName.get("Python");
        assertThat(python).as("profile skills without history must still appear").isNotNull();
        assertThat(python.path("firstUsedMonth").isNull()).isTrue();
        assertThat(python.path("lastUsedMonth").isNull()).isTrue();
        assertThat(python.path("level").asText()).isEqualTo("beginner");

        JsonNode kubernetes = byName.get("Kubernetes");
        assertThat(kubernetes).as("history-only skills must appear").isNotNull();
        assertThat(kubernetes.path("firstUsedMonth").asText()).isEqualTo("2022-03");
    }

    @Test
    void journey_is_candidate_only() throws Exception {
        String employer = registerAndGetToken("journey.employer@example.com", "employer");
        mockMvc.perform(get("/api/candidate/journey")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer))
                .andExpect(status().isForbidden());
    }
}
