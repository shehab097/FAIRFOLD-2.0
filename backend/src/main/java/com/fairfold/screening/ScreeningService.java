package com.fairfold.screening;

import com.fairfold.ai.AiGateway;
import com.fairfold.application.Application;
import com.fairfold.application.ApplicationRepository;
import com.fairfold.application.ApplicationStatus;
import com.fairfold.application.ApplicationStatusService;
import com.fairfold.common.errors.ApiException;
import com.fairfold.config.AppProperties;
import com.fairfold.job.Job;
import com.fairfold.job.JobRepository;
import com.fairfold.job.JobStatus;
import com.fairfold.resume.Resume;
import com.fairfold.resume.ResumeRepository;
import com.fairfold.resume.ResumeStatus;
import com.fairfold.screening.dto.ScreeningEstimateResponse;
import com.fairfold.screening.dto.ScreeningProgressResponse;
import com.fairfold.screening.dto.ScreeningStartResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** FR-028: estimate, start, progress and retry for screening runs. */
@Service
public class ScreeningService {

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final ResumeRepository resumeRepository;
    private final ScreeningRunRepository runRepository;
    private final ScreeningItemRepository itemRepository;
    private final ApplicationStatusService statusService;
    private final ScreeningWorker worker;
    private final AiGateway aiGateway;
    private final AppProperties.Ai aiConfig;

    public ScreeningService(JobRepository jobRepository,
                            ApplicationRepository applicationRepository,
                            ResumeRepository resumeRepository,
                            ScreeningRunRepository runRepository,
                            ScreeningItemRepository itemRepository,
                            ApplicationStatusService statusService,
                            ScreeningWorker worker,
                            AiGateway aiGateway,
                            AppProperties properties) {
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.resumeRepository = resumeRepository;
        this.runRepository = runRepository;
        this.itemRepository = itemRepository;
        this.statusService = statusService;
        this.worker = worker;
        this.aiGateway = aiGateway;
        this.aiConfig = properties.ai();
    }

    @Transactional(readOnly = true)
    public ScreeningEstimateResponse estimate(UUID companyId, UUID jobId) {
        requireJob(companyId, jobId);
        List<Application> eligible = eligibleApplications(jobId);
        String mode = aiGateway.activeMode();
        return new ScreeningEstimateResponse(eligible.size(), mode, estimateCost(eligible, mode));
    }

    @Transactional
    public ScreeningStartResponse start(UUID companyId, UUID userId, UUID jobId) {
        Job job = requireJob(companyId, jobId);
        if (job.getStatus() == JobStatus.draft) {
            throw ApiException.conflict("JOB_NOT_SCREENABLE", "Publish the job before screening.");
        }
        if (runRepository.existsByJobIdAndStatus(jobId, RunStatus.running)) {
            throw ApiException.conflict("SCREENING_RUNNING", "A screening run is already in progress.");
        }
        List<Application> eligible = eligibleApplications(jobId);
        if (eligible.isEmpty()) {
            throw ApiException.conflict("NOTHING_TO_SCREEN", "There are no new applicants to screen.");
        }
        String mode = aiGateway.activeMode();
        ScreeningRun run = new ScreeningRun();
        run.setJobId(jobId);
        run.setStartedBy(userId);
        run.setMode(ScreeningMode.valueOf(mode));
        run.setTotal(eligible.size());
        run.setEstimatedCost(estimateCost(eligible, mode));
        run.setStatus(RunStatus.running);
        runRepository.save(run);

        for (Application application : eligible) {
            ScreeningItem item = new ScreeningItem();
            item.setRunId(run.getId());
            item.setApplicationId(application.getId());
            item.setStatus(ItemStatus.queued);
            itemRepository.save(item);
            statusService.changeStatus(application, ApplicationStatus.screening, userId, "Screening started");
        }

        UUID runId = run.getId();
        afterCommit(() -> worker.processRun(runId));
        return new ScreeningStartResponse(runId);
    }

    @Transactional(readOnly = true)
    public ScreeningProgressResponse progress(UUID companyId, UUID jobId) {
        requireJob(companyId, jobId);
        ScreeningRun run = runRepository.findFirstByJobIdOrderByCreatedAtDesc(jobId)
                .orElseThrow(() -> ApiException.notFound("SCREENING_NOT_FOUND",
                        "No screening run has been started for this job."));
        return toProgress(run);
    }

    @Transactional
    public int retry(UUID companyId, UUID jobId) {
        requireJob(companyId, jobId);
        ScreeningRun run = runRepository.findFirstByJobIdOrderByCreatedAtDesc(jobId)
                .orElseThrow(() -> ApiException.notFound("SCREENING_NOT_FOUND",
                        "No screening run has been started for this job."));
        int requeued = itemRepository.requeueFailed(run.getId());
        if (requeued == 0) {
            throw ApiException.conflict("NOTHING_TO_RETRY", "There are no failed items to retry.");
        }
        run.setFailedCount(Math.max(0, run.getFailedCount() - requeued));
        run.setStatus(RunStatus.running);
        run.setFinishedAt(null);
        runRepository.save(run);
        afterCommit(() -> worker.processRun(run.getId()));
        return requeued;
    }

    private Job requireJob(UUID companyId, UUID jobId) {
        return jobRepository.findByIdAndCompanyId(jobId, companyId)
                .orElseThrow(() -> ApiException.notFound("JOB_NOT_FOUND", "Job not found."));
    }

    private List<Application> eligibleApplications(UUID jobId) {
        List<Application> applied = applicationRepository
                .findByJobIdAndStatusOrderByCreatedAtAscIdAsc(jobId, ApplicationStatus.applied);
        List<Application> eligible = new ArrayList<>();
        for (Application application : applied) {
            Resume resume = resumeRepository.findById(application.getResumeId()).orElse(null);
            if (resume != null && resume.getStatus() == ResumeStatus.ready) {
                eligible.add(application);
            }
        }
        return eligible;
    }

    private BigDecimal estimateCost(List<Application> applications, String mode) {
        if ("rule_based".equals(mode)) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Application application : applications) {
            Resume resume = resumeRepository.findById(application.getResumeId()).orElse(null);
            if (resume == null || resume.getStrippedText() == null) {
                continue;
            }
            BigDecimal tokensIn = BigDecimal.valueOf(resume.getStrippedText().length())
                    .divide(BigDecimal.valueOf(4), 4, RoundingMode.HALF_UP)
                    .add(BigDecimal.valueOf(600));
            BigDecimal inputCost = tokensIn.multiply(aiConfig.inputPrice());
            BigDecimal outputCost = BigDecimal.valueOf(400).multiply(aiConfig.outputPrice());
            total = total.add(inputCost).add(outputCost);
        }
        return total.divide(BigDecimal.valueOf(1_000_000), 4, RoundingMode.HALF_UP);
    }

    private ScreeningProgressResponse toProgress(ScreeningRun run) {
        return new ScreeningProgressResponse(
                run.getId(), run.getStatus().name(), run.getMode().name(),
                run.getTotal(), run.getDoneCount(), run.getFailedCount(), run.getCreatedAt());
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
