package com.fairfold.screening.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EmployerReview(
        UUID id,
        UUID jobId,
        String jobTitle,
        String alias,
        String status,
        Integer score,
        String scoringMode,
        String biasStatus,
        String biasNote,
        String gateStatus,
        Integer cutoffScore,
        List<String> missingSkills,
        List<Claim> claims,
        String strippedText,
        String notMatchedRule,
        List<HistoryEntry> history,
        Identity identity
) {
    public record Claim(String requirement, String kind, String verdict, String quote, String source) {}

    public record HistoryEntry(String status, Instant at) {}

    public record Identity(String firstName, String lastName, String email, String phone) {}
}
