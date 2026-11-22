package com.fairfold.screening.dto;

public record EmployerDashboardResponse(
        long openJobs,
        long newApplications,
        long awaitingScreening,
        long biasFlags,
        Integer interviewsThisWeek,
        Object quota
) {
}
