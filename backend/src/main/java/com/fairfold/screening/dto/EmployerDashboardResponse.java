package com.fairfold.screening.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EmployerDashboardResponse(
        long openJobs,
        long newApplications,
        long awaitingScreening,
        long biasFlags,
        Integer interviewsThisWeek,
        Object quota,
        Pipeline pipeline,
        List<RecentApplication> recentApplications,
        List<JobActivity> jobs
) {
    public record Pipeline(
            long applied,
            long screening,
            long screened,
            long shortlisted,
            long interview,
            long hired,
            long rejected,
            long notMatched
    ) {}

    public record RecentApplication(
            UUID id,
            UUID jobId,
            String jobTitle,
            String alias,
            Integer score,
            String status,
            String biasStatus,
            Instant appliedAt
    ) {}

    public record JobActivity(
            UUID id,
            String title,
            String status,
            long applicants,
            long awaitingScreening,
            long screened
    ) {}
}
