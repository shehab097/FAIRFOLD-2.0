package com.fairfold.coaching;

import com.fairfold.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** FR-060: practice data is isolated from hiring data. */
class CoachingIsolationTest extends AbstractIntegrationTest {

    @Test
    void Fr060_practice_tables_have_no_foreign_key_path_to_jobs_or_applications() {
        Long violations = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME IN ('practice_questions', 'practice_sessions')
                  AND REFERENCED_TABLE_NAME IN ('jobs', 'applications')
                """, Long.class);
        assertThat(violations).isZero();

        List<String> referenced = jdbcTemplate.queryForList("""
                SELECT DISTINCT REFERENCED_TABLE_NAME FROM information_schema.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME IN ('practice_questions', 'practice_sessions')
                  AND REFERENCED_TABLE_NAME IS NOT NULL
                """, String.class);
        assertThat(referenced).doesNotContain("jobs", "applications");
    }

    @Test
    void Fr060_hiring_packages_do_not_import_the_coaching_package() throws Exception {
        for (String pkg : List.of("application", "screening", "job")) {
            try (Stream<Path> files = Files.walk(Path.of("src/main/java/com/fairfold", pkg))) {
                for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    assertThat(Files.readString(file))
                            .as(file.toString())
                            .doesNotContain("com.fairfold.coaching");
                }
            }
        }
        try (Stream<Path> files = Files.walk(Path.of("src/main/java/com/fairfold/coaching"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertThat(source).as(file.toString())
                        .doesNotContain("com.fairfold.job", "com.fairfold.application",
                                "com.fairfold.screening");
            }
        }
    }
}
