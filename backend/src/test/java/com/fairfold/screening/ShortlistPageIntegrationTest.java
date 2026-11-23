package com.fairfold.screening;

import com.fairfold.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A single company-wide shortlist page (FR-030 follow-up). */
class ShortlistPageIntegrationTest extends AbstractIntegrationTest {

    @Test
    void Fr030_shortlisted_page_lists_candidates_across_jobs() throws Exception {
        TestEmployer employer = registerEmployer("shortlist.hr@acme.com");
        completeCompany(employer.token());
        UUID firstJob = createActiveJob(employer.token());
        UUID secondJob = createActiveJob(employer.token());

        UUID firstCandidate = insertCandidate(firstJob, "shortlist.one@example.com", "One");
        UUID secondCandidate = insertCandidate(secondJob, "shortlist.two@example.com", "Two");
        UUID notShortlisted = insertCandidate(firstJob, "shortlist.no@example.com", "No");

        jdbcTemplate.update("""
                        UPDATE applications SET status = 'shortlisted', score = 82,
                            shortlisted_at = DATE_SUB(NOW(3), INTERVAL 2 MINUTE)
                        WHERE id = ?
                        """, firstCandidate.toString());
        jdbcTemplate.update("""
                        UPDATE applications SET status = 'shortlisted', score = 75,
                            shortlisted_at = DATE_SUB(NOW(3), INTERVAL 1 MINUTE)
                        WHERE id = ?
                        """, secondCandidate.toString());
        jdbcTemplate.update("""
                        UPDATE applications SET status = 'screened', score = 70
                        WHERE id = ?
                        """, notShortlisted.toString());

        mockMvc.perform(get("/api/employer/shortlisted")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].candidateEmail").value("shortlist.two@example.com"))
                .andExpect(jsonPath("$.items[0].candidateName").value("Two Candidate"))
                .andExpect(jsonPath("$.items[0].jobTitle").isNotEmpty())
                .andExpect(jsonPath("$.items[0].score").value(75))
                .andExpect(jsonPath("$.items[0].status").value("shortlisted"))
                .andExpect(jsonPath("$.items[1].candidateEmail").value("shortlist.one@example.com"));

        TestEmployer other = registerEmployer("shortlist.other@acme.com");
        mockMvc.perform(get("/api/employer/shortlisted")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + other.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    private UUID insertCandidate(UUID jobId, String email, String firstName) throws Exception {
        TestUser user = registerUser(email, "candidate");
        jdbcTemplate.update("UPDATE users SET first_name = ?, last_name = 'Candidate' WHERE id = ?",
                firstName, user.userId().toString());
        UUID resumeId = insertResume(user.userId());
        UUID applicationId = UUID.randomUUID();
        jdbcTemplate.update("""
                        INSERT INTO applications (id, job_id, candidate_id, resume_id, status)
                        VALUES (?, ?, ?, ?, 'applied')
                        """,
                applicationId.toString(), jobId.toString(), user.userId().toString(),
                resumeId.toString());
        return applicationId;
    }
}
