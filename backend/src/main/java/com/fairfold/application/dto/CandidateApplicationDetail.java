package com.fairfold.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CandidateApplicationDetail(
        UUID id,
        UUID jobId,
        String jobTitle,
        String companyName,
        UUID companyId,
        String city,
        String country,
        String status,
        Instant appliedAt,
        List<HistoryEntry> history,
        Integer score,
        String scoringMode,
        List<Claim> claims,
        List<String> missingSkills,
        String notMatchedRule,
        String gateStatus
) {
    public record HistoryEntry(String status, Instant at) {}

    public record Claim(String requirement, String kind, String verdict, String quote) {}
}
