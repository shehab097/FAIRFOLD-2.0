package com.fairfold.team;

import com.fairfold.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Part 4.1: team invites, roles and disabled accounts (FR-047). */
class TeamIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TeamService teamService;

    private String hash(String raw) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(raw.getBytes(StandardCharsets.UTF_8)));
    }

    private String inviteAndSetToken(String hrToken, UUID companyId, String email, String role,
                                     String rawToken) throws Exception {
        var result = mockMvc.perform(post("/api/employer/team/invites")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "role", role))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID inviteId = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id").asText());
        jdbcTemplate.update("UPDATE team_invites SET token_hash = ? WHERE id = ?",
                hash(rawToken), inviteId.toString());
        return inviteId.toString();
    }

    private String acceptInvite(String rawToken, String email) throws Exception {
        var result = mockMvc.perform(post("/api/auth/accept-invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "token", rawToken, "firstName", "Team", "lastName", "Member",
                                "password", "password123", "consent", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value(email))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("accessToken").asText();
    }

    @Test
    void Fr047_invite_accept_then_disabled_login_is_forbidden() throws Exception {
        TestEmployer employer = registerEmployer("team.hr@acme.com");
        String email = "interviewer@acme.com";
        String raw = "invite-token-one";
        inviteAndSetToken(employer.token(), employer.companyId(), email, "interviewer", raw);

        mockMvc.perform(get("/api/auth/invites/" + raw))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("interviewer"))
                .andExpect(jsonPath("$.email").value(email));

        String interviewed = acceptInvite(raw, email);
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + interviewed))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("INTERVIEWER"))
                .andExpect(jsonPath("$.companyId").value(employer.companyId().toString()));

        mockMvc.perform(get("/api/employer/team")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members.length()").value(2))
                .andExpect(jsonPath("$.invites.length()").value(0));

        UUID memberId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", UUID.class, email);
        mockMvc.perform(delete("/api/employer/team/members/" + memberId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", "password123"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));

        mockMvc.perform(get("/api/employer/team")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members.length()").value(1));
    }

    @Test
    void Fr047_last_hr_cannot_be_demoted_or_removed_and_self_removal_is_blocked() throws Exception {
        TestEmployer employer = registerEmployer("last.hr@acme.com");
        TestUser candidate = registerUser("taken@example.com", "candidate");

        mockMvc.perform(post("/api/employer/team/invites")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "taken@example.com", "role", "interviewer"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));

        UUID hrId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = 'last.hr@acme.com'", UUID.class);
        mockMvc.perform(patch("/api/employer/team/members/" + hrId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"employer_manager\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LAST_HR"));

        mockMvc.perform(delete("/api/employer/team/members/" + hrId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CANNOT_REMOVE_SELF"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        teamService.remove(employer.companyId(), UUID.randomUUID(), hrId))
                .isInstanceOf(com.fairfold.common.errors.ApiException.class)
                .hasFieldOrPropertyWithValue("code", "LAST_HR");
        assertThat(candidate.userId()).isNotNull();
    }

    @Test
    void Fr047_role_change_and_reinvite_replaces_the_pending_invite() throws Exception {
        TestEmployer employer = registerEmployer("roles.hr@acme.com");
        String email = "pending@acme.com";
        String firstId = inviteAndSetToken(employer.token(), employer.companyId(), email,
                "interviewer", "replace-one");
        String rawTwo = "replace-two";
        String secondId = inviteAndSetToken(employer.token(), employer.companyId(), email,
                "employer_manager", rawTwo);

        mockMvc.perform(get("/api/employer/team")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invites.length()").value(1))
                .andExpect(jsonPath("$.invites[0].id").value(secondId))
                .andExpect(jsonPath("$.invites[0].role").value("employer_manager"));

        String memberToken = acceptInvite(rawTwo, email);
        UUID memberId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", UUID.class, email);
        mockMvc.perform(patch("/api/employer/team/members/" + memberId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"employer_hr\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("employer_hr"));

        mockMvc.perform(patch("/api/employer/team/members/" + memberId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"candidate\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/employer/team/invites/" + firstId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken))
                .andExpect(status().isOk());
        assertThat(secondId).isNotEqualTo(firstId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT disabled_at FROM users WHERE id = ?", Timestamp.class, memberId)).isNull();
    }
}
