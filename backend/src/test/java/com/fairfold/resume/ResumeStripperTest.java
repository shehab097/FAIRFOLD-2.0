package com.fairfold.resume;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResumeStripperTest {

    private final ResumeStripper stripper = new ResumeStripper();

    private final ResumeStripper.Identifiers ids =
            new ResumeStripper.Identifiers("Tanvir", "Ahmed", "tanvir.ahmed@example.com", "+8801712345678");

    private final ResumeStripper.Terms terms = new ResumeStripper.Terms(
            List.of("University of Dhaka", "North South University"),
            List.of("Grameenphone", "BRAC Bank", "Therap BD"));

    @Test
    void Fr011_removes_known_identifiers_case_insensitively() {
        String input = """
                TANVIR AHMED
                tanvir.ahmed@example.com | +8801712345678
                Tanvir built APIs. Ahmed writes tests.
                """;
        String out = stripper.strip(input, ids, terms).text();
        assertThat(out).doesNotContain("Tanvir", "TANVIR", "Ahmed", "tanvir.ahmed@example.com", "+8801712345678");
        assertThat(out).contains("[NAME]").contains("[EMAIL]").contains("[PHONE]");
    }

    @Test
    void Fr011_replaces_header_name_with_two_to_four_title_case_words() {
        String out = stripper.strip("Nusrat Jahan\nSoftware Engineer\n", ResumeStripper.Identifiers.none(), terms).text();
        assertThat(out).startsWith("[NAME]");
        assertThat(out).doesNotContain("Nusrat Jahan");
    }

    @Test
    void Fr011_deletes_personal_detail_lines() {
        String input = """
                Profile
                Date of Birth: 12 March 1994
                Father's Name: Karim Ahmed
                Religion: Islam
                NID: 1990123456789
                Present Address: House 4, Dhaka
                Blood Group: B+
                Software Engineer
                """;
        String out = stripper.strip(input, ResumeStripper.Identifiers.none(), terms).text();
        assertThat(out).doesNotContain("Date of Birth", "Father", "Religion", "NID",
                "Present Address", "Blood Group");
        assertThat(out).contains("Software Engineer");
    }

    @Test
    void Fr011_replaces_emails_phones_and_urls() {
        String input = """
                Contact me at someone@example.com or call +880 1712-345678.
                Portfolio: https://example.dev/me and github.com/someone/project
                """;
        String out = stripper.strip(input, ResumeStripper.Identifiers.none(), terms).text();
        assertThat(out).contains("[EMAIL]").contains("[PHONE]").contains("[URL]");
        assertThat(out).doesNotContain("someone@example.com", "1712", "example.dev", "github.com");
    }

    @Test
    void Fr011_replaces_seed_employers_and_institutions_consistently() {
        String input = """
                Education
                North South University, 2014 - 2018
                Experience
                Grameenphone
                Then Grameenphone again, and BRAC Bank.
                """;
        ResumeStripper.Result result = stripper.strip(input, ResumeStripper.Identifiers.none(), terms);
        assertThat(result.text()).doesNotContain("North South", "Grameenphone", "BRAC Bank");
        assertThat(result.text().split("\\[INSTITUTION_1]", -1)).hasSize(2);
        assertThat(result.text().split("\\[EMPLOYER_1]", -1)).hasSize(3);
        assertThat(result.mappings()).hasSize(3);
        assertThat(result.mappings()).anyMatch(m -> m.placeholder().equals("INSTITUTION_1")
                && m.originalValue().equals("North South University"));
    }

    @Test
    void Fr011_replaces_pattern_employers_and_institutions() {
        String input = """
                Worked at Acme Technologies and Orbital Systems Ltd.
                Studied at Springfield College of Science.
                """;
        String out = stripper.strip(input, ResumeStripper.Identifiers.none(), ResumeStripper.Terms.empty()).text();
        assertThat(out).doesNotContain("Acme Technologies", "Orbital Systems", "Springfield College");
        assertThat(out).contains("[EMPLOYER_1]").contains("[INSTITUTION_1]");
    }

    @Test
    void Fr011_education_years_become_year_placeholder() {
        String out = stripper.strip("""
                BSc in Computer Science
                University of Dhaka
                2014 - 2018
                CGPA 3.75/4.00
                """, ResumeStripper.Identifiers.none(), terms).text();
        assertThat(out).contains("[YEAR]");
        assertThat(out).doesNotContain("2014", "2018");
        assertThat(out).contains("3.75/4.00");
    }

    @Test
    void Fr011_work_dates_become_duration_and_are_saved_on_employer() {
        ResumeStripper.Result result = stripper.strip("""
                Software Engineer
                Grameenphone
                Jan 2020 - Mar 2023
                Built services.
                """, ResumeStripper.Identifiers.none(), terms);
        assertThat(result.text()).contains("(3 yrs 2 mos)");
        assertThat(result.text()).doesNotContain("2020", "2023");
        ResumeStripper.Mapping mapping = result.mappings().stream()
                .filter(m -> m.kind().equals("employer")).findFirst().orElseThrow();
        assertThat(mapping.startMonth()).isEqualTo(LocalDate.of(2020, 1, 1));
        assertThat(mapping.endMonth()).isEqualTo(LocalDate.of(2023, 3, 1));
        assertThat(mapping.current()).isFalse();
    }

    @Test
    void Fr011_current_job_sets_is_current() {
        ResumeStripper.Result result = stripper.strip("""
                Senior Engineer
                Therap BD
                June 2022 - Present
                """, ResumeStripper.Identifiers.none(), terms);
        ResumeStripper.Mapping mapping = result.mappings().stream()
                .filter(m -> m.kind().equals("employer")).findFirst().orElseThrow();
        assertThat(mapping.current()).isTrue();
        assertThat(mapping.endMonth()).isNull();
        assertThat(mapping.startMonth()).isEqualTo(LocalDate.of(2022, 6, 1));
    }

    @Test
    void Fr057_institution_and_employer_names_never_survive() {
        String input = """
                EDUCATION
                BSc, North South University, 2016
                EXPERIENCE
                Software Engineer, Grameenphone, 2018 - 2021
                """;
        ResumeStripper.Result result = stripper.strip(input, ResumeStripper.Identifiers.none(), terms);
        assertThat(result.text()).doesNotContain("North South", "Grameenphone");
        assertThat(result.mappings()).hasSize(2);
    }

    @Test
    void Fr011_parenthesised_year_range_is_not_treated_as_a_phone() {
        String out = stripper.strip("""
                Senior Engineer - Acme Technologies (2018 - 2021)
                """, ResumeStripper.Identifiers.none(), ResumeStripper.Terms.empty()).text();
        assertThat(out).contains("(3 yrs 0 mos)");
        assertThat(out).doesNotContain("[PHONE]");
    }

    @Test
    void IdentifierScan_blocks_name_email_and_phone_tail() {
        assertThat(IdentifierScan.leaks("Tanvir writes Java", ids)).isTrue();
        assertThat(IdentifierScan.leaks("mail tanvir.ahmed@example.com", ids)).isTrue();
        assertThat(IdentifierScan.leaks("call 2345678", ids)).isTrue();
        assertThat(IdentifierScan.leaks("Java, Spring Boot and Docker", ids)).isFalse();
    }
}
