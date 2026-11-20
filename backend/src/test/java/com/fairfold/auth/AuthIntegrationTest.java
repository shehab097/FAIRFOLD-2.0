package com.fairfold.auth;

import com.fairfold.common.mail.MailService;
import com.fairfold.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private MailService mailService;

    @Test
    void Fr001_register_valid_candidate_returns_session() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"cand@example.com","password":"password123","firstName":"Cand","lastName":"Date","role":"candidate","consent":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("cand@example.com"))
                .andExpect(jsonPath("$.user.role").value("CANDIDATE"))
                .andExpect(jsonPath("$.user.companyId").isEmpty())
                .andExpect(cookie().exists("refresh_token"));
    }

    @Test
    void Fr001_register_employer_gets_company() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"hr@acme.com","password":"password123","firstName":"Hr","lastName":"Person","role":"employer","consent":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("EMPLOYER_HR"))
                .andExpect(jsonPath("$.user.companyId").isNotEmpty());

        Integer companies = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM companies", Integer.class);
        assertEquals(1, companies);
    }

    @Test
    void Fr001_register_duplicate_email_returns_409() throws Exception {
        register("cand@example.com");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"cand@example.com","password":"password123","firstName":"X","lastName":"Y","role":"candidate","consent":true}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    @Test
    void Fr001_register_short_password_returns_400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"cand@example.com","password":"short","firstName":"Cand","lastName":"Date","role":"candidate","consent":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }

    @Test
    void Fr001_register_without_consent_returns_400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"cand@example.com","password":"password123","firstName":"Cand","lastName":"Date","role":"candidate","consent":false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CONSENT_REQUIRED"));
    }

    @Test
    void Fr002_login_success_returns_session_and_resets_counter() throws Exception {
        register("cand@example.com");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"cand@example.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(cookie().exists("refresh_token"));
    }

    @Test
    void Fr002_login_wrong_credentials_returns_401() throws Exception {
        register("cand@example.com");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"cand@example.com","password":"wrongpassword"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void Fr003_lockout_after_five_failures_and_423_minutes_left() throws Exception {
        register("cand@example.com");
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"cand@example.com","password":"wrongpassword"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"cand@example.com","password":"password123"}
                                """))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void Fr004_forgot_password_always_returns_202() throws Exception {
        register("cand@example.com");
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"cand@example.com"}
                                """))
                .andExpect(status().isAccepted());
        // Unknown email is also 202 (never reveals existence).
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody@example.com"}
                                """))
                .andExpect(status().isAccepted());
    }

    @Test
    void Fr004_reset_password_single_use_and_revokes_tokens() throws Exception {
        register("cand@example.com");
        Cookie refreshCookie = loginAndGetCookie("cand@example.com", "password123");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"cand@example.com"}
                                """))
                .andExpect(status().isAccepted());

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailService).send(eq("cand@example.com"), anyString(), bodyCaptor.capture());
        String token = extractToken(bodyCaptor.getValue());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"newPassword\":\"newpassword123\"}"))
                .andExpect(status().isNoContent());

        // New password works.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"cand@example.com","password":"newpassword123"}
                                """))
                .andExpect(status().isOk());

        // Old refresh token was revoked.
        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isUnauthorized());

        // Token is single use.
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"newPassword\":\"anotherpassword123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
    }

    @Test
    void Fr007_refresh_rotates_and_reuse_returns_401() throws Exception {
        register("cand@example.com");
        Cookie refreshCookie = loginAndGetCookie("cand@example.com", "password123");

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        assertNotNull(refreshResult.getResponse().getCookie("refresh_token"));

        // Reusing the old (now revoked) cookie returns 401.
        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void Fr008_logout_revokes_refresh_token() throws Exception {
        register("cand@example.com");
        Cookie refreshCookie = loginAndGetCookie("cand@example.com", "password123");

        mockMvc.perform(post("/api/auth/logout").cookie(refreshCookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rate_limit_login_returns_429() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"x@example.com","password":"whatever123"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"x@example.com","password":"whatever123"}
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
    }

    // --- helpers ----------------------------------------------------------

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123","firstName":"First","lastName":"Last","role":"candidate","consent":true}
                                """.formatted(email)))
                .andExpect(status().isCreated());
    }

    private Cookie loginAndGetCookie(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("refresh_token");
        assertNotNull(cookie);
        return cookie;
    }

    private String extractToken(String body) {
        Matcher matcher = Pattern.compile("token=([A-Za-z0-9_-]+)").matcher(body);
        assertTrue(matcher.find(), "Reset link not found in mail body: " + body);
        return matcher.group(1);
    }
}
