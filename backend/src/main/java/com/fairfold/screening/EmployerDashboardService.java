package com.fairfold.screening;

import com.fairfold.application.ApplicationRepository;
import com.fairfold.application.ApplicationStatus;
import com.fairfold.job.JobRepository;
import com.fairfold.job.JobStatus;
import com.fairfold.screening.dto.EmployerDashboardResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** FR-046 employer dashboard counters. */
@Service
public class EmployerDashboardService {

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final Clock clock;

    public EmployerDashboardService(JobRepository jobRepository,
                                    ApplicationRepository applicationRepository,
                                    Clock clock) {
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public EmployerDashboardResponse dashboard(UUID companyId) {
        long openJobs = jobRepository.countByCompanyIdAndStatus(companyId, JobStatus.active);
        long newApplications = applicationRepository.countNewApplications(
                companyId, clock.instant().minus(7, ChronoUnit.DAYS));
        long awaiting = applicationRepository.countByCompanyAndStatus(
                companyId, ApplicationStatus.applied);
        long biasFlags = applicationRepository.countByCompanyStatusAndBias(
                companyId, ApplicationStatus.screened, BiasStatus.flagged);
        return new EmployerDashboardResponse(openJobs, newApplications, awaiting, biasFlags, null, null);
    }
}
