package com.fairfold.job;

import com.fairfold.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicJobBoardIntegrationTest extends AbstractIntegrationTest {

    @Test
    void Fr042_search_by_text() throws Exception {
        TestEmployer employer = setupEmployer("hr@acme.com");
        createActiveJob(employer, "Backend Engineer", "We build blockchain ledgers.", "Bangladesh", false, "senior", List.of());
        createActiveJob(employer, "Frontend Engineer", "We build UIs.", "Bangladesh", false, "mid", List.of());

        mockMvc.perform(get("/api/jobs").param("q", "blockchain"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Backend Engineer"));
    }

    @Test
    void Fr042_search_by_skill_name() throws Exception {
        TestEmployer employer = setupEmployer("hr@acme.com");
        createActiveJob(employer, "React Dev", "UI work", "Bangladesh", false, "mid", List.of("React"));
        createActiveJob(employer, "Java Dev", "Backend work", "Bangladesh", false, "senior", List.of("Java"));

        mockMvc.perform(get("/api/jobs").param("q", "react"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].title").value("React Dev"));
    }

    @Test
    void Fr042_filter_location() throws Exception {
        TestEmployer employer = setupEmployer("hr@acme.com");
        createActiveJob(employer, "Dhaka Job", "desc", "Bangladesh", false, "mid", List.of());
        createActiveJob(employer, "Remote Job", "desc", "India", false, "mid", List.of());

        mockMvc.perform(get("/api/jobs").param("location", "bangladesh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Dhaka Job"));
    }

    @Test
    void Fr042_filter_remote_and_level() throws Exception {
        TestEmployer employer = setupEmployer("hr@acme.com");
        createActiveJob(employer, "Remote Senior", "desc", "Bangladesh", true, "senior", List.of());
        createActiveJob(employer, "Onsite Entry", "desc", "Bangladesh", false, "entry", List.of());

        mockMvc.perform(get("/api/jobs").param("remote", "true").param("level", "senior"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Remote Senior"));
    }

    @Test
    void Fr042_hidden_company_name() throws Exception {
        TestEmployer employer = setupEmployer("hr@acme.com", false);
        createActiveJob(employer, "Hidden Co Job", "desc", "Bangladesh", false, "mid", List.of());

        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].companyName").isEmpty());

        // now make it public
        putCompany(employer, "Acme Corp", true);
        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].companyName").value("Acme Corp"));
    }

    @Test
    void Fr042_inactive_jobs_not_listed() throws Exception {
        TestEmployer employer = setupEmployer("hr@acme.com");
        // draft job (not activated)
        createDraftJob(employer, "Draft Job");
        // active job
        createActiveJob(employer, "Active Job", "desc", "Bangladesh", false, "mid", List.of());

        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Active Job"));
    }

    // --- helpers -----------------------------------------------------------

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private TestEmployer setupEmployer(String email) throws Exception {
        return setupEmployer(email, false);
    }

    private TestEmployer setupEmployer(String email, boolean showName) throws Exception {
        TestEmployer employer = registerEmployer(email);
        putCompany(employer, "Acme Corp", showName);
        return employer;
    }

    private void putCompany(TestEmployer employer, String name, boolean showName) throws Exception {
        String body = """
                {"name":"%s","industry":"Software","size":"11-50","showNamePublicly":%s}
                """.formatted(name, showName);
        mockMvc.perform(put("/api/employer/company")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token()))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    private UUID createActiveJob(TestEmployer employer, String title, String description,
                                 String country, boolean remote, String level, List<String> skills) throws Exception {
        UUID jobId = createJob(employer, title, description, country, remote, level, skills);
        mockMvc.perform(post("/api/employer/jobs/" + jobId + "/activate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isOk());
        return jobId;
    }

    private UUID createDraftJob(TestEmployer employer, String title) throws Exception {
        return createJob(employer, title, "desc", "Bangladesh", false, "mid", List.of());
    }

    private UUID createJob(TestEmployer employer, String title, String description,
                           String country, boolean remote, String level, List<String> skillNames) throws Exception {
        List<String> skillIds = skillNames.stream().map(this::skillId).map(UUID::toString).toList();
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("description", description);
        body.put("requirements", "");
        body.put("experienceLevel", level);
        body.put("city", "City");
        body.put("country", country);
        body.put("remote", remote);
        body.put("requiredSkillIds", skillIds);
        body.put("niceToHaveSkillIds", List.of());
        body.put("mustHaveSkillIds", List.of());

        var result = mockMvc.perform(post("/api/employer/jobs")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }
}
