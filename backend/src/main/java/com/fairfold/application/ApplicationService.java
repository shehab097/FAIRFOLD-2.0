package com.fairfold.application;

import com.fairfold.application.dto.CandidateApplicationDetail;
import com.fairfold.application.dto.CandidateApplicationListItem;
import com.fairfold.application.dto.EmployerApplicantListItem;
import com.fairfold.common.audit.AuditService;
import com.fairfold.common.errors.ApiException;
import com.fairfold.common.paging.PageResponse;
import com.fairfold.company.Company;
import com.fairfold.company.CompanyRepository;
import com.fairfold.job.Job;
import com.fairfold.job.JobRepository;
import com.fairfold.job.JobStatus;
import com.fairfold.notification.NotificationService;
import com.fairfold.resume.Resume;
import com.fairfold.resume.ResumeRepository;
import com.fairfold.resume.ResumeStatus;
import com.fairfold.screening.AliasService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final ResumeRepository resumeRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final AliasService aliasService;

    public ApplicationService(ApplicationRepository applicationRepository,
                              ApplicationStatusHistoryRepository historyRepository,
                              JobRepository jobRepository,
                              CompanyRepository companyRepository,
                              ResumeRepository resumeRepository,
                              NotificationService notificationService,
                              AuditService auditService,
                              AliasService aliasService) {
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
        this.jobRepository = jobRepository;
        this.companyRepository = companyRepository;
        this.resumeRepository = resumeRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.aliasService = aliasService;
    }

    @Transactional
    public CandidateApplicationDetail apply(UUID candidateId, UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> ApiException.notFound("JOB_NOT_FOUND", "Job not found."));
        if (job.getStatus() != JobStatus.active) {
            throw ApiException.conflict("JOB_NOT_ACTIVE", "This job is not accepting applications.");
        }
        Resume resume = resumeRepository.findByUserIdAndCurrentTrue(candidateId)
                .orElseThrow(() -> ApiException.conflict("RESUME_REQUIRED",
                        "Upload a resume before applying."));
        if (resume.getStatus() != ResumeStatus.uploaded && resume.getStatus() != ResumeStatus.ready) {
            throw ApiException.conflict("RESUME_REQUIRED", "Upload a resume before applying.");
        }
        if (applicationRepository.existsByJobIdAndCandidateId(jobId, candidateId)) {
            throw ApiException.conflict("ALREADY_APPLIED", "You have already applied to this job.");
        }

        Application application = new Application();
        application.setJobId(jobId);
        application.setCandidateId(candidateId);
        application.setResumeId(resume.getId());
        application.setStatus(ApplicationStatus.applied);
        applicationRepository.save(application);

        historyRepository.save(new ApplicationStatusHistory(
                application.getId(), null, ApplicationStatus.applied, candidateId, null));

        aliasService.aliasFor(job.getCompanyId(), candidateId);
        auditService.record(candidateId, "application_created", "application",
                application.getId().toString(), "{}");

        notificationService.notify(candidateId, "application_submitted", "Application submitted",
                "You applied to " + job.getTitle() + ".",
                "/candidate/applications/" + application.getId());
        notificationService.notify(job.getCreatedBy(), "application_received", "New application",
                "A candidate applied to " + job.getTitle() + ".",
                "/employer/jobs/" + jobId);

        return toDetail(application, job);
    }

    @Transactional(readOnly = true)
    public PageResponse<CandidateApplicationListItem> listForCandidate(UUID candidateId, String status, Pageable pageable) {
        ApplicationStatus statusParam = parseStatus(status);
        Page<Application> page = applicationRepository.findByCandidate(candidateId, statusParam, pageable);

        Map<UUID, Job> jobs = jobsBy(page.getContent());
        return PageResponse.of(
                page.getContent().stream()
                        .map(a -> toListItem(a, jobs.get(a.getJobId())))
                        .toList(),
                page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public CandidateApplicationDetail getForCandidate(UUID candidateId, UUID applicationId) {
        Application application = applicationRepository.findByIdAndCandidateId(applicationId, candidateId)
                .orElseThrow(() -> ApiException.notFound("APPLICATION_NOT_FOUND", "Application not found."));
        Job job = jobRepository.findById(application.getJobId())
                .orElseThrow(() -> ApiException.notFound("JOB_NOT_FOUND", "Job not found."));
        return toDetail(application, job);
    }

    @Transactional
    public PageResponse<EmployerApplicantListItem> listForEmployer(UUID companyId, UUID jobId, Pageable pageable) {
        jobRepository.findByIdAndCompanyId(jobId, companyId)
                .orElseThrow(() -> ApiException.notFound("JOB_NOT_FOUND", "Job not found."));

        List<Application> all = applicationRepository.findByJobIdOrderByCreatedAtAscIdAsc(jobId);
        int total = all.size();
        int from = Math.min(pageable.getPageNumber() * pageable.getPageSize(), total);
        int to = Math.min(from + pageable.getPageSize(), total);
        List<Application> slice = all.subList(from, to);

        List<EmployerApplicantListItem> items = new java.util.ArrayList<>();
        for (int i = 0; i < slice.size(); i++) {
            Application application = slice.get(i);
            String alias = aliasService.aliasFor(companyId, application.getCandidateId());
            items.add(new EmployerApplicantListItem(
                    application.getId(),
                    alias,
                    alias,
                    application.getScore(),
                    application.getStatus().name(),
                    application.getBiasStatus() == null ? null : application.getBiasStatus().name(),
                    application.getGateStatus().name(),
                    application.getScoringMode() == null ? null : application.getScoringMode().name(),
                    application.getNotMatchedRule(),
                    application.getCreatedAt()));
        }
        return PageResponse.of(items, pageable.getPageNumber(), pageable.getPageSize(), total);
    }

    // --- helpers -----------------------------------------------------------

    private CandidateApplicationListItem toListItem(Application application, Job job) {
        Company company = job == null ? null : resolvePublicCompany(job.getCompanyId());
        return new CandidateApplicationListItem(
                application.getId(), application.getJobId(),
                job == null ? null : job.getTitle(),
                company == null ? null : company.getName(),
                company == null ? null : company.getId(),
                application.getStatus().name(), application.getCreatedAt());
    }

    private CandidateApplicationDetail toDetail(Application application, Job job) {
        List<CandidateApplicationDetail.HistoryEntry> history = historyRepository
                .findByApplicationIdOrderByCreatedAtAsc(application.getId()).stream()
                .map(h -> new CandidateApplicationDetail.HistoryEntry(
                        h.getToStatus().name(), h.getCreatedAt()))
                .toList();
        Company company = resolvePublicCompany(job.getCompanyId());
        return new CandidateApplicationDetail(
                application.getId(), application.getJobId(), job.getTitle(),
                company == null ? null : company.getName(),
                company == null ? null : company.getId(),
                job.getCity(), job.getCountry(),
                application.getStatus().name(), application.getCreatedAt(), history);
    }

    private Map<UUID, Job> jobsBy(List<Application> applications) {
        List<UUID> ids = applications.stream().map(Application::getJobId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return jobRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Job::getId, Function.identity()));
    }

    private Company resolvePublicCompany(UUID companyId) {
        return companyRepository.findById(companyId)
                .filter(Company::isShowNamePublicly)
                .orElse(null);
    }

    private ApplicationStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ApplicationStatus.valueOf(status.trim().toLowerCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("INVALID_STATUS", "Unknown application status.");
        }
    }
}
