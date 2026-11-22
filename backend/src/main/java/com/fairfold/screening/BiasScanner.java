package com.fairfold.screening;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** BR-21: removes claims that reference protected traits or proxies. */
@Service
public class BiasScanner {

    private final List<Pattern> terms;
    private final ScoringService scoringService;

    public BiasScanner(ScoringService scoringService) {
        this.scoringService = scoringService;
        this.terms = loadTerms();
    }

    public record Result(int score, List<ScoringService.Claim> claims, BiasStatus status, String note) {}

    public Result scan(List<ScoringService.Claim> claims) {
        List<ScoringService.Claim> kept = new ArrayList<>();
        int removed = 0;
        for (ScoringService.Claim claim : claims) {
            if ((claim.verdict() == Verdict.met || claim.verdict() == Verdict.partial)
                    && (matches(claim.text()) || matches(claim.quote()))) {
                removed++;
                continue;
            }
            kept.add(claim);
        }
        int score = scoringService.scoreClaims(kept);
        if (removed > 0) {
            return new Result(score, kept, BiasStatus.flagged,
                    "Removed " + removed + " claim(s) that referenced a protected trait or proxy");
        }
        return new Result(score, kept, BiasStatus.clear, null);
    }

    private boolean matches(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (Pattern term : terms) {
            if (term.matcher(value).find()) {
                return true;
            }
        }
        return false;
    }

    private List<Pattern> loadTerms() {
        List<Pattern> compiled = new ArrayList<>();
        try (InputStream stream = getClass().getResourceAsStream("/bias/bias_terms.txt")) {
            if (stream == null) {
                return compiled;
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String term = line.strip();
                    if (!term.isEmpty() && !term.startsWith("#")) {
                        compiled.add(Pattern.compile(term, Pattern.CASE_INSENSITIVE));
                    }
                }
            }
        } catch (IOException ignored) {
            // An unreadable term list must not break screening; it simply flags nothing.
        }
        return compiled;
    }
}
