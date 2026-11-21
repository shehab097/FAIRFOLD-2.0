package com.fairfold.candidate.dto;

import java.util.List;
import java.util.UUID;

public record DashboardResponse(
        Checklist checklist,
        List<LatestApplication> latestApplications
) {
    public record Checklist(boolean profileSaved, boolean resumeUploaded, boolean skillsAdded) {}

    public record LatestApplication(UUID id, UUID jobId, String jobTitle, String status, String appliedAt) {}
}
