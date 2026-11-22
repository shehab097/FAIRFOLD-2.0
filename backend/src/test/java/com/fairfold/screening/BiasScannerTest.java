package com.fairfold.screening;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BiasScannerTest {

    private final ScoringService scoring = new ScoringService();
    private final BiasScanner scanner = new BiasScanner(scoring);

    private ScoringService.Requirement required(String key, String name, int weight) {
        return new ScoringService.Requirement(key, name, ClaimKind.required, weight, name);
    }

    @Test
    void claim_with_gendered_quote_is_removed_and_score_recomputed() {
        var claims = List.of(
                new ScoringService.Claim("skill:1", "Java", ClaimKind.required, 3,
                        Verdict.met, "She led the Java team.", ClaimSource.ai),
                new ScoringService.Claim("skill:2", "Docker", ClaimKind.required, 3,
                        Verdict.met, "Built Docker images for services.", ClaimSource.ai));
        var result = scanner.scan(claims);
        assertThat(result.status()).isEqualTo(BiasStatus.flagged);
        assertThat(result.note()).isEqualTo("Removed 1 claim(s) that referenced a protected trait or proxy");
        assertThat(result.claims()).hasSize(1);
        assertThat(result.claims().get(0).key()).isEqualTo("skill:2");
        assertThat(result.score()).isEqualTo(100);
    }

    @Test
    void institution_placeholder_in_quote_is_flagged() {
        var claims = List.of(
                new ScoringService.Claim("skill:1", "Java", ClaimKind.required, 3,
                        Verdict.partial, "Studied at [INSTITUTION_1] and used Java.", ClaimSource.ai));
        var result = scanner.scan(claims);
        assertThat(result.status()).isEqualTo(BiasStatus.flagged);
        assertThat(result.claims()).isEmpty();
        assertThat(result.score()).isZero();
    }

    @Test
    void clear_claims_stay_clear() {
        var claims = List.of(
                new ScoringService.Claim("skill:1", "Java", ClaimKind.required, 3,
                        Verdict.met, "Built Java services with Docker.", ClaimSource.ai));
        var result = scanner.scan(claims);
        assertThat(result.status()).isEqualTo(BiasStatus.clear);
        assertThat(result.note()).isNull();
        assertThat(result.score()).isEqualTo(100);
    }

    @Test
    void not_met_claims_are_never_bias_scanned() {
        var claims = List.of(
                new ScoringService.Claim("skill:1", "Java", ClaimKind.required, 3,
                        Verdict.not_met, "She never used Java.", ClaimSource.ai));
        var result = scanner.scan(claims);
        assertThat(result.status()).isEqualTo(BiasStatus.clear);
        assertThat(result.claims()).hasSize(1);
    }
}
