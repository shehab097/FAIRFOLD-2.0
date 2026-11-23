package com.fairfold.application;

import com.fairfold.application.dto.CandidateApplicationDetail;
import com.fairfold.application.dto.CandidateApplicationListItem;
import com.fairfold.application.dto.CandidateAssessmentRequirement;
import com.fairfold.application.dto.MyApplicationResponse;
import com.fairfold.assessment.Assessment;
import com.fairfold.assessment.AssessmentAttempt;
import com.fairfold.assessment.AssessmentAttemptRepository;
import com.fairfold.assessment.AssessmentRepository;
import com.fairfold.assessment.AttemptStatus;
import com.fairfold.assessment.GateService;
import com.fairfold.assessment.JobRequiredAssessment;
import com.fairfold.assessment.JobRequiredAssessmentRepository;
import com.fairfold.common.audit.AuditService;
import com.fairfold.common.errors.ApiException;
import com.fairfold.common.paging.PageResponse;
import com.fairfold.company.Company;
import com.fairfold.company.CompanyRepository;
import com.fairfold.interview.InterviewRepository;
import com.fairfold.job.Job;
import com.fairfold.job.JobRepository;
import com.fairfold.job.JobStatus;
import com.fairfold.notification.NotificationService;
import com.fairfold.resume.Resume;
import com.fairfold.resume.ResumeRepository;
import com.fairfold.resume.ResumeStatus;
import com.fairfold.screening.ApplicationClaim;
import com.fairfold.screening.ApplicationClaimRepository;
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
    private final ApplicationClaimRepository claimRepository;
    private final JobRequiredAssessmentRepository requiredAssessmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final GateService gateService;
    private final InterviewRepository interviewRepository;

    public ApplicationService(ApplicationRepository applicationRepository,
                              ApplicationStatusHistoryRepository historyRepository,
                              JobRepository jobRepository,
                              CompanyRepository companyRepository,
                              ResumeRepository resumeRepository,
                              NotificationService notificationService,
                              AuditService auditService,
                              AliasService aliasService,
                              ApplicationClaimRepository claimRepository,
                              JobRequiredAssessmentRepository requiredAssessmentRepository,
                              AssessmentRepository assessmentRepository,
                              AssessmentAttemptRepository attemptRepository,
                              GateService gateService,
                              InterviewRepository interviewRepository) {
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
        this.jobRepository = jobRepository;
        this.companyRepository = companyRepository;
        this.resumeRepository = resumeRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.aliasService = aliasService;
        this.claimRepository = claimRepository;
        this.requiredAssessmentRepository = requiredAssessmentRepository;
        this.assessmentRepository = assessmentRepository;
        this.attemptRepository = attemptRepository;
        this.gateService = gateService;
        this.interviewRepository = interviewRepository;
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

        gateService.recompute(application.getId());
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
    public MyApplicationResponse myApplication(UUID candidateId, UUID jobId) {
        return applicationRepository.findByJobIdAndCandidateId(jobId, candidateId)
                .map(application -> new MyApplicationResponse(
                        true, application.getId(), application.getStatus().name(), application.getCreatedAt()))
                .orElseGet(() -> new MyApplicationResponse(false, null, null, null));
    }

    @Transactional(readOnly = true)
    public CandidateApplicationDetail getForCandidate(UUID candidateId, UUID applicationId) {
        Application application = applicationRepository.findByIdAndCandidateId(applicationId, candidateId)
                .orElseThrow(() -> ApiException.notFound("APPLICATION_NOT_FOUND", "Application not found."));
        Job job = jobRepository.findById(application.getJobId())
                .orElseThrow(() -> ApiException.notFound("JOB_NOT_FOUND", "Job not found."));
        return toDetail(application, job);
    }

    @Transactional(readOnly = true)
    public List<CandidateAssessmentRequirement> requiredAssessmentsForCandidate(UUID candidateId) {
        List<CandidateAssessmentRequirement> result = new java.util.ArrayList<>();
        for (Application application : applicationRepository.findByCandidateId(candidateId)) {
            if (application.getStatus() == ApplicationStatus.rejected
                    || application.getStatus() == ApplicationStatus.hired) {
                continue;
            }
            Job job = jobRepository.findById(application.getJobId()).orElse(null);
            if (job == null) {
                continue;
            }
            List<JobRequiredAssessment> requirements = requiredAssessmentRepository
                    .findByJobId(job.getId());
            if (requirements.isEmpty()) {
                continue;
            }
            List<UUID> ids = requirements.stream()
                    .map(JobRequiredAssessment::getAssessmentId).toList();
            Map<UUID, Assessment> assessments = assessmentRepository.findAllById(ids).stream()
                    .collect(Collectors.toMap(Assessment::getId, Function.identity()));
            List<AssessmentAttempt> attempts = attemptRepository
                    .findByUserIdAndAssessmentIdInAndStatusNot(
                            candidateId, ids, AttemptStatus.in_progress);
            for (JobRequiredAssessment requirement : requirements) {
                List<AssessmentAttempt> own = attempts.stream()
                        .filter(a -> a.getAssessmentId().equals(requirement.getAssessmentId()))
                        .toList();
                Integer best = own.stream().map(AssessmentAttempt::getScore)
                        .filter(score -> score != null).max(Integer::compareTo).orElse(null);
                String status = best != null && best >= requirement.getMinScore()
                        ? "passed" : own.isEmpty() ? "pending" : "failed";
                Assessment assessment = assessments.get(requirement.getAssessmentId());
                result.add(new CandidateAssessmentRequirement(
                        application.getId(), job.getId(), job.getTitle(),
                        requirement.getAssessmentId(),
                        assessment == null ? null : assessment.getTitle(),
                        requirement.getMinScore(), best, status));
            }
        }
        result.sort(java.util.Comparator
                .comparing((CandidateAssessmentRequirement r) -> "passed".equals(r.status()))
                .thenComparing(CandidateAssessmentRequirement::assessmentTitle,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));
        return result;
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
        List<CandidateApplicationDetail.Claim> claims = claimRepository
                .findByApplicationIdOrderByIdAsc(application.getId()).stream()
                .map(this::toCandidateClaim)
                .toList();
        return new CandidateApplicationDetail(
                application.getId(), application.getJobId(), job.getTitle(),
                company == null ? null : company.getName(),
                company == null ? null : company.getId(),
                job.getCity(), job.getCountry(),
                application.getStatus().name(), application.getCreatedAt(), history,
                application.getScore(),
                application.getScoringMode() == null ? null : application.getScoringMode().name(),
                claims, application.getMissingSkills(), application.getNotMatchedRule(),
                application.getGateStatus().name(),
                requiredAssessments(application,job),
                interviewRepository.findByApplicationIdOrderByScheduledAtAsc(application.getId())
                        .stream()
                        .map(i -> new CandidateApplicationDetail.InterviewRef(
                                i.getScheduledAt(), i.getDurationMinutes(), i.getMeetingLink()))
                        .toList());
    }

    private List<CandidateApplicationDetail.RequiredAssessment> requiredAssessments(
            Application application, Job job) {
        List<JobRequiredAssessment> requirements = requiredAssessmentRepository.findByJobId(job.getId());
        if (requirements.isEmpty()) {
            return List.of();
        }
        List<UUID> assessmentIds = requirements.stream()
                .map(JobRequiredAssessment::getAssessmentId).toList();
        Map<UUID, Assessment> assessments = assessmentRepository.findAllById(assessmentIds).stream()
                .collect(Collectors.toMap(Assessment::getId, Function.identity()));
        List<AssessmentAttempt> attempts = attemptRepository
                .findByUserIdAndAssessmentIdInAndStatusNot(
                        application.getCandidateId(), assessmentIds, AttemptStatus.in_progress);
        return requirements.stream()
                .sorted(java.util.Comparator.comparing(r -> assessments.containsKey(r.getAssessmentId())
                        ? assessments.get(r.getAssessmentId()).getTitle() : ""))
                .map(requirement -> {
                    List<AssessmentAttempt> own = attempts.stream()
                            .filter(a -> a.getAssessmentId().equals(requirement.getAssessmentId()))
                            .toList();
                    Integer best = own.stream().map(AssessmentAttempt::getScore)
                            .filter(score -> score != null).max(Integer::compareTo).orElse(null);
                    String status = best != null && best >= requirement.getMinScore()
                            ? "passed" : own.isEmpty() ? "pending" : "failed";
                    return new CandidateApplicationDetail.RequiredAssessment(
                            requirement.getAssessmentId(),
                            assessments.containsKey(requirement.getAssessmentId())
                                    ? assessments.get(requirement.getAssessmentId()).getTitle() : null,
                            requirement.getMinScore(),
                            best,
                            status);
                })
                .toList();
    }

    private CandidateApplicationDetail.Claim toCandidateClaim(ApplicationClaim claim) {
        return new CandidateApplicationDetail.Claim(
                claim.getRequirementText(), claim.getKind().name(),
                claim.getVerdict().name(), claim.getQuote());
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
