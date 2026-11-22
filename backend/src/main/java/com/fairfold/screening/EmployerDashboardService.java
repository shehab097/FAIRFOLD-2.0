package com.fairfold.screening;

import com.fairfold.application.Application;
import com.fairfold.application.ApplicationRepository;
import com.fairfold.application.ApplicationStatus;
import com.fairfold.job.Job;
import com.fairfold.job.JobRepository;
import com.fairfold.job.JobStatus;
import com.fairfold.screening.dto.EmployerDashboardResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** FR-046 employer dashboard counters plus a small activity snapshot. */
@Service
public class EmployerDashboardService {

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final AliasService aliasService;
    private final Clock clock;

    public EmployerDashboardService(JobRepository jobRepository,
                                    ApplicationRepository applicationRepository,
                                    AliasService aliasService,
                                    Clock clock) {
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.aliasService = aliasService;
        this.clock = clock;
    }

    @Transactional
    public EmployerDashboardResponse dashboard(UUID companyId) {
        long openJobs = jobRepository.countByCompanyIdAndStatus(companyId, JobStatus.active);
        long newApplications = applicationRepository.countNewApplications(
                companyId, clock.instant().minus(7, ChronoUnit.DAYS));
        long awaiting = applicationRepository.countByCompanyAndStatus(
                companyId, ApplicationStatus.applied);
        long biasFlags = applicationRepository.countByCompanyStatusAndBias(
                companyId, ApplicationStatus.screened, BiasStatus.flagged);

        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        for (Object[] row : applicationRepository.countByStatusForCompany(companyId)) {
            counts.put((ApplicationStatus) row[0], ((Number) row[1]).longValue());
        }
        EmployerDashboardResponse.Pipeline pipeline = new EmployerDashboardResponse.Pipeline(
                count(counts, ApplicationStatus.applied),
                count(counts, ApplicationStatus.screening),
                count(counts, ApplicationStatus.screened),
                count(counts, ApplicationStatus.shortlisted),
                count(counts, ApplicationStatus.interview),
                count(counts, ApplicationStatus.hired),
                count(counts, ApplicationStatus.rejected),
                count(counts, ApplicationStatus.not_matched));

        List<Application> recent = applicationRepository.findRecentForCompany(
                companyId, PageRequest.of(0, 5));
        Map<UUID, Job> recentJobs = recent.isEmpty() ? Map.of()
                : jobRepository.findAllById(recent.stream().map(Application::getJobId).toList())
                .stream().collect(Collectors.toMap(Job::getId, Function.identity()));
        List<EmployerDashboardResponse.RecentApplication> recentApplications = recent.stream()
                .map(application -> {
                    Job job = recentJobs.get(application.getJobId());
                    return new EmployerDashboardResponse.RecentApplication(
                            application.getId(), application.getJobId(),
                            job == null ? null : job.getTitle(),
                            aliasService.aliasFor(companyId, application.getCandidateId()),
                            application.getScore(),
                            application.getStatus().name(),
                            application.getBiasStatus() == null ? null : application.getBiasStatus().name(),
                            application.getCreatedAt());
                })
                .toList();

        List<EmployerDashboardResponse.JobActivity> jobs = jobRepository
                .findByCompanyIdAndStatusOrderByUpdatedAtDesc(
                        companyId, JobStatus.active, PageRequest.of(0, 5))
                .stream()
                .map(job -> new EmployerDashboardResponse.JobActivity(
                        job.getId(), job.getTitle(), job.getStatus().name(),
                        applicationRepository.countByJobId(job.getId()),
                        applicationRepository.countByJobIdAndStatus(job.getId(), ApplicationStatus.applied),
                        applicationRepository.countByJobIdAndStatus(job.getId(), ApplicationStatus.screened)))
                .toList();

        return new EmployerDashboardResponse(
                openJobs, newApplications, awaiting, biasFlags, null, null,
                pipeline, recentApplications, jobs);
    }

    private long count(Map<ApplicationStatus, Long> counts, ApplicationStatus status) {
        return counts.getOrDefault(status, 0L);
    }
}
