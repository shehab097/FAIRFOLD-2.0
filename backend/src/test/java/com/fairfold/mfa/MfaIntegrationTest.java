package com.fairfold.mfa;

import com.fairfold.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Part 5.3: TOTP setup, login step, replay protection and lockout. */
class MfaIntegrationTest extends AbstractIntegrationTest {

    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private static byte[] base32Decode(String value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int buffer = 0;
        int bits = 0;
        for (char c : value.toCharArray()) {
            int index = BASE32.indexOf(c);
            buffer = (buffer << 5) | index;
            bits += 5;
            if (bits >= 8) {
                out.write((buffer >> (bits - 8)) & 0xff);
                bits -= 8;
            }
        }
        return out.toByteArray();
    }

    private static String code(String secret, long step) {
        try {
            byte[] data = new byte[8];
            for (int i = 7; i >= 0; i--) {
                data[i] = (byte) (step & 0xff);
                step >>>= 8;
            }
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(base32Decode(secret), "HmacSHA1"));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            return String.format("%06d", binary % 1_000_000);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static long step() {
        return java.time.Instant.now().getEpochSecond() / 30;
    }

    private String setupAndEnable(String token) throws Exception {
        var setup = mockMvc.perform(post("/api/account/mfa/setup")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secret").isNotEmpty())
                .andExpect(jsonPath("$.otpauthUri").value(
                        org.hamcrest.Matchers.startsWith("otpauth://totp/FairFold:")))
                .andExpect(jsonPath("$.qrDataUri").value(
                        org.hamcrest.Matchers.startsWith("data:image/png;base64,")))
                .andReturn();
        String secret = objectMapper.readTree(setup.getResponse().getContentAsString())
                .path("secret").asText();
        mockMvc.perform(post("/api/account/mfa/enable")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", code(secret, step())))))
                .andExpect(status().isNoContent());
        return secret;
    }

    @Test
    void Fr005_Fr006_setup_login_replay_disable_and_me() throws Exception {
        TestUser candidate = registerUser("mfa.candidate@example.com", "candidate");
        String secret = setupAndEnable(candidate.token());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT mfa_enabled FROM users WHERE id = ?", Boolean.class,
                candidate.userId().toString())).isTrue();
        String encrypted = jdbcTemplate.queryForObject(
                "SELECT mfa_secret_enc FROM users WHERE id = ?", String.class,
                candidate.userId().toString());
        assertThat(encrypted).isNotBlank().isNotEqualTo(secret);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT mfa_pending_secret_enc FROM users WHERE id = ?", String.class,
                candidate.userId().toString())).isNull();

        mockMvc.perform(post("/api/account/mfa/setup")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate.token()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MFA_ALREADY_ENABLED"));

        var login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "mfa.candidate@example.com", "password", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaRequired").value(true))
                .andExpect(jsonPath("$.mfaToken").isNotEmpty())
                .andReturn();
        JsonNode challenge = objectMapper.readTree(login.getResponse().getContentAsString());
        String mfaToken = challenge.path("mfaToken").asText();
        assertThat(challenge.has("accessToken")).isFalse();

        mockMvc.perform(get("/api/candidate/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + mfaToken))
                .andExpect(status().isUnauthorized());

        String valid = code(secret, step());
        var verified = mockMvc.perform(post("/api/auth/mfa/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "mfaToken", mfaToken, "code", valid))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.mfaEnabled").value(true))
                .andReturn();
        String accessToken = objectMapper.readTree(verified.getResponse().getContentAsString())
                .path("accessToken").asText();
        mockMvc.perform(get("/api/candidate/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/mfa/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "mfaToken", mfaToken, "code", valid))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("MFA_CODE_INVALID"));

        mockMvc.perform(post("/api/account/mfa/disable")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "password", "wrong", "code", code(secret, step() + 1)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD"));

        mockMvc.perform(post("/api/account/mfa/disable")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "password", "password123", "code", code(secret, step() + 1)))))
                .andExpect(status().isNoContent());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT mfa_enabled FROM users WHERE id = ?", Boolean.class,
                candidate.userId().toString())).isFalse();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT mfa_secret_enc FROM users WHERE id = ?", String.class,
                candidate.userId().toString())).isNull();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "mfa.candidate@example.com", "password", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void Fr006_wrong_codes_lock_then_admin_reset_and_unlock() throws Exception {
        TestUser candidate = registerUser("mfa.lock@example.com", "candidate");
        String secret = setupAndEnable(candidate.token());
        var login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "mfa.lock@example.com", "password", "password123"))))
                .andExpect(status().isOk()).andReturn();
        String mfaToken = objectMapper.readTree(login.getResponse().getContentAsString())
                .path("mfaToken").asText();
        String valid = code(secret, step());
        String wrong = valid.equals("000000") ? "111111" : "000000";

        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/auth/mfa/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "mfaToken", mfaToken, "code", wrong))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("MFA_CODE_INVALID"))
                    .andExpect(jsonPath("$.details.triesLeft").value(5 - i));
        }
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "mfa.lock@example.com", "password", "password123"))))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));

        String admin = loginAdmin("mfa.admin@example.com");
        mockMvc.perform(post("/api/admin/users/" + candidate.userId() + "/reset-mfa")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                .andExpect(status().isNoContent());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = 'mfa_reset'", Long.class)).isEqualTo(1);
        mockMvc.perform(post("/api/admin/users/" + candidate.userId() + "/unlock")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "mfa.lock@example.com", "password", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }
}
