package com.fairfold.screening;

import com.fairfold.ai.AiGateway;
import com.fairfold.application.Application;
import com.fairfold.application.ApplicationRepository;
import com.fairfold.application.ApplicationStatus;
import com.fairfold.application.ApplicationStatusHistory;
import com.fairfold.application.ApplicationStatusHistoryRepository;
import com.fairfold.application.ApplicationStatusService;
import com.fairfold.application.dto.EmployerApplicantListItem;
import com.fairfold.assessment.Assessment;
import com.fairfold.assessment.AssessmentAttempt;
import com.fairfold.assessment.AssessmentAttemptRepository;
import com.fairfold.assessment.AssessmentRepository;
import com.fairfold.assessment.AttemptStatus;
import com.fairfold.assessment.JobRequiredAssessment;
import com.fairfold.assessment.JobRequiredAssessmentRepository;
import com.fairfold.assessment.ProctorEvent;
import com.fairfold.assessment.ProctorEventRepository;
import com.fairfold.candidate.CandidateProfile;
import com.fairfold.candidate.CandidateProfileRepository;
import com.fairfold.candidate.CandidateSkill;
import com.fairfold.candidate.CandidateSkillRepository;
import com.fairfold.common.audit.AuditService;
import com.fairfold.common.errors.ApiException;
import com.fairfold.common.paging.PageResponse;
import com.fairfold.interview.Interview;
import com.fairfold.interview.InterviewFeedback;
import com.fairfold.interview.InterviewFeedbackRepository;
import com.fairfold.interview.InterviewInterviewer;
import com.fairfold.interview.InterviewInterviewerRepository;
import com.fairfold.interview.InterviewRepository;
import com.fairfold.job.Job;
import com.fairfold.job.JobRepository;
import com.fairfold.resume.Resume;
import com.fairfold.resume.ResumeRepository;
import com.fairfold.screening.dto.DecisionRequest;
import com.fairfold.screening.dto.EmployerReview;
import com.fairfold.screening.dto.ScreeningStartResponse;
import com.fairfold.skill.Skill;
import com.fairfold.skill.SkillRepository;
import com.fairfold.user.User;
import com.fairfold.user.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Employer applicant list, review page, shortlist / reject / pull-back rules.
 * Identity is only returned after a shortlist, and every reveal is audited.
 */
@Service
public class EmployerReviewService {

    private static final int MIN_REASON = 10;

    private final ApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final ApplicationStatusService statusService;
    private final ApplicationClaimRepository claimRepository;
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;
    private final CandidateProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final CandidateAliasRepository aliasRepository;
    private final AliasService aliasService;
    private final ScreeningRunRepository runRepository;
    private final ScreeningItemRepository itemRepository;
    private final ScreeningWorker worker;
    private final AiGateway aiGateway;
    private final AuditService auditService;
    private final JobRequiredAssessmentRepository requiredAssessmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final ProctorEventRepository proctorEventRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final SkillRepository skillRepository;
    private final InterviewRepository interviewRepository;
    private final InterviewInterviewerRepository interviewInterviewerRepository;
    private final InterviewFeedbackRepository interviewFeedbackRepository;
    private final Clock clock;

