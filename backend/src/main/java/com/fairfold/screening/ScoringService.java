package com.fairfold.screening;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** BR-20: quote validation, credit and score. Pure and unit-testable. */
import org.springframework.stereotype.Service;

@Service
public final class ScoringService {

    public record Requirement(
            String key,
            String text,
            ClaimKind kind,
            int weight,
            String skillName
    ) {}

    public record ProviderVerdict(String key, String verdict, String quote) {}

    public record Claim(
            String key,
            String text,
            ClaimKind kind,
            int weight,
            Verdict verdict,
            String quote,
            ClaimSource source
    ) {}

    public record Result(int score, List<Claim> claims, List<String> missingSkills) {}

    public Result score(List<Requirement> requirements, List<ProviderVerdict> verdicts,
                        String strippedText, ClaimSource source) {
        Map<String, ProviderVerdict> byKey = new LinkedHashMap<>();
        if (verdicts != null) {
            for (ProviderVerdict verdict : verdicts) {
                if (verdict != null && verdict.key() != null) {
                    byKey.put(verdict.key(), verdict);
                }
            }
        }
        String haystack = normalise(strippedText);
        List<Claim> claims = new ArrayList<>();
        for (Requirement requirement : requirements) {
            ProviderVerdict provided = byKey.get(requirement.key());
            Verdict verdict = Verdict.parse(provided == null ? null : provided.verdict());
            String quote = provided == null ? null : trimToNull(provided.quote());
            if (source == ClaimSource.ai && verdict != Verdict.not_met && !validQuote(quote, haystack)) {
                verdict = Verdict.not_met;
                quote = null;
            }
            claims.add(new Claim(requirement.key(), requirement.text(), requirement.kind(),
                    requirement.weight(), verdict, quote, source));
        }
        return new Result(scoreClaims(claims), claims, missingSkills(claims, requirements));
    }

    public int scoreClaims(List<Claim> claims) {
        double weighted = 0;
        double total = 0;
        for (Claim claim : claims) {
            double credit = switch (claim.verdict()) {
                case met -> 1.0;
                case partial -> 0.5;
                case not_met -> 0.0;
            };
            weighted += claim.weight() * credit;
            total += claim.weight();
        }
        if (total == 0) {
            return 0;
        }
        return (int) Math.round(100.0 * weighted / total);
    }

    public List<String> missingSkills(List<Claim> claims, List<Requirement> requirements) {
        Map<String, String> namesByKey = new LinkedHashMap<>();
        for (Requirement requirement : requirements) {
            if (requirement.kind() == ClaimKind.required && requirement.skillName() != null) {
                namesByKey.put(requirement.key(), requirement.skillName());
            }
        }
        List<String> missing = new ArrayList<>();
        for (Claim claim : claims) {
            if (claim.kind() == ClaimKind.required
                    && claim.verdict() == Verdict.not_met
                    && namesByKey.containsKey(claim.key())) {
                missing.add(namesByKey.get(claim.key()));
            }
        }
        return missing;
    }

    public boolean validQuote(String quote, String normalisedText) {
        if (quote == null) {
            return false;
        }
        String clean = quote.strip();
        if (clean.length() < 10 || clean.length() > 300) {
            return false;
        }
        return normalisedText.contains(normalise(clean));
    }

    public String normalise(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
