package com.fairfold.analytics.dto;

import java.time.Instant;
import java.util.List;

public record AnalyticsResponse(
        List<FunnelStage> funnel,
        List<TimeToHire> timeToHire,
        List<Override> overrides,
        long pullBacks
) {
    public record FunnelStage(String stage, long count) {}

    public record TimeToHire(String month, long hires, double avgDays) {}

    public record Override(Instant date, String userName, String jobTitle, String alias,
                           String direction, String reason) {}
}