    public EmployerReviewService(ApplicationRepository applicationRepository,
                                 ApplicationStatusHistoryRepository historyRepository,
                                 ApplicationStatusService statusService,
                                 ApplicationClaimRepository claimRepository,
                                 JobRepository jobRepository,
                                 ResumeRepository resumeRepository,
                                 CandidateProfileRepository profileRepository,
                                 UserRepository userRepository,
                                 CandidateAliasRepository aliasRepository,
                                 AliasService aliasService,
                                 ScreeningRunRepository runRepository,
                                 ScreeningItemRepository itemRepository,
                                 ScreeningWorker worker,
                                 AiGateway aiGateway,
                                 AuditService auditService,
                                 JobRequiredAssessmentRepository requiredAssessmentRepository,
                                 AssessmentRepository assessmentRepository,
                                 AssessmentAttemptRepository attemptRepository,
                                 ProctorEventRepository proctorEventRepository,
                                 CandidateSkillRepository candidateSkillRepository,
                                 SkillRepository skillRepository,
                                 InterviewRepository interviewRepository,
                                 InterviewInterviewerRepository interviewInterviewerRepository,
                                 InterviewFeedbackRepository interviewFeedbackRepository,
                                 Clock clock) {
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
        this.statusService = statusService;
        this.claimRepository = claimRepository;
        this.jobRepository = jobRepository;
        this.resumeRepository = resumeRepository;
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.aliasRepository = aliasRepository;
        this.aliasService = aliasService;
        this.runRepository = runRepository;
        this.itemRepository = itemRepository;
        this.worker = worker;
        this.aiGateway = aiGateway;
        this.auditService = auditService;
        this.requiredAssessmentRepository = requiredAssessmentRepository;
        this.assessmentRepository = assessmentRepository;
        this.attemptRepository = attemptRepository;
        this.proctorEventRepository = proctorEventRepository;
        this.candidateSkillRepository = candidateSkillRepository;
        this.skillRepository = skillRepository;
        this.interviewRepository = interviewRepository;
        this.interviewInterviewerRepository = interviewInterviewerRepository;
        this.interviewFeedbackRepository = interviewFeedbackRepository;
        this.clock = clock;
    }

    @Transactional
    public PageResponse<EmployerApplicantListItem> list(UUID companyId, UUID jobId, String status,
                                                        String gate, String bias, boolean includeNotMatched,
                                                        Pageable pageable) {
        Job job = jobRepository.findByIdAndCompanyId(jobId, companyId)
                .orElseThrow(() -> ApiException.notFound("JOB_NOT_FOUND", "Job not found."));
        ApplicationStatus statusFilter = parseStatus(status);
        GateStatus gateFilter = parseGate(gate);
        BiasStatus biasFilter = parseBias(bias);

        List<Application> applications = new ArrayList<>();
        for (Application application : applicationRepository.findByJobIdOrderByCreatedAtAscIdAsc(jobId)) {
            if (application.getStatus() == ApplicationStatus.not_matched && !includeNotMatched) {
                continue;
            }
            if (statusFilter != null && application.getStatus() != statusFilter) {
                continue;
            }
            if (gateFilter != null && application.getGateStatus() != gateFilter) {
                continue;
            }
            if (biasFilter != null && application.getBiasStatus() != biasFilter) {
                continue;
            }
            applications.add(application);
        }
        applications.sort(Comparator
                .comparing(Application::getScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Application::getCreatedAt)
                .thenComparing(Application::getId));

        int size = Math.max(1, pageable.getPageSize());
        int total = applications.size();
        int from = Math.min(pageable.getPageNumber() * size, total);
        int to = Math.min(from + size, total);
        List<EmployerApplicantListItem> items = new ArrayList<>();
        for (Application application : applications.subList(from, to)) {
            String alias = aliasService.aliasFor(companyId, application.getCandidateId());
            items.add(new EmployerApplicantListItem(
                    application.getId(), alias, alias, application.getScore(),
                    application.getStatus().name(),
                    application.getBiasStatus() == null ? null : application.getBiasStatus().name(),
                    application.getGateStatus().name(),
                    application.getScoringMode() == null ? null : application.getScoringMode().name(),
                    application.getNotMatchedRule(), application.getCreatedAt()));
        }
        return PageResponse.of(items, pageable.getPageNumber(), size, total);
    }

    @Transactional
    public EmployerReview review(UUID companyId, UUID applicationId) {
        Application application = loadApplication(companyId, applicationId);
        Job job = jobRepository.findById(application.getJobId())
                .orElseThrow(() -> ApiException.notFound("JOB_NOT_FOUND", "Job not found."));
        return buildReview(companyId, application, job);
    }

