package com.fairfold.message;

import com.fairfold.screening.ScreeningTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Part 4.2: application messaging after shortlist (FR-043). */
class MessageIntegrationTest extends ScreeningTestSupport {

    private UUID shortlisted(TestEmployer employer, UUID jobId, String email) throws Exception {
        Candidate candidate = insertApplicant(jobId, email, "Message", "Candidate",
                "Dhaka, Bangladesh", new BigDecimal("5"), true);
        jdbcTemplate.update("""
                        UPDATE applications SET status = 'shortlisted', shortlisted_at = CURRENT_TIMESTAMP(3)
                        WHERE id = ?
                        """, candidate.applicationId().toString());
        return candidate.applicationId();
    }

    private String tokenFor(String email, String password) throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("accessToken").asText();
    }

    @Test
    void Fr043_thread_opens_after_shortlist_and_both_sides_exchange_messages() throws Exception {
        TestEmployer employer = registerEmployer("message.hr@acme.com");
        completeCompany(employer.token());
        UUID jobId = createScreeningJob(employer.token());
        Candidate candidate = insertApplicant(jobId, "message.before@example.com", "Before", "Shortlist",
                "Dhaka, Bangladesh", new BigDecimal("5"), true);

        mockMvc.perform(get("/api/applications/" + candidate.applicationId() + "/messages")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidate.token()))
                .andExpect(status().isForbidden());

        UUID applicationId = shortlisted(employer, jobId, "message.candidate@example.com");
        String candidateToken = tokenFor("message.candidate@example.com", "password123");

        mockMvc.perform(post("/api/applications/" + applicationId + "/messages")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Hello, I am available for a call.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mine").value(true));

        mockMvc.perform(get("/api/applications/" + applicationId + "/messages")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].mine").value(false))
                .andExpect(jsonPath("$[0].senderName").value("Message Candidate"))
                .andExpect(jsonPath("$[0].body").value("Hello, I am available for a call."));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM messages WHERE application_id = ? AND read_at IS NOT NULL",
                Long.class, applicationId.toString())).isEqualTo(1);

        mockMvc.perform(post("/api/applications/" + applicationId + "/messages")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Thanks, does Tuesday work?\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/applications/" + applicationId + "/messages")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].mine").value(true))
                .andExpect(jsonPath("$[1].mine").value(false))
                .andExpect(jsonPath("$[1].body").value("Thanks, does Tuesday work?"));

        TestEmployer other = registerEmployer("message.other@acme.com");
        mockMvc.perform(get("/api/applications/" + applicationId + "/messages")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + other.token()))
                .andExpect(status().isNotFound());
    }

    @Test
    void Fr043_closed_threads_reject_sending_after_rejection() throws Exception {
        TestEmployer employer = registerEmployer("closed.hr@acme.com");
        completeCompany(employer.token());
        UUID jobId = createScreeningJob(employer.token());
        UUID applicationId = shortlisted(employer, jobId, "closed.candidate@example.com");
        String candidateToken = tokenFor("closed.candidate@example.com", "password123");

        mockMvc.perform(post("/api/employer/applications/" + applicationId + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/applications/" + applicationId + "/messages")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Can we still talk?\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("THREAD_CLOSED"));
    }

    @Test
    void Fr043_rate_limit_notification_collapse_and_no_text_in_audit_or_ai() throws Exception {
        TestEmployer employer = registerEmployer("limit.hr@acme.com");
        completeCompany(employer.token());
        UUID jobId = createScreeningJob(employer.token());
        UUID applicationId = shortlisted(employer, jobId, "limit.candidate@example.com");
        String candidateToken = tokenFor("limit.candidate@example.com", "password123");
        rateLimitService.reset();

        String secret = "SECRET-MESSAGE-TEXT-7788";
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/applications/" + applicationId + "/messages")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidateToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("body", secret + " " + i))))
                    .andExpect(status().isCreated());
        }
        mockMvc.perform(post("/api/applications/" + applicationId + "/messages")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"one too many\"}"))
                .andExpect(status().isTooManyRequests());

        UUID hrId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = 'limit.hr@acme.com'", UUID.class);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM notifications WHERE user_id = ? AND type = 'message' "
                        + "AND read_at IS NULL", Long.class, hrId.toString())).isEqualTo(1);

        jdbcTemplate.update("UPDATE notifications SET read_at = NOW(3) WHERE user_id = ? AND type = 'message'",
                hrId.toString());
        rateLimitService.reset();
        mockMvc.perform(post("/api/applications/" + applicationId + "/messages")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"after notification read\"}"))
                .andExpect(status().isCreated());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM notifications WHERE user_id = ? AND type = 'message'",
                Long.class, hrId.toString())).isEqualTo(2);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_log WHERE details LIKE ?", Long.class,
                "%" + secret + "%")).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM ai_calls", Long.class)).isZero();
    }

    @Test
    void Fr043_afterId_returns_only_newer_messages() throws Exception {
        TestEmployer employer = registerEmployer("after.hr@acme.com");
        completeCompany(employer.token());
        UUID jobId = createScreeningJob(employer.token());
        UUID applicationId = shortlisted(employer, jobId, "after.candidate@example.com");
        String candidateToken = tokenFor("after.candidate@example.com", "password123");

        var first = mockMvc.perform(post("/api/applications/" + applicationId + "/messages")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"first message\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String firstId = objectMapper.readTree(first.getResponse().getContentAsString())
                .path("id").asText();
        mockMvc.perform(post("/api/applications/" + applicationId + "/messages")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"second message\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/applications/" + applicationId + "/messages")
                        .param("afterId", firstId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].body").value("second message"));
    }
}
