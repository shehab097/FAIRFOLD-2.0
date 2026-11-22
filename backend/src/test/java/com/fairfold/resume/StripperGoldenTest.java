package com.fairfold.resume;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The quality gate for BR-05: at least 95% of every "must not appear" string is
 * removed across the 30 fixtures, while every "must appear" skill/title stays.
 */
class StripperGoldenTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ResumeStripper stripper = new ResumeStripper();

    @Test
    void removes_at_least_95_percent_of_personal_details() throws Exception {
        JsonNode expected = readExpected();
        ResumeStripper.Terms terms = seedTerms();

        int total = 0;
        int removed = 0;
        List<String> misses = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();

        var fields = expected.fields();
        while (fields.hasNext()) {
            var entry = fields.next();
            String file = entry.getKey();
            JsonNode data = entry.getValue();
            String text = read("/resumes/" + file);
            ResumeStripper.Identifiers ids = new ResumeStripper.Identifiers(
                    data.path("firstName").asText(),
                    data.path("lastName").asText(),
                    data.path("email").asText(),
                    data.path("phone").asText());
            String stripped = stripper.strip(text, ids, terms).text();
            String haystack = stripped.toLowerCase(Locale.ROOT);

            for (JsonNode item : data.path("mustNotAppear")) {
                total++;
                String value = item.asText();
                if (!haystack.contains(value.toLowerCase(Locale.ROOT))) {
                    removed++;
                } else {
                    misses.add(file + ": " + value);
                }
            }
            for (JsonNode item : data.path("mustAppear")) {
                String value = item.asText();
                if (!haystack.contains(value.toLowerCase(Locale.ROOT))) {
                    missingSkills.add(file + ": " + value);
                }
            }
        }

        double ratio = total == 0 ? 0 : (double) removed / total;
        assertThat(missingSkills).as("skills and titles that must stay").isEmpty();
        assertThat(ratio).as("removed %s of %s strings; misses: %s", removed, total, misses)
                .isGreaterThanOrEqualTo(0.95);
    }

    private JsonNode readExpected() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/resumes/expected.json")) {
            assertThat(stream).isNotNull();
            return objectMapper.readTree(stream);
        }
    }

    private String read(String resource) throws Exception {
        try (InputStream stream = getClass().getResourceAsStream(resource)) {
            assertThat(stream).as(resource).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Reads the seed list from the real migration so the test guards the data too. */
    private ResumeStripper.Terms seedTerms() throws Exception {
        String sql = read("/db/migration/V2_1_2__seed_aliases_terms.sql");
        Pattern pattern = Pattern.compile("\\('(institution|employer)', '([^']+)'\\)");
        Matcher matcher = pattern.matcher(sql);
        List<String> institutions = new ArrayList<>();
        List<String> employers = new ArrayList<>();
        while (matcher.find()) {
            if ("institution".equals(matcher.group(1))) {
                institutions.add(matcher.group(2));
            } else {
                employers.add(matcher.group(2));
            }
        }
        assertThat(institutions).hasSizeGreaterThanOrEqualTo(100);
        assertThat(employers).hasSizeGreaterThanOrEqualTo(100);
        return new ResumeStripper.Terms(institutions, employers);
    }
}