    @Transactional
    public EmployerReview shortlist(UUID companyId, UUID actorId, UUID applicationId, DecisionRequest request) {
        Application application = loadApplication(companyId, applicationId);
        if (application.getStatus() != ApplicationStatus.screened) {
            throw ApiException.conflict("INVALID_STATUS", "Only screened applications can be shortlisted.");
        }
        GateStatus gate = application.getGateStatus();
        if (gate == GateStatus.pending || gate == GateStatus.failed) {
            throw ApiException.conflict("GATE_NOT_PASSED", "This candidate has not passed the assessment gate.");
        }
        Job job = jobRepository.findById(application.getJobId())
                .orElseThrow(() -> ApiException.notFound("JOB_NOT_FOUND", "Job not found."));
        String reason = reason(request);
        boolean below = belowCutoff(application, job);
        if (below && !validReason(reason)) {
            throw reasonRequired("A written reason is required when shortlisting below the cut-off.");
        }
        if (reason != null && reason.length() < MIN_REASON) {
            throw reasonRequired("The reason must be at least 10 characters.");
        }

        application.setShortlistedAt(clock.instant());
        application.setDecidedBy(actorId);
        application.setDecisionReason(reason);
        application.setOverrideDirection(below
                ? OverrideDirection.shortlisted_below_cutoff : OverrideDirection.none);
        applicationRepository.save(application);
        statusService.changeStatus(application, ApplicationStatus.shortlisted, actorId, reason);

        auditService.record(actorId, "identity_reveal", "application", applicationId.toString(),
                "{\"jobId\":\"" + job.getId() + "\"}");
        if (below) {
            auditService.record(actorId, "ai_decision", "application", applicationId.toString(),
                    aiDecisionDetails("shortlisted_below_cutoff", application, job));
        }
        return buildReview(companyId, application, job);
    }

    @Transactional
    public void reject(UUID companyId, UUID actorId, UUID applicationId, DecisionRequest request) {
        Application application = loadApplication(companyId, applicationId);
        if (application.getStatus() != ApplicationStatus.screened
                && application.getStatus() != ApplicationStatus.not_matched
                && application.getStatus() != ApplicationStatus.shortlisted
                && application.getStatus() != ApplicationStatus.interview) {
            throw ApiException.conflict("INVALID_STATUS",
                    "Only screened, not-matched, shortlisted or interview applications can be rejected.");
        }
        Job job = jobRepository.findById(application.getJobId())
                .orElseThrow(() -> ApiException.notFound("JOB_NOT_FOUND", "Job not found."));
        String reason = reason(request);
        boolean cutoffRules = application.getStatus() == ApplicationStatus.screened
                || application.getStatus() == ApplicationStatus.not_matched;
        boolean above = cutoffRules && aboveCutoff(application, job);
        if (above && !validReason(reason)) {
            throw reasonRequired("A written reason is required when rejecting at or above the cut-off.");
        }
        if (reason != null && reason.length() < MIN_REASON) {
            throw reasonRequired("The reason must be at least 10 characters.");
        }

        application.setDecidedBy(actorId);
        application.setDecisionReason(reason);
        application.setOverrideDirection(above
                ? OverrideDirection.rejected_above_cutoff : OverrideDirection.none);
        applicationRepository.save(application);
        statusService.changeStatus(application, ApplicationStatus.rejected, actorId, reason);
        if (above) {
            auditService.record(actorId, "ai_decision", "application", applicationId.toString(),
                    aiDecisionDetails("rejected_above_cutoff", application, job));
        }
    }

    @Transactional
    public ScreeningStartResponse pullBack(UUID companyId, UUID actorId, UUID applicationId) {
        Application application = loadApplication(companyId, applicationId);
        if (application.getStatus() != ApplicationStatus.not_matched) {
            throw ApiException.conflict("INVALID_STATUS", "Only not-matched applications can be pulled back.");
        }
        application.setPulledBackAt(clock.instant());
        application.setPulledBackBy(actorId);
        applicationRepository.save(application);
        statusService.changeStatus(application, ApplicationStatus.screening, actorId, "Pulled back");

        ScreeningMode mode = runRepository.findFirstByJobIdOrderByCreatedAtDesc(application.getJobId())
                .map(ScreeningRun::getMode)
                .orElse("rule_based".equals(aiGateway.activeMode())
                        ? ScreeningMode.rule_based : ScreeningMode.ai);
        ScreeningRun run = new ScreeningRun();
        run.setJobId(application.getJobId());
        run.setStartedBy(actorId);
        run.setMode(mode);
        run.setTotal(1);
        run.setStatus(RunStatus.running);
        runRepository.save(run);
        ScreeningItem item = new ScreeningItem();
        item.setRunId(run.getId());
        item.setApplicationId(applicationId);
        item.setStatus(ItemStatus.queued);
        item.setSkipFilters(true);
        itemRepository.save(item);

        auditService.record(actorId, "pull_back", "application", applicationId.toString(),
                "{\"runId\":\"" + run.getId() + "\"}");
        UUID runId = run.getId();
        afterCommit(() -> worker.processRun(runId));
        return new ScreeningStartResponse(runId);
    }

