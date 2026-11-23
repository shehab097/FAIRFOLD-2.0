package com.fairfold.screening;

import com.fairfold.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Shared setup for screening integration tests. */
public abstract class ScreeningTestSupport extends AbstractIntegrationTest {

    protected UUID createScreeningJob(String token) throws Exception {
        return createScreeningJob(token, true);
    }

    protected UUID createScreeningJob(String token, boolean activate) throws Exception {
        UUID javaId = skillId("Java");
        UUID springId = skillId("Spring Boot");
        UUID dockerId = skillId("Docker");
        String body = """
                {"title":"Backend Engineer","description":"Build services.","requirements":"Java",
                 "experienceLevel":"senior","city":"Dhaka","country":"Bangladesh","remote":false,
                 "requiredSkillIds":["%s","%s"],"niceToHaveSkillIds":["%s"],"mustHaveSkillIds":["%s"],
                 "cutoffScore":60}
                """.formatted(javaId, springId, dockerId, javaId);
        var result = mockMvc.perform(post("/api/employer/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        UUID jobId = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id").asText());
        if (activate) {
            mockMvc.perform(post("/api/employer/jobs/" + jobId + "/activate")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
        return jobId;
    }

    protected Candidate insertApplicant(UUID jobId, String email, String firstName, String lastName,
                                        String location, BigDecimal years, boolean withJava) throws Exception {
        rateLimitService.reset();
        TestUser user = registerUser(email, "candidate");
        jdbcTemplate.update("UPDATE users SET first_name = ?, last_name = ? WHERE id = ?",
                firstName, lastName, user.userId().toString());
        jdbcTemplate.update("""
                        INSERT INTO candidate_profiles
                            (user_id, phone, title, location, remote_preference, total_experience_years,
                             education_level, previous_job_titles)
                        VALUES (?, '+8801712345678', 'Backend Engineer', ?, 'any', ?, 'bachelor', JSON_ARRAY())
                        ON DUPLICATE KEY UPDATE location = VALUES(location),
                                                total_experience_years = VALUES(total_experience_years)
                        """,
                user.userId().toString(), location, years);

        if (withJava) {
            insertSkill(user.userId(), skillId("Java"), "expert");
            insertSkill(user.userId(), skillId("Spring Boot"), "expert");
        } else {
            insertSkill(user.userId(), skillId("Python"), "intermediate");
        }

        String text = withJava
                ? "Built Java and Spring Boot services with Docker for a payments platform. "
                + "Led a team and improved reliability across the stack."
                : "Built Python services with Django and delivered internal tools.";
        UUID resumeId = UUID.randomUUID();
        jdbcTemplate.update("""
                        INSERT INTO resumes
                            (id, user_id, original_filename, storage_key, size_bytes, status, stripped_text,
                             is_current, current_user_key)
                        VALUES (?, ?, 'cv.pdf', ?, 1024, 'ready', ?, true, ?)
                        """,
                resumeId.toString(), user.userId().toString(), UUID.randomUUID().toString(),
                text, user.userId().toString());

        UUID applicationId = UUID.randomUUID();
        jdbcTemplate.update("""
                        INSERT INTO applications (id, job_id, candidate_id, resume_id, status, created_at)
                        VALUES (?, ?, ?, ?, 'applied', CURRENT_TIMESTAMP(3))
                        """,
                applicationId.toString(), jobId.toString(), user.userId().toString(),
                resumeId.toString());
        return new Candidate(user.userId(), user.token(), applicationId, resumeId);
    }

    private void insertSkill(UUID userId, UUID skillId, String level) {
        jdbcTemplate.update("""
                        INSERT INTO candidate_skills (id, user_id, skill_id, level, years, verified)
                        VALUES (?, ?, ?, ?, NULL, false)
                        """,
                UUID.randomUUID().toString(), userId.toString(), skillId.toString(), level);
    }

    protected void setResult(UUID applicationId, String status, Integer score,
                             String biasStatus, String gateStatus) {
        jdbcTemplate.update("""
                        UPDATE applications
                        SET status = ?, score = ?, bias_status = ?, gate_status = ?
                        WHERE id = ?
                        """,
                status, score, biasStatus, gateStatus, applicationId.toString());
        jdbcTemplate.update("""
                        UPDATE applications
                        SET scoring_mode = CASE WHEN status IN ('screened', 'not_matched')
                                                THEN 'rule_based' ELSE NULL END
                        WHERE id = ?
                        """,
                applicationId.toString());
    }

    protected JsonNode startAndAwait(String token, UUID jobId) throws Exception {
        var result = mockMvc.perform(post("/api/employer/jobs/" + jobId + "/screening")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted())
                .andReturn();
        String runId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("runId").asText();
        for (int i = 0; i < 200; i++) {
            var progress = mockMvc.perform(get("/api/employer/jobs/" + jobId + "/screening")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode node = objectMapper.readTree(progress.getResponse().getContentAsString());
            if ("finished".equals(node.path("status").asText())) {
                return node;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Screening run " + runId + " did not finish in time");
    }

    protected record Candidate(UUID userId, String token, UUID applicationId, UUID resumeId) {}
}
