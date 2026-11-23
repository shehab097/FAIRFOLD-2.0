package com.fairfold.interview;

import com.fairfold.application.Application;
import com.fairfold.application.ApplicationRepository;
import com.fairfold.application.ApplicationStatus;
import com.fairfold.application.ApplicationStatusService;
import com.fairfold.common.audit.AuditService;
import com.fairfold.common.errors.ApiException;
import com.fairfold.common.mail.MailService;
import com.fairfold.common.paging.PageResponse;
import com.fairfold.interview.dto.InterviewDetail;
import com.fairfold.interview.dto.InterviewFeedbackDto;
import com.fairfold.interview.dto.InterviewFeedbackRequest;
import com.fairfold.interview.dto.InterviewListItem;
import com.fairfold.interview.dto.InterviewPackDto;
import com.fairfold.interview.dto.ScheduleInterviewRequest;
import com.fairfold.job.Job;
import com.fairfold.job.JobRepository;
import com.fairfold.notification.NotificationService;
import com.fairfold.user.Role;
import com.fairfold.user.User;
import com.fairfold.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final InterviewInterviewerRepository interviewerRepository;
    private final InterviewFeedbackRepository feedbackRepository;
    private final InterviewPackService packService;
    private final ApplicationRepository applicationRepository;
    private final ApplicationStatusService statusService;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final MailService mailService;
    private final AuditService auditService;
    private final Clock clock;

    public InterviewService(InterviewRepository interviewRepository,
                            InterviewInterviewerRepository interviewerRepository,
                            InterviewFeedbackRepository feedbackRepository,
                            InterviewPackService packService,
                            ApplicationRepository applicationRepository,
                            ApplicationStatusService statusService,
                            JobRepository jobRepository,
                            UserRepository userRepository,
                            NotificationService notificationService,
                            MailService mailService,
                            AuditService auditService,
                            Clock clock) {
        this.interviewRepository = interviewRepository;
        this.interviewerRepository = interviewerRepository;
        this.feedbackRepository = feedbackRepository;
        this.packService = packService;
        this.applicationRepository = applicationRepository;
        this.statusService = statusService;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.mailService = mailService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public InterviewDetail schedule(UUID companyId, UUID actorId, UUID applicationId,
                                    ScheduleInterviewRequest request) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> ApiException.notFound("APPLICATION_NOT_FOUND", "Application not found."));
        Job job = jobRepository.findByIdAndCompanyId(application.getJobId(), companyId)
                .orElseThrow(() -> ApiException.notFound("APPLICATION_NOT_FOUND", "Application not found."));
        if (application.getStatus() != ApplicationStatus.shortlisted
                && application.getStatus() != ApplicationStatus.interview) {
            throw ApiException.conflict("INVALID_STATUS",
                    "Only shortlisted candidates can have an interview scheduled.");
        }
        if (request == null || request.scheduledAt() == null) {
            throw ApiException.badRequest("INVALID_SCHEDULE", "Choose a date and time.");
        }
        if (!request.scheduledAt().isAfter(clock.instant())) {
            throw ApiException.badRequest("INVALID_SCHEDULE", "The interview must be in the future.");
        }
        if (request.durationMinutes() == null || request.durationMinutes() < 15
                || request.durationMinutes() > 240) {
            throw ApiException.badRequest("INVALID_SCHEDULE",
                    "Duration must be between 15 and 240 minutes.");
        }
        String link = request.meetingLink() == null ? "" : request.meetingLink().strip();
        if (!link.startsWith("https://")) {
            throw ApiException.badRequest("INVALID_MEETING_LINK",
                    "The meeting link must start with https://.");
        }
        List<User> interviewers = validateInterviewers(companyId, request.interviewerIds());

        Interview interview = new Interview();
        interview.setApplicationId(application.getId());
        interview.setScheduledAt(request.scheduledAt());
        interview.setDurationMinutes(request.durationMinutes());
        interview.setMeetingLink(link);
        interview.setCreatedBy(actorId);
        interviewRepository.save(interview);

        for (User interviewer : interviewers) {
            interviewerRepository.save(new InterviewInterviewer(
                    new InterviewInterviewerId(interview.getId(), interviewer.getId())));
        }

        if (application.getStatus() == ApplicationStatus.shortlisted) {
            statusService.changeStatus(application, ApplicationStatus.interview, actorId, null);
        }

        User candidate = userRepository.findById(application.getCandidateId()).orElse(null);
        String when = interview.getScheduledAt().toString();
        if (candidate != null) {
            notificationService.notify(candidate.getId(), "interview_scheduled", "Interview scheduled",
                    "Your interview for " + job.getTitle() + " is scheduled for " + when + ".",
                    "/candidate/applications/" + application.getId());
            mailService.send(candidate.getEmail(), "FairFold interview scheduled",
                    "Your interview for " + job.getTitle() + " is scheduled for " + when
                            + " (" + interview.getDurationMinutes() + " minutes). Meeting link: "
                            + interview.getMeetingLink());
        }
        for (User interviewer : interviewers) {
            notificationService.notify(interviewer.getId(), "interview_scheduled",
                    "Interview assigned",
                    "Interview for " + job.getTitle() + " on " + when + ".",
                    "/employer/interviews/" + interview.getId());
            mailService.send(interviewer.getEmail(), "FairFold interview assigned",
                    "You are interviewing for " + job.getTitle() + " on " + when
                            + " (" + interview.getDurationMinutes() + " minutes). Meeting link: "
                            + interview.getMeetingLink());
        }
        auditService.record(actorId, "interview_scheduled", "interview",
                interview.getId().toString(), "{\"applicationId\":\"" + application.getId() + "\"}");
        return detail(companyId, actorId, Role.EMPLOYER_HR, interview.getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<InterviewListItem> list(UUID companyId, UUID userId, Role role,
                                                String tab, Pageable pageable) {
        boolean past = "past".equalsIgnoreCase(tab);
        Page<Interview> page;
        if (role == Role.INTERVIEWER) {
            page = past
                    ? interviewRepository.findPastForInterviewer(userId, clock.instant(), pageable)
                    : interviewRepository.findUpcomingForInterviewer(userId, clock.instant(), pageable);
        } else {
            page = past
                    ? interviewRepository.findPastForCompany(companyId, clock.instant(), pageable)
                    : interviewRepository.findUpcomingForCompany(companyId, clock.instant(), pageable);
        }
        List<InterviewListItem> items = page.getContent().stream()
                .map(interview -> toListItem(interview, userId))
                .toList();
        return PageResponse.of(items, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public InterviewDetail detail(UUID companyId, UUID userId, Role role, UUID interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> ApiException.notFound("INTERVIEW_NOT_FOUND", "Interview not found."));
        Context context = context(interview);
        authorize(companyId, userId, role, interview, context);
        return toDetail(interview, context, role, userId);
    }

    @Transactional
    public InterviewFeedbackDto submitFeedback(UUID companyId, UUID userId, Role role,
                                               UUID interviewId, InterviewFeedbackRequest request) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> ApiException.notFound("INTERVIEW_NOT_FOUND", "Interview not found."));
        Context context = context(interview);
        authorize(companyId, userId, role, interview, context);
        if (!interviewerRepository.existsByIdInterviewIdAndIdUserId(interviewId, userId)) {
            throw ApiException.forbidden("Only assigned interviewers can submit feedback.");
        }
        InterviewPackDto pack = packService.forJob(context.job().getId());
        Map<String, Integer> scores = request == null || request.scores() == null
                ? Map.of() : request.scores();
        if (!pack.rubric().isEmpty()) {
            for (InterviewPackDto.RubricItem item : pack.rubric()) {
                Integer score = scores.get(item.id());
                if (score == null || score < 1 || score > 5) {
                    throw ApiException.badRequest("INVALID_SCORE",
                            "Score every rubric item from 1 to 5.");
                }
            }
        }
        for (Integer score : scores.values()) {
            if (score == null || score < 1 || score > 5) {
                throw ApiException.badRequest("INVALID_SCORE", "Scores must be between 1 and 5.");
            }
        }
        FeedbackRecommendation recommendation;
        try {
            recommendation = FeedbackRecommendation.valueOf(
                    request.recommendation().strip().toLowerCase());
        } catch (RuntimeException e) {
            throw ApiException.badRequest("INVALID_RECOMMENDATION",
                    "Recommendation must be strong_yes, yes, no or strong_no.");
        }

        InterviewFeedback feedback = feedbackRepository
                .findByInterviewIdAndInterviewerId(interviewId, userId)
                .orElseGet(() -> {
                    InterviewFeedback created = new InterviewFeedback();
                    created.setInterviewId(interviewId);
                    created.setInterviewerId(userId);
                    return created;
                });
        feedback.setScores(scores);
        String comments = request.comments() == null ? null : request.comments().strip();
        feedback.setComments(comments == null || comments.isEmpty() ? null : comments);
        feedback.setRecommendation(recommendation);
        feedbackRepository.save(feedback);
        return toFeedbackDto(feedback);
    }

    @Transactional
    public void hire(UUID companyId, UUID actorId, UUID applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> ApiException.notFound("APPLICATION_NOT_FOUND", "Application not found."));
        Job job = jobRepository.findByIdAndCompanyId(application.getJobId(), companyId)
                .orElseThrow(() -> ApiException.notFound("APPLICATION_NOT_FOUND", "Application not found."));
        if (application.getStatus() != ApplicationStatus.interview) {
            throw ApiException.conflict("INVALID_STATUS",
                    "Only interviewed candidates can be marked hired.");
        }
        statusService.changeStatus(application, ApplicationStatus.hired, actorId, null);
        notificationService.notify(application.getCandidateId(), "hired", "You were hired",
                "You were hired for " + job.getTitle() + ".",
                "/candidate/applications/" + application.getId());
        auditService.record(actorId, "application_hired", "application",
                application.getId().toString(), "{}");
    }

    private List<User> validateInterviewers(UUID companyId, List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            throw ApiException.badRequest("INVALID_INTERVIEWER", "Choose at least one interviewer.");
        }
        Set<UUID> unique = new LinkedHashSet<>(ids);
        List<User> users = userRepository.findAllById(unique);
        if (users.size() != unique.size()) {
            throw ApiException.badRequest("INVALID_INTERVIEWER", "One or more interviewers do not exist.");
        }
        for (User user : users) {
            if (user.getDisabledAt() != null || !companyId.equals(user.getCompanyId())
                    || user.getRole() == Role.CANDIDATE || user.getRole() == Role.ADMIN) {
                throw ApiException.badRequest("INVALID_INTERVIEWER",
                        "Every interviewer must be an active member of your company.");
            }
        }
        return users;
    }

    private record Context(Application application, Job job) {
    }

    private Context context(Interview interview) {
        Application application = applicationRepository.findById(interview.getApplicationId())
                .orElseThrow(() -> ApiException.notFound("APPLICATION_NOT_FOUND", "Application not found."));
        Job job = jobRepository.findById(application.getJobId())
                .orElseThrow(() -> ApiException.notFound("JOB_NOT_FOUND", "Job not found."));
        return new Context(application, job);
    }

    private void authorize(UUID companyId, UUID userId, Role role, Interview interview, Context context) {
        if (role == Role.INTERVIEWER) {
            if (!interviewerRepository.existsByIdInterviewIdAndIdUserId(interview.getId(), userId)) {
                throw ApiException.notFound("INTERVIEW_NOT_FOUND", "Interview not found.");
            }
            return;
        }
        if (!context.job().getCompanyId().equals(companyId)) {
            throw ApiException.notFound("INTERVIEW_NOT_FOUND", "Interview not found.");
        }
    }

    private InterviewListItem toListItem(Interview interview, UUID userId) {
        Context context = context(interview);
        User candidate = userRepository.findById(context.application().getCandidateId()).orElse(null);
        List<InterviewListItem.InterviewerRef> interviewers = interviewerRepository
                .findByIdInterviewId(interview.getId()).stream()
                .map(row -> userRepository.findById(row.getId().getUserId()).orElse(null))
                .filter(user -> user != null)
                .map(user -> new InterviewListItem.InterviewerRef(
                        user.getId(), (user.getFirstName() + " " + user.getLastName()).trim()))
                .toList();
        boolean submitted = feedbackRepository
                .findByInterviewIdAndInterviewerId(interview.getId(), userId).isPresent();
        return new InterviewListItem(
                interview.getId(), interview.getScheduledAt(),
                candidate == null ? null : (candidate.getFirstName() + " " + candidate.getLastName()).trim(),
                context.job().getTitle(), interviewers, submitted);
    }

    private InterviewDetail toDetail(Interview interview, Context context, Role role, UUID userId) {
        User candidate = userRepository.findById(context.application().getCandidateId()).orElse(null);
        List<InterviewDetail.InterviewerRef> interviewers = new ArrayList<>();
        for (InterviewInterviewer row : interviewerRepository.findByIdInterviewId(interview.getId())) {
            userRepository.findById(row.getId().getUserId()).ifPresent(user ->
                    interviewers.add(new InterviewDetail.InterviewerRef(
                            user.getId(), (user.getFirstName() + " " + user.getLastName()).trim(),
                            user.getRole().name().toLowerCase())));
        }
        InterviewFeedback mine = feedbackRepository
                .findByInterviewIdAndInterviewerId(interview.getId(), userId).orElse(null);
        List<InterviewDetail.FeedbackView> all = null;
        if (role == Role.EMPLOYER_HR || role == Role.EMPLOYER_MANAGER) {
            all = new ArrayList<>();
            for (InterviewFeedback feedback : feedbackRepository
                    .findByInterviewIdOrderByIdAsc(interview.getId())) {
                all.add(toFeedbackView(feedback));
            }
        }
        return new InterviewDetail(
                interview.getId(), interview.getScheduledAt(), interview.getDurationMinutes(),
                interview.getMeetingLink(),
                candidate == null ? null : new InterviewDetail.CandidateRef(
                        candidate.getFirstName(), candidate.getLastName()),
                context.application().getId(), context.job().getId(), context.job().getTitle(),
                interviewers,
                packService.forJob(context.job().getId()),
                mine == null ? null : toFeedbackView(mine), all);
    }

    private InterviewDetail.FeedbackView toFeedbackView(InterviewFeedback feedback) {
        User interviewer = feedback.getInterviewerId() == null ? null
                : userRepository.findById(feedback.getInterviewerId()).orElse(null);
        return new InterviewDetail.FeedbackView(
                feedback.getInterviewerId(),
                interviewer == null ? null
                        : (interviewer.getFirstName() + " " + interviewer.getLastName()).trim(),
                feedback.getScores(), feedback.getComments(),
                feedback.getRecommendation().name());
    }

    private InterviewFeedbackDto toFeedbackDto(InterviewFeedback feedback) {
        return new InterviewFeedbackDto(
                feedback.getInterviewerId(), feedback.getScores(), feedback.getComments(),
                feedback.getRecommendation().name());
    }
}
