package com.fairfold.candidate;

import com.fairfold.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CandidateIntegrationTest extends AbstractIntegrationTest {

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void Fr009_profile_starts_empty_and_saves() throws Exception {
        TestUser candidate = registerUser("cand@example.com", "candidate");

        mockMvc.perform(get("/api/candidate/profile").header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").isEmpty())
                .andExpect(jsonPath("$.remotePreference").value("any"));

        mockMvc.perform(put("/api/candidate/profile")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"+880","title":"Backend Engineer","bio":"Java dev",
                                 "location":"Dhaka","remotePreference":"remote",
                                 "totalExperienceYears":5.5,"educationLevel":"bachelor"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Backend Engineer"))
                .andExpect(jsonPath("$.location").value("Dhaka"))
                .andExpect(jsonPath("$.remotePreference").value("remote"))
                .andExpect(jsonPath("$.educationLevel").value("bachelor"));

        mockMvc.perform(get("/api/candidate/profile").header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Backend Engineer"));
    }

    @Test
    void Fr009_profile_invalid_remote_preference_400() throws Exception {
        TestUser candidate = registerUser("cand@example.com", "candidate");
        mockMvc.perform(put("/api/candidate/profile")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":null,"title":"T","bio":null,"location":"L",
                                 "remotePreference":"on_mars","totalExperienceYears":null,"educationLevel":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REMOTE_PREFERENCE"));
    }

    @Test
    void Fr014_add_list_update_delete_skill() throws Exception {
        TestUser candidate = registerUser("cand@example.com", "candidate");
        UUID java = skillId("Java");
        UUID react = skillId("React");

        mockMvc.perform(post("/api/candidate/skills")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"skillId":"%s","level":"intermediate","years":4}
                                """.formatted(java)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Java"))
                .andExpect(jsonPath("$.level").value("intermediate"))
                .andExpect(jsonPath("$.years").value(4));

        mockMvc.perform(post("/api/candidate/skills")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"skillId":"%s","level":"beginner","years":1}
                                """.formatted(react)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/candidate/skills").header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void Fr014_duplicate_skill_returns_409() throws Exception {
        TestUser candidate = registerUser("cand@example.com", "candidate");
        UUID java = skillId("Java");
        String body = """
                {"skillId":"%s","level":"beginner","years":1}
                """.formatted(java);
        mockMvc.perform(post("/api/candidate/skills")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token()))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/candidate/skills")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token()))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SKILL_EXISTS"));
    }

    @Test
    void Fr014_edit_and_delete_skill() throws Exception {
        TestUser candidate = registerUser("cand@example.com", "candidate");
        UUID java = skillId("Java");
        var result = mockMvc.perform(post("/api/candidate/skills")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"skillId":"%s","level":"beginner","years":1}
                                """.formatted(java)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID skillRowId = UUID.fromString(objectMapper.readTree(
                result.getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(put("/api/candidate/skills/" + skillRowId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"level":"expert","years":8}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value("expert"))
                .andExpect(jsonPath("$.years").value(8));

        mockMvc.perform(delete("/api/candidate/skills/" + skillRowId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/candidate/skills").header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void Fr014_invalid_years_returns_400() throws Exception {
        TestUser candidate = registerUser("cand@example.com", "candidate");
        UUID java = skillId("Java");
        mockMvc.perform(post("/api/candidate/skills")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"skillId":"%s","level":"beginner","years":99}
                                """.formatted(java)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_YEARS"));
    }

    @Test
    void dashboard_checklist_reflects_setup() throws Exception {
        TestUser candidate = registerUser("cand@example.com", "candidate");
        mockMvc.perform(get("/api/candidate/dashboard").header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checklist.profileSaved").value(false))
                .andExpect(jsonPath("$.checklist.resumeUploaded").value(false))
                .andExpect(jsonPath("$.checklist.skillsAdded").value(false));

        mockMvc.perform(put("/api/candidate/profile")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":null,"title":"Engineer","bio":null,"location":"Dhaka",
                                 "remotePreference":"any","totalExperienceYears":null,"educationLevel":null}
                                """))
                .andExpect(status().isOk());
        insertResume(candidate.userId());
        mockMvc.perform(post("/api/candidate/skills")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"skillId":"%s","level":"beginner","years":1}
                                """.formatted(skillId("Java"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/candidate/dashboard").header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checklist.profileSaved").value(true))
                .andExpect(jsonPath("$.checklist.resumeUploaded").value(true))
                .andExpect(jsonPath("$.checklist.skillsAdded").value(true));
    }
}