    private Application loadApplication(UUID companyId, UUID applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> ApiException.notFound("APPLICATION_NOT_FOUND", "Application not found."));
        jobRepository.findByIdAndCompanyId(application.getJobId(), companyId)
                .orElseThrow(() -> ApiException.notFound("APPLICATION_NOT_FOUND", "Application not found."));
        return application;
    }

    private EmployerReview buildReview(UUID companyId, Application application, Job job) {
        String alias = aliasRepository.findByCompanyIdAndCandidateId(companyId, application.getCandidateId())
                .map(CandidateAlias::getAlias)
                .orElseGet(() -> aliasService.aliasFor(companyId, application.getCandidateId()));
        List<EmployerReview.Claim> claims = claimRepository
                .findByApplicationIdOrderByIdAsc(application.getId()).stream()
                .map(c -> new EmployerReview.Claim(
                        c.getRequirementText(), c.getKind().name(), c.getVerdict().name(),
                        c.getQuote(), c.getSource().name()))
                .toList();
        List<EmployerReview.HistoryEntry> history = historyRepository
                .findByApplicationIdOrderByCreatedAtAsc(application.getId()).stream()
                .map(h -> new EmployerReview.HistoryEntry(h.getToStatus().name(), h.getCreatedAt()))
                .toList();
        Resume resume = resumeRepository.findById(application.getResumeId()).orElse(null);
        EmployerReview.Identity identity = identity(application);
        return new EmployerReview(
                application.getId(), application.getJobId(), job.getTitle(), alias,
                application.getStatus().name(), application.getScore(),
                application.getScoringMode() == null ? null : application.getScoringMode().name(),
                application.getBiasStatus() == null ? null : application.getBiasStatus().name(),
                application.getBiasNote(), application.getGateStatus().name(), job.getCutoffScore(),
                application.getMissingSkills(), claims,
                resume == null ? null : resume.getStrippedText(),
                application.getNotMatchedRule(), history, identity,
                assessmentSummaries(application), verifiedSkills(application.getCandidateId()),
                interviewSummaries(application));
    }

    private List<EmployerReview.InterviewSummary> interviewSummaries(Application application) {
        List<Interview> interviews = interviewRepository
                .findByApplicationIdOrderByScheduledAtAsc(application.getId());
        List<EmployerReview.InterviewSummary> result = new ArrayList<>();
        for (Interview interview : interviews) {
            List<EmployerReview.Interviewer> interviewers = new ArrayList<>();
            for (InterviewInterviewer row : interviewInterviewerRepository
                    .findByIdInterviewId(interview.getId())) {
                userRepository.findById(row.getId().getUserId()).ifPresent(user ->
                        interviewers.add(new EmployerReview.Interviewer(
                                user.getId(),
                                (user.getFirstName() + " " + user.getLastName()).trim())));
            }
            result.add(new EmployerReview.InterviewSummary(
                    interview.getId(), interview.getScheduledAt(),
                    interview.getDurationMinutes(), interview.getMeetingLink(), interviewers,
                    interviewFeedbackRepository.countByInterviewId(interview.getId())));
        }
        return result;
    }

    private List<EmployerReview.AssessmentSummary> assessmentSummaries(Application application) {
        List<JobRequiredAssessment> requirements = requiredAssessmentRepository
                .findByJobId(application.getJobId());
        if (requirements.isEmpty()) {
            return List.of();
        }
        List<UUID> assessmentIds = requirements.stream()
                .map(JobRequiredAssessment::getAssessmentId).toList();
        Map<UUID, Assessment> assessments = assessmentRepository.findAllById(assessmentIds).stream()
                .collect(java.util.stream.Collectors.toMap(Assessment::getId, Function.identity()));
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
                    AssessmentAttempt best = own.stream()
                            .filter(a -> a.getScore() != null)
                            .max(java.util.Comparator.comparingInt(AssessmentAttempt::getScore)
                                    .thenComparing(AssessmentAttempt::getSubmittedAt,
                                            java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder())))
                            .orElse(null);
                    Integer bestScore = best == null ? null : best.getScore();
                    boolean passed = bestScore != null && bestScore >= requirement.getMinScore();
                    Instant lastAttemptAt = own.stream()
                            .map(AssessmentAttempt::getSubmittedAt)
                            .filter(java.util.Objects::nonNull)
                            .max(java.util.Comparator.naturalOrder()).orElse(null);
                    EmployerReview.Focus focus = focus(best);
                    return new EmployerReview.AssessmentSummary(
                            assessments.containsKey(requirement.getAssessmentId())
                                    ? assessments.get(requirement.getAssessmentId()).getTitle() : null,
                            requirement.getMinScore(), bestScore, passed, lastAttemptAt, focus);
                })
                .toList();
    }

    private EmployerReview.Focus focus(AssessmentAttempt attempt) {
        if (attempt == null || !Boolean.TRUE.equals(attempt.getProctoringConsent())) {
            return new EmployerReview.Focus(false, 0, 0, null);
        }
        List<ProctorEvent> events = proctorEventRepository
                .findByAttemptIdOrderByOccurredAtAsc(attempt.getId());
        int totalSeconds = events.stream().mapToInt(ProctorEvent::getDurationSeconds).sum();
        return new EmployerReview.Focus(true, events.size(), totalSeconds, attempt.getExplanation());
    }

    private List<String> verifiedSkills(UUID candidateId) {
        List<CandidateSkill> verified = candidateSkillRepository
                .findByUserIdOrderByCreatedAtAsc(candidateId).stream()
                .filter(CandidateSkill::isVerified)
                .toList();
        if (verified.isEmpty()) {
            return List.of();
        }
        Map<UUID, String> names = skillRepository.findAllById(verified.stream()
                        .map(CandidateSkill::getSkillId).toList()).stream()
                .collect(java.util.stream.Collectors.toMap(Skill::getId, Skill::getName));
        return verified.stream()
                .map(skill -> names.get(skill.getSkillId()))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private EmployerReview.Identity identity(Application application) {
        if (application.getShortlistedAt() == null) {
            return null;
        }
        User user = userRepository.findById(application.getCandidateId()).orElse(null);
        if (user == null) {
            return null;
        }
        CandidateProfile profile = profileRepository.findById(application.getCandidateId()).orElse(null);
        return new EmployerReview.Identity(user.getFirstName(), user.getLastName(), user.getEmail(),
                profile == null ? null : profile.getPhone());
    }

    private boolean belowCutoff(Application application, Job job) {
        return job.getCutoffScore() != null && application.getScore() != null
                && application.getScore() < job.getCutoffScore();
    }

    private boolean aboveCutoff(Application application, Job job) {
        return job.getCutoffScore() != null && application.getScore() != null
                && application.getScore() >= job.getCutoffScore();
    }

    private String reason(DecisionRequest request) {
        if (request == null || request.reason() == null) {
            return null;
        }
        String trimmed = request.reason().strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean validReason(String reason) {
        return reason != null && reason.length() >= MIN_REASON;
    }

    private ApiException reasonRequired(String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "REASON_REQUIRED", message);
    }

    private String aiDecisionDetails(String direction, Application application, Job job) {
        return "{\"direction\":\"" + direction + "\",\"score\":" + application.getScore()
                + ",\"cutoff\":" + job.getCutoffScore() + "}";
    }

    private ApplicationStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ApplicationStatus.valueOf(value.trim().toLowerCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("INVALID_STATUS", "Unknown application status.");
        }
    }

    private GateStatus parseGate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return GateStatus.valueOf(value.trim().toLowerCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("INVALID_GATE", "Unknown gate status.");
        }
    }

    private BiasStatus parseBias(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return BiasStatus.valueOf(value.trim().toLowerCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("INVALID_BIAS", "Unknown bias status.");
        }
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
