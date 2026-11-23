package com.fairfold.assessment;

import com.fairfold.skill.Skill;
import com.fairfold.skill.SkillAliasRepository;
import com.fairfold.skill.SkillCanonicalizer;
import com.fairfold.skill.SkillRepository;
import com.fairfold.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Duplicate skill merge: move references, skip conflicts, keep an alias. */
class SkillMergeIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private SkillAliasRepository aliasRepository;

    @Autowired
    private SkillCanonicalizer canonicalizer;

    private String sourceName;

    @AfterEach
    void cleanAlias() {
        if (sourceName != null) {
            aliasRepository.findByAliasIgnoreCase(sourceName).ifPresent(aliasRepository::delete);
        }
    }

    @Test
    void merge_moves_references_skips_conflicts_aliases_and_removes_the_duplicate() throws Exception {
        String admin = loginAdmin("merge-admin@example.com");
        UUID targetId = skillId("React");
        sourceName = "React Legacy " + UUID.randomUUID().toString().substring(0, 6);
        Skill source = new Skill();
        source.setName(sourceName);
        source = skillRepository.save(source);
        UUID sourceId = source.getId();

        TestEmployer employer = registerEmployer("merge.hr@acme.com");
        completeCompany(employer.token());
        Map<String, Object> jobBody = Map.of(
                "title", "Merge Job",
                "description", "Build UIs.",
                "requirements", "",
                "experienceLevel", "senior",
                "city", "Dhaka",
                "country", "Bangladesh",
                "remote", false,
                "requiredSkillIds", List.of(sourceId.toString(), targetId.toString()),
                "niceToHaveSkillIds", List.of(),
                "mustHaveSkillIds", List.of());
        mockMvc.perform(post("/api/employer/jobs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobBody)))
                .andExpect(status().isCreated());

        String candidate = registerAndGetToken("merge.candidate@example.com", "candidate");
        for (UUID skill : List.of(sourceId, targetId)) {
            mockMvc.perform(post("/api/candidate/skills")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "skillId", skill.toString(), "level", "intermediate"))))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(post("/api/admin/resources")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "skillId", sourceId.toString(), "title", "Merge resource",
                                "url", "https://example.com/merge", "cost", "free",
                                "language", "English", "level", "beginner", "active", true))))
                .andExpect(status().isCreated());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM assessments WHERE skill_id = ?", Long.class,
                sourceId.toString())).isEqualTo(1);

        mockMvc.perform(post("/api/admin/skills/" + sourceId + "/merge")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetSkillId\":\"" + targetId + "\"}"))
                .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM candidate_skills WHERE skill_id = ?", Long.class,
                sourceId.toString())).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM candidate_skills WHERE skill_id = ?", Long.class,
                targetId.toString())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM job_skills WHERE skill_id = ?", Long.class,
                sourceId.toString())).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM job_skills WHERE skill_id = ?", Long.class,
                targetId.toString())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM resources WHERE skill_id = ?", Long.class,
                targetId.toString())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM assessments WHERE skill_id = ?", Long.class,
                sourceId.toString())).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM assessments WHERE skill_id = ?", Long.class,
                targetId.toString())).isEqualTo(1);
        assertThat(skillRepository.findById(sourceId)).isEmpty();
        assertThat(aliasRepository.findByAliasIgnoreCase(sourceName))
                .get().extracting(alias -> alias.getSkillId()).isEqualTo(targetId);
        assertThat(canonicalizer.resolve(sourceName)).get().extracting(Skill::getId)
                .isEqualTo(targetId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_log WHERE action = 'skill_merged'", Long.class))
                .isEqualTo(1);

        mockMvc.perform(get("/api/candidate/skills")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.length()").value(1))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$[0].name").value("React"));
    }
}
