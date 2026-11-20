package com.fairfold.support;

import com.fairfold.common.ratelimit.RateLimitService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for integration tests. Runs against {@code fairfold_test} and
 * truncates every table before each test (Flyway migrations are applied once
 * by Spring Boot on context startup).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected RateLimitService rateLimitService;

    protected record TestEmployer(String token, UUID companyId) {}

    protected record TestUser(String token, UUID userId) {}

    @BeforeEach
    void cleanDatabase() {
        rateLimitService.reset();
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        // Reference data (skills) is seeded by Flyway and preserved between tests.
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() AND table_name NOT IN ('flyway_schema_history', 'skills')",
                String.class);
        for (String table : tables) {
            jdbcTemplate.execute("TRUNCATE TABLE `" + table + "`");
        }
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }

    protected String registerAndGetToken(String email, String role) throws Exception {
        return registerUser(email, role).token();
    }

    protected TestUser registerUser(String email, String role) throws Exception {
        MvcResult result = performRegister(email, role);
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return new TestUser(node.get("accessToken").asText(),
                UUID.fromString(node.get("user").get("id").asText()));
    }

    protected TestEmployer registerEmployer(String email) throws Exception {
        MvcResult result = performRegister(email, "employer");
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return new TestEmployer(node.get("accessToken").asText(),
                UUID.fromString(node.get("user").get("companyId").asText()));
    }

    private MvcResult performRegister(String email, String role) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", "password123",
                "firstName", "First",
                "lastName", "Last",
                "role", role,
                "consent", true));
        return mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
    }

    protected UUID skillId(String name) {
        return jdbcTemplate.queryForObject("SELECT id FROM skills WHERE name = ?", UUID.class, name);
    }
}
