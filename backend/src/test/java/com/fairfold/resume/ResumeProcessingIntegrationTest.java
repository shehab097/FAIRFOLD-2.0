package com.fairfold.resume;

import com.fairfold.common.storage.StorageService;
import com.fairfold.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.io.InputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResumeProcessingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ResumeProcessingService processingService;

    @Autowired
    private ResumeProcessingRecovery recovery;

    @Autowired
    private StorageService storageService;

    @Test
    void Fr012_processing_marks_resume_ready_with_stripped_text_and_suggestions() throws Exception {
        TestUser candidate = registerUser("tanvir@example.com", "candidate");
        prepareCandidate(candidate);

        upload(candidate.token(), "normal_1.pdf");
        JsonNode resume = awaitTerminal(candidate.token(), "ready");

        String stripped = resume.path("strippedText").asText();
        assertThat(stripped).isNotBlank();
        assertThat(stripped).doesNotContain("test.candidate@example.com", "University of Dhaka",
                "Acme Technologies", "BlueOrbit Solutions", "12 March 1992");
        assertThat(stripped).contains("[NAME]", "[PHONE]");
        assertThat(resume.path("droppedPlaceholders")).isNotEmpty();

        mockMvc.perform(get("/api/candidate/resume/suggestions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("rule_based"))
                .andExpect(jsonPath("$.educationLevel").value("bachelor"))
                .andExpect(jsonPath("$.skills[?(@.name == 'Java')]").exists())
                .andExpect(jsonPath("$.skills[?(@.name == 'Spring Boot')]").exists());

        Integer audits = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = 'resume_stripped'", Integer.class);
        assertThat(audits).isEqualTo(1);
    }

    @Test
    void Fr013_apply_suggestions_updates_profile_skills_and_work_history() throws Exception {
        TestUser candidate = registerUser("apply@example.com", "candidate");
        prepareCandidate(candidate);
        upload(candidate.token(), "normal_1.pdf");
        awaitTerminal(candidate.token(), "ready");

        String body = """
                {"skills":[{"name":"Java","level":"intermediate","years":3},
                            {"name":"JS","level":"beginner","years":null},
                            {"name":"Spring Security","level":"intermediate","years":2},
                            {"name":"Java 17/21","level":"expert","years":7},
                            {"name":"TCP/IP","level":"intermediate","years":2},
                            {"name":"Bad(Name)","level":"beginner","years":null},
                            {"name":"CustomTool-1","level":"expert","years":1}],
                 "totalExperienceYears":4.5,
                 "educationLevel":"bachelor",
                 "jobTitles":["Backend Engineer","Software Engineer"],
                 "workHistory":[{"title":"Backend Engineer","employer":"Acme Technologies",
                                 "startMonth":"2020-01","endMonth":null,"isCurrent":true,
                                 "skillsUsed":["Java","Spring Boot"]}]}
                """;
        mockMvc.perform(post("/api/candidate/resume/suggestions/apply")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/candidate/profile")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExperienceYears").value(4.5))
                .andExpect(jsonPath("$.educationLevel").value("bachelor"))
                .andExpect(jsonPath("$.previousJobTitles[0]").value("Backend Engineer"));

        MvcResult skills = mockMvc.perform(get("/api/candidate/skills")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode skillsNode = objectMapper.readTree(skills.getResponse().getContentAsString());
        assertThat(skillsNode).isNotEmpty();
        assertThat(skillsNode.findValuesAsText("name")).contains("Java", "JavaScript");
        assertThat(skillsNode.findValuesAsText("name"))
                .contains("CustomTool-1", "Spring Security", "TCP/IP", "Java")
                .doesNotContain("Bad(Name)", "Java 17/21");

        mockMvc.perform(get("/api/candidate/work-history")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Backend Engineer"))
                .andExpect(jsonPath("$[0].employer").value("Acme Technologies"))
                .andExpect(jsonPath("$[0].startMonth").value("2020-01"))
                .andExpect(jsonPath("$[0].isCurrent").value(true));
    }

    @Test
    void Fr015_skill_suggestions_resolve_aliases_to_the_canonical_skill() throws Exception {
        TestUser candidate = registerUser("alias@example.com", "candidate");

        String js = mockMvc.perform(get("/api/skills/suggest").param("q", "JS")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(js).findValuesAsText("name")).contains("JavaScript");

        String react = mockMvc.perform(get("/api/skills/suggest").param("q", "ReactJS")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(react).findValuesAsText("name")).contains("React");
    }

    @Test
    void Fr012_scanned_pdf_fails_with_a_clear_reason() throws Exception {
        TestUser candidate = registerUser("scan@example.com", "candidate");
        upload(candidate.token(), "image_only_1.pdf");
        JsonNode resume = awaitTerminal(candidate.token(), "failed");
        assertThat(resume.path("failureReason").asText()).isEqualTo("SCANNED_OR_EMPTY");
        assertThat(resume.path("failureMessage").asText()).contains("readable text");
        assertThat(resume.path("strippedText").isNull()).isTrue();
    }

    @Test
    void Fr012_bangla_pdf_fails_as_not_english() throws Exception {
        TestUser candidate = registerUser("bangla@example.com", "candidate");
        upload(candidate.token(), "bangla.pdf");
        JsonNode resume = awaitTerminal(candidate.token(), "failed");
        assertThat(resume.path("failureReason").asText()).isEqualTo("NOT_ENGLISH");
        assertThat(resume.path("failureMessage").asText()).contains("English");
    }

    @Test
    void Fr057_candidate_can_report_a_wrong_strip() throws Exception {
        TestUser candidate = registerUser("report@example.com", "candidate");
        prepareCandidate(candidate);
        upload(candidate.token(), "normal_1.pdf");
        awaitTerminal(candidate.token(), "ready");

        mockMvc.perform(post("/api/candidate/resume/strip-reports")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"My project name was stripped\",\"excerpt\":\"[EMPLOYER_1]\"}"))
                .andExpect(status().isCreated());

        Integer reports = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM strip_reports", Integer.class);
        assertThat(reports).isEqualTo(1);
    }

    @Test
    void work_history_crud() throws Exception {
        TestUser candidate = registerUser("work@example.com", "candidate");

        MvcResult created = mockMvc.perform(post("/api/candidate/work-history")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Backend Engineer","employer":"Acme","startMonth":"2021-02",
                                 "endMonth":"2023-04","isCurrent":false,"skillsUsed":["Java"]}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).path("id").asText();

        mockMvc.perform(put("/api/candidate/work-history/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Senior Backend Engineer","employer":"Acme","startMonth":"2021-02",
                                 "endMonth":null,"isCurrent":true,"skillsUsed":["Java","AWS"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Senior Backend Engineer"))
                .andExpect(jsonPath("$.isCurrent").value(true))
                .andExpect(jsonPath("$.endMonth").value(org.hamcrest.Matchers.nullValue()));

        mockMvc.perform(delete("/api/candidate/work-history/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/candidate/work-history")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void restart_recovery_requeues_processing_resumes() throws Exception {
        TestUser candidate = registerUser("recover@example.com", "candidate");
        String storageKey = storageService.save(bytes("/resumes/pdf/normal_1.pdf"), "cv.pdf");
        UUID resumeId = insertProcessingResume(candidate.userId(), storageKey);

        recovery.requeue();

        JsonNode resume = awaitTerminal(candidate.token(), "ready");
        assertThat(resume.path("id").asText()).isEqualTo(resumeId.toString());
    }

    private void prepareCandidate(TestUser candidate) throws Exception {
        jdbcTemplate.update("UPDATE users SET first_name = 'Tanvir', last_name = 'Ahmed' WHERE id = ?",
                candidate.userId().toString());
        mockMvc.perform(put("/api/candidate/profile")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"+8801712345678\",\"remotePreference\":\"any\"}"))
                .andExpect(status().isOk());
    }

    private void upload(String token, String pdf) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", pdf, "application/pdf",
                bytes("/resumes/pdf/" + pdf));
        mockMvc.perform(multipart("/api/candidate/resume").file(file)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("processing"));
    }

    private JsonNode awaitTerminal(String token, String expected) throws Exception {
        for (int i = 0; i < 100; i++) {
            MvcResult result = mockMvc.perform(get("/api/candidate/resume")
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
            String status = node.path("status").asText();
            if ("ready".equals(status) || "failed".equals(status)) {
                assertThat(status).as("resume status").isEqualTo(expected);
                return node;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Resume did not finish processing in time");
    }

    private UUID insertProcessingResume(UUID userId, String storageKey) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO resumes (id, user_id, original_filename, storage_key, size_bytes, status, is_current, current_user_key) "
                        + "VALUES (?, ?, 'cv.pdf', ?, 1234, 'processing', true, ?)",
                id.toString(), userId.toString(), storageKey, userId.toString());
        return id;
    }

    private byte[] bytes(String resource) throws Exception {
        try (InputStream stream = getClass().getResourceAsStream(resource)) {
            assertThat(stream).as(resource).isNotNull();
            return stream.readAllBytes();
        }
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
