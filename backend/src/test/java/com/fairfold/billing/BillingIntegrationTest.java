package com.fairfold.billing;

import com.fairfold.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Part 5.2: plans, fake checkout, subscriptions and quota display. */
class BillingIntegrationTest extends AbstractIntegrationTest {

    private String planId(String token, String name) throws Exception {
        var result = mockMvc.perform(get("/api/employer/billing")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        for (JsonNode plan : objectMapper.readTree(result.getResponse().getContentAsString())
                .path("plans")) {
            if (plan.path("name").asText().equals(name)) {
                return plan.path("id").asText();
            }
        }
        throw new AssertionError("Plan not found: " + name);
    }

    private String checkout(String token, String planId) throws Exception {
        var result = mockMvc.perform(post("/api/employer/billing/checkout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("planId", planId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirectUrl").value(
                        org.hamcrest.Matchers.containsString("/api/payments/fake/pay")))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("redirectUrl").asText();
    }

    private String providerPaymentId() {
        return jdbcTemplate.queryForObject(
                "SELECT provider_payment_id FROM payments WHERE status = 'initiated' "
                        + "ORDER BY created_at DESC LIMIT 1", String.class);
    }

    private void fakePay(String providerPaymentId, String outcome) throws Exception {
        mockMvc.perform(get("/api/payments/fake/pay")
                        .param("paymentId", providerPaymentId)
                        .param("outcome", outcome))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION,
                        org.hamcrest.Matchers.containsString("/employer/billing?payment=")));
    }

    @Test
    void Fr048_Fr056_fake_purchase_activates_plan_and_is_idempotent() throws Exception {
        TestEmployer employer = registerEmployer("billing.hr@example.com");
        completeCompany(employer.token());

        mockMvc.perform(get("/api/employer/billing")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan.name").value("Free"))
                .andExpect(jsonPath("$.quota").value(20))
                .andExpect(jsonPath("$.plans.length()").value(3));

        String standard = planId(employer.token(), "Standard");
        String redirect = checkout(employer.token(), standard);
        assertThat(redirect).contains("outcome=success");
        String paymentId = providerPaymentId();

        mockMvc.perform(get("/api/employer/billing")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(jsonPath("$.plan.name").value("Free"));

        fakePay(paymentId, "success");
        mockMvc.perform(get("/api/employer/billing")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan.name").value("Standard"))
                .andExpect(jsonPath("$.quota").value(200))
                .andExpect(jsonPath("$.plan.endsAt").isNotEmpty());

        fakePay(paymentId, "success");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM company_subscriptions", Long.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payments WHERE status = 'completed'", Long.class)).isEqualTo(1);
        mockMvc.perform(get("/api/employer/billing/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].planName").value("Standard"))
                .andExpect(jsonPath("$.items[0].status").value("completed"));
    }

    @Test
    void Fr056_cancelled_failed_and_amount_mismatch_never_activate_a_plan() throws Exception {
        TestEmployer employer = registerEmployer("billing.bad@example.com");
        completeCompany(employer.token());
        String standard = planId(employer.token(), "Standard");

        checkout(employer.token(), standard);
        fakePay(providerPaymentId(), "cancelled");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM payments ORDER BY created_at DESC LIMIT 1", String.class))
                .isEqualTo("cancelled");

        checkout(employer.token(), standard);
        fakePay(providerPaymentId(), "failed");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM payments ORDER BY created_at DESC LIMIT 1", String.class))
                .isEqualTo("failed");

        checkout(employer.token(), standard);
        String mismatchId = providerPaymentId();
        jdbcTemplate.update("UPDATE payments SET amount_bdt = 9999 WHERE provider_payment_id = ?",
                mismatchId);
        fakePay(mismatchId, "success");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM payments WHERE provider_payment_id = ?", String.class, mismatchId))
                .isEqualTo("failed");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM company_subscriptions", Long.class)).isZero();
        mockMvc.perform(get("/api/employer/billing")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(jsonPath("$.plan.name").value("Free"))
                .andExpect(jsonPath("$.quota").value(20));
    }

    @Test
    void Fr048_expiry_returns_to_free_and_a_new_purchase_replaces_the_old() throws Exception {
        TestEmployer employer = registerEmployer("billing.cycle@example.com");
        completeCompany(employer.token());
        String standard = planId(employer.token(), "Standard");
        String pro = planId(employer.token(), "Pro");

        checkout(employer.token(), standard);
        fakePay(providerPaymentId(), "success");
        jdbcTemplate.update("UPDATE company_subscriptions "
                + "SET starts_at = DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 40 DAY), "
                + "ends_at = DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 5 SECOND)");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM company_subscriptions WHERE ends_at > ?", Long.class,
                java.sql.Timestamp.from(Instant.now()))).isZero();
        mockMvc.perform(get("/api/employer/billing")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(jsonPath("$.plan.name").value("Free"));

        checkout(employer.token(), standard);
        fakePay(providerPaymentId(), "success");
        checkout(employer.token(), pro);
        fakePay(providerPaymentId(), "success");
        mockMvc.perform(get("/api/employer/billing")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employer.token()))
                .andExpect(jsonPath("$.plan.name").value("Pro"))
                .andExpect(jsonPath("$.quota").value(1000));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM company_subscriptions WHERE ends_at > ?",
                Long.class, java.sql.Timestamp.from(Instant.now()))).isEqualTo(1);
    }

    @Test
    void Fr048_dashboard_quota_is_visible_to_hr_only() throws Exception {
        TestEmployer hr = registerEmployer("billing.dashboard@example.com");
        completeCompany(hr.token());
        mockMvc.perform(get("/api/employer/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + hr.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quota.quota").value(20))
                .andExpect(jsonPath("$.quota.planName").value("Free"));

        UUID managerId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO users (id, email, password_hash, first_name, last_name, "
                        + "role, company_id, consent_at) VALUES (?, 'billing.manager@example.com', ?, "
                        + "'Billing', 'Manager', 'EMPLOYER_MANAGER', ?, ?)",
                managerId.toString(), passwordEncoder.encode("password123"),
                hr.companyId().toString(), java.sql.Timestamp.from(Instant.now()));
        var login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "billing.manager@example.com",
                                "password", "password123"))))
                .andExpect(status().isOk()).andReturn();
        String manager = objectMapper.readTree(login.getResponse().getContentAsString())
                .path("accessToken").asText();
        mockMvc.perform(get("/api/employer/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quota").doesNotExist());
    }
}
