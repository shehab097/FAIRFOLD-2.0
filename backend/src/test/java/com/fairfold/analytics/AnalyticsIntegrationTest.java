package com.fairfold.analytics;

import com.fairfold.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Part 5.2: analytics funnel, time-to-hire, overrides and pull-backs. */
class AnalyticsIntegrationTest extends AbstractIntegrationTest {

    private record Applied(UUID applicationId, UUID candidateId) {}

    private Applied apply(String candidateToken, UUID candidateId, UUID jobId) throws Exception {
        insertResume(candidateId);
        var result = mockMvc.perform(post("/api/jobs/" + jobId + "/applications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + candidateToken))
                .andExpect(status().isCreated())
                .andReturn();
        return new Applied(UUID.fromString(objectMapper.readTree(
                result.getResponse().getContentAsString()).path("id").asText()), candidateId);
    }

    private void history(UUID applicationId, String status, UUID actor, Instant at) {
        jdbcTemplate.update("INSERT INTO application_status_history "
                        + "(id, application_id, to_status, changed_by, created_at) "
                        + "VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID().toString(), applicationId.toString(), status,
                actor == null ? null : actor.toString(), Timestamp.from(at));
    }

    @Test
    void Fr035_funnel_time_to_hire_overrides_and_pullbacks() throws Exception {
        TestEmployer employer = registerEmployer("analytics.hr@example.com");
        completeCompany(employer.token());
        UUID jobId = createActiveJob(employer.token());
        UUID employerId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = 'analytics.hr@example.com'", UUID.class);

        TestUser c1 = registerUser("analytics.c1@example.com", "candidate");
        TestUser c2 = registerUser("analytics.c2@example.com", "candidate");
        TestUser c3 = registerUser("analytics.c3@example.com", "candidate");
        TestUser c4 = registerUser("analytics.c4@example.com", "candidate");
        Applied a1 = apply(c1.token(), c1.userId(), jobId);
        Applied a2 = apply(c2.token(), c2.userId(), jobId);
        Applied a3 = apply(c3.token(), c3.userId(), jobId);
        apply(c4.token(), c4.userId(), jobId);

        Instant hiredAt = Instant.now();
        Instant fiveDaysAgo = hiredAt.minusSeconds(5L * 24 * 3600);
        jdbcTemplate.update("UPDATE applications SET created_at = ? WHERE id = ?",
                Timestamp.from(fiveDaysAgo), a1.applicationId().toString());
        history(a1.applicationId(), "screened", employerId, fiveDaysAgo.plusSeconds(3600));
        history(a1.applicationId(), "shortlisted", employerId, fiveDaysAgo.plusSeconds(7200));
        history(a1.applicationId(), "interview", employerId, fiveDaysAgo.plusSeconds(10800));
        history(a1.applicationId(), "hired", employerId, hiredAt);
        history(a2.applicationId(), "screened", employerId, hiredAt);
        history(a3.applicationId(), "shortlisted", employerId, hiredAt);

        int aliased = jdbcTemplate.update("UPDATE candidate_aliases SET alias = 'C-ABCDE' "
                        + "WHERE company_id = ? AND candidate_id = ?",
                employer.companyId().toString(), a2.candidateId().toString());
        if (aliased == 0) {
            jdbcTemplate.update("INSERT INTO candidate_aliases (id, company_id, candidate_id, alias) "
                            + "VALUES (?, ?, ?, 'C-ABCDE')", UUID.randomUUID().toString(),
                    employer.companyId().toString(), a2.candidateId().toString());
        }
        jdbcTemplate.update("UPDATE applications SET override_direction = 'rejected_above_cutoff', "
                        + "decision_reason = 'Evidence was strong but the role needed more depth', "
                        + "decided_by = ?, updated_at = ? WHERE id = ?",
                employerId.toString(), Timestamp.from(hiredAt), a2.applicationId().toString());
        jdbcTemplate.update("UPDATE applications SET pulled_back_at = ? WHERE id = ?",
                Timestamp.from(hiredAt), a3.applicationId().toString());

        mockMvc.perform(get("/api/employer/analytics")
                        .param("jobId", jobId.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.funnel[0].stage").value("applied"))
                .andExpect(jsonPath("$.funnel[0].count").value(4))
                .andExpect(jsonPath("$.funnel[1].count").value(2))
                .andExpect(jsonPath("$.funnel[2].count").value(2))
                .andExpect(jsonPath("$.funnel[3].count").value(1))
                .andExpect(jsonPath("$.funnel[4].count").value(1))
                .andExpect(jsonPath("$.timeToHire.length()").value(1))
                .andExpect(jsonPath("$.timeToHire[0].hires").value(1))
                .andExpect(jsonPath("$.timeToHire[0].avgDays").value(5.0))
                .andExpect(jsonPath("$.overrides.length()").value(1))
                .andExpect(jsonPath("$.overrides[0].alias").value("C-ABCDE"))
                .andExpect(jsonPath("$.overrides[0].userName").value("First Last"))
                .andExpect(jsonPath("$.overrides[0].direction").value("rejected_above_cutoff"))
                .andExpect(jsonPath("$.pullBacks").value(1));

        mockMvc.perform(get("/api/employer/analytics")
                        .param("jobId", jobId.toString())
                        .param("from", LocalDate.now().minusDays(2).toString())
                        .param("to", LocalDate.now().toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.funnel[0].count").value(3));

        UUID managerId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO users (id, email, password_hash, first_name, last_name, "
                        + "role, company_id, consent_at) VALUES (?, 'analytics.manager@example.com', ?, "
                        + "'Analytics', 'Manager', 'EMPLOYER_MANAGER', ?, ?)",
                managerId.toString(), passwordEncoder.encode("password123"),
                employer.companyId().toString(), Timestamp.from(Instant.now()));
        var login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "analytics.manager@example.com", "password", "password123"))))
                .andExpect(status().isOk()).andReturn();
        String manager = objectMapper.readTree(login.getResponse().getContentAsString())
                .path("accessToken").asText();
        mockMvc.perform(get("/api/employer/analytics")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + manager))
                .andExpect(status().isOk());

        TestEmployer other = registerEmployer("analytics.other@example.com");
        completeCompany(other.token());
        UUID otherJob = createActiveJob(other.token());
        mockMvc.perform(get("/api/employer/analytics")
                        .param("jobId", otherJob.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isNotFound());
    }
}
