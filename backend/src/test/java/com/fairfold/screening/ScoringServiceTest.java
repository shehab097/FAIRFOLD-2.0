package com.fairfold.screening;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScoringServiceTest {

    private final ScoringService scoring = new ScoringService();

    private ScoringService.Requirement java() {
        return new ScoringService.Requirement("skill:1", "Java", ClaimKind.required, 3, "Java");
    }

    private ScoringService.Requirement docker() {
        return new ScoringService.Requirement("skill:2", "Docker", ClaimKind.nice_to_have, 1, "Docker");
    }

    private ScoringService.Requirement experience() {
        return new ScoringService.Requirement("experience", "Experience level: mid", ClaimKind.experience, 1, null);
    }

    private String text() {
        return "I build Java services with Docker and deploy them on Linux. Five years of experience.";
    }

    @Test
    void all_met_is_100() {
        var result = scoring.score(
                List.of(java(), docker(), experience()),
                List.of(new ScoringService.ProviderVerdict("skill:1", "met", "I build Java services"),
                        new ScoringService.ProviderVerdict("skill:2", "met", "Java services with Docker"),
                        new ScoringService.ProviderVerdict("experience", "met", "Five years of experience.")),
                text(), ClaimSource.ai);
        assertThat(result.score()).isEqualTo(100);
        assertThat(result.missingSkills()).isEmpty();
    }

    @Test
    void all_not_met_is_0_and_required_skills_are_missing() {
        var result = scoring.score(
                List.of(java(), docker(), experience()),
                List.of(new ScoringService.ProviderVerdict("skill:1", "not_met", null),
                        new ScoringService.ProviderVerdict("skill:2", "not_met", null),
                        new ScoringService.ProviderVerdict("experience", "not_met", null)),
                text(), ClaimSource.ai);
        assertThat(result.score()).isZero();
        assertThat(result.missingSkills()).containsExactly("Java");
    }

    @Test
    void mixed_weights_and_partial_credit_are_rounded() {
        var result = scoring.score(
                List.of(java(), docker(), experience()),
                List.of(new ScoringService.ProviderVerdict("skill:1", "met", "I build Java services"),
                        new ScoringService.ProviderVerdict("skill:2", "not_met", null),
                        new ScoringService.ProviderVerdict("experience", "partial",
                                "Five years of experience.")),
                text(), ClaimSource.ai);
        assertThat(result.score()).isEqualTo(70);
    }

    @Test
    void invented_ai_quote_downgrades_the_verdict() {
        var result = scoring.score(
                List.of(java(), experience()),
                List.of(new ScoringService.ProviderVerdict("skill:1", "met", "I invented this quote"),
                        new ScoringService.ProviderVerdict("experience", "met", "Five years of experience.")),
                text(), ClaimSource.ai);
        var java = result.claims().stream().filter(c -> c.key().equals("skill:1")).findFirst().orElseThrow();
        assertThat(java.verdict()).isEqualTo(Verdict.not_met);
        assertThat(java.quote()).isNull();
        assertThat(result.score()).isEqualTo(25);
        assertThat(result.missingSkills()).containsExactly("Java");
    }

    @Test
    void short_quote_and_unknown_verdict_become_not_met() {
        var result = scoring.score(
                List.of(java(), experience()),
                List.of(new ScoringService.ProviderVerdict("skill:1", "met", "Java"),
                        new ScoringService.ProviderVerdict("experience", "maybe", "Five years of experience.")),
                text(), ClaimSource.ai);
        assertThat(result.claims()).allMatch(c -> c.verdict() == Verdict.not_met);
        assertThat(result.score()).isZero();
    }

    @Test
    void missing_verdict_becomes_not_met() {
        var result = scoring.score(List.of(java(), experience()), List.of(), text(), ClaimSource.ai);
        assertThat(result.claims()).allMatch(c -> c.verdict() == Verdict.not_met);
    }

    @Test
    void rule_based_results_are_labelled_and_not_quote_validated() {
        var result = scoring.score(
                List.of(experience()),
                List.of(new ScoringService.ProviderVerdict("experience", "met", null)),
                text(), ClaimSource.rule_based);
        assertThat(result.score()).isEqualTo(100);
        assertThat(result.claims().get(0).source()).isEqualTo(ClaimSource.rule_based);
    }
}
