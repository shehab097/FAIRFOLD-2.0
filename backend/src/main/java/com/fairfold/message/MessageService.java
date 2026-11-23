package com.fairfold.message;

import com.fairfold.application.Application;
import com.fairfold.application.ApplicationRepository;
import com.fairfold.application.ApplicationStatus;
import com.fairfold.common.errors.ApiException;
import com.fairfold.common.ratelimit.RateLimitService;
import com.fairfold.job.Job;
import com.fairfold.job.JobRepository;
import com.fairfold.message.dto.MessageDto;
import com.fairfold.notification.NotificationRepository;
import com.fairfold.notification.NotificationService;
import com.fairfold.user.Role;
import com.fairfold.user.User;
import com.fairfold.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Application messaging. Messages are never sent to AI and never written to
 * audit details. A thread opens after the first shortlist.
 */
@Service
public class MessageService {

    private static final int MAX_BODY = 2000;

    private final MessageRepository messageRepository;
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final RateLimitService rateLimitService;
    private final Clock clock;

    public MessageService(MessageRepository messageRepository,
                          ApplicationRepository applicationRepository,
                          JobRepository jobRepository,
                          UserRepository userRepository,
                          NotificationRepository notificationRepository,
                          NotificationService notificationService,
                          RateLimitService rateLimitService,
                          Clock clock) {
        this.messageRepository = messageRepository;
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
        this.rateLimitService = rateLimitService;
        this.clock = clock;
    }

    @Transactional
    public List<MessageDto> thread(UUID userId, UUID companyId, Role role, UUID applicationId,
                                   UUID afterId) {
        Context context = load(userId, companyId, role, applicationId);
        if (context.application().getShortlistedAt() == null) {
            throw ApiException.forbidden("Messaging opens after the employer shortlists this application.");
        }
        Instant now = clock.instant();
        messageRepository.markReadFor(applicationId, userId, now);
        List<Message> messages = messageRepository
                .findByApplicationIdOrderByCreatedAtAscIdAsc(applicationId);
        if (afterId != null) {
            int index = -1;
            for (int i = 0; i < messages.size(); i++) {
                if (messages.get(i).getId().equals(afterId)) {
                    index = i;
                    break;
                }
            }
            if (index >= 0) {
                messages = messages.subList(index + 1, messages.size());
            }
        }
        List<MessageDto> result = new ArrayList<>();
        for (Message message : messages) {
            result.add(toDto(message, userId));
        }
        return result;
    }

    @Transactional
    public MessageDto send(UUID userId, UUID companyId, Role role, UUID applicationId, String body) {
        Context context = load(userId, companyId, role, applicationId);
        if (context.application().getShortlistedAt() == null) {
            throw ApiException.forbidden("Messaging opens after the employer shortlists this application.");
        }
        ApplicationStatus status = context.application().getStatus();
        if (status != ApplicationStatus.shortlisted
                && status != ApplicationStatus.interview
                && status != ApplicationStatus.hired) {
            throw ApiException.conflict("THREAD_CLOSED", "This conversation is closed.");
        }
        String clean = body == null ? "" : body.strip();
        if (clean.isEmpty() || clean.length() > MAX_BODY) {
            throw ApiException.badRequest("INVALID_MESSAGE",
                    "A message must be between 1 and 2000 characters.");
        }
        rateLimitService.check("messages:" + userId, 10, Duration.ofMinutes(1), 60);

        Message message = new Message();
        message.setApplicationId(applicationId);
        message.setSenderId(userId);
        message.setBody(clean);
        messageRepository.save(message);

        notifyOtherSide(userId, role, context, applicationId);
        return toDto(message, userId);
    }

    private void notifyOtherSide(UUID senderId, Role senderRole, Context context, UUID applicationId) {
        Job job = context.job();
        String employerLink = "/employer/jobs/" + job.getId() + "/applications/" + applicationId;
        String candidateLink = "/candidate/applications/" + applicationId;
        if (senderRole == Role.CANDIDATE) {
            Set<Role> employerRoles = Set.of(Role.EMPLOYER_HR, Role.EMPLOYER_MANAGER);
            for (User member : userRepository
                    .findByCompanyIdAndDisabledAtIsNullOrderByFirstNameAscLastNameAsc(job.getCompanyId())) {
                if (employerRoles.contains(member.getRole())) {
                    notifyOnce(member.getId(), employerLink);
                }
            }
        } else {
            notifyOnce(context.application().getCandidateId(), candidateLink);
        }
    }

    private void notifyOnce(UUID recipientId, String link) {
        if (notificationRepository.existsByUserIdAndTypeAndLinkAndReadAtIsNull(
                recipientId, "message", link)) {
            return;
        }
        notificationService.notify(recipientId, "message", "New message",
                "You have a new message about an application.", link);
    }

    private MessageDto toDto(Message message, UUID readerId) {
        String senderName = message.getSenderId() == null ? "Deleted user"
                : userRepository.findById(message.getSenderId())
                .map(user -> (user.getFirstName() + " " + user.getLastName()).trim())
                .orElse("Deleted user");
        return new MessageDto(
                message.getId(),
                message.getSenderId() != null && message.getSenderId().equals(readerId),
                senderName,
                message.getBody() == null ? "" : message.getBody(),
                message.getCreatedAt());
    }

    private record Context(Application application, Job job) {
    }

    private Context load(UUID userId, UUID companyId, Role role, UUID applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> ApiException.notFound("APPLICATION_NOT_FOUND", "Application not found."));
        Job job = jobRepository.findById(application.getJobId())
                .orElseThrow(() -> ApiException.notFound("APPLICATION_NOT_FOUND", "Application not found."));
        if (role == Role.CANDIDATE) {
            if (!application.getCandidateId().equals(userId)) {
                throw ApiException.notFound("APPLICATION_NOT_FOUND", "Application not found.");
            }
        } else if (companyId == null || !companyId.equals(job.getCompanyId())) {
            throw ApiException.notFound("APPLICATION_NOT_FOUND", "Application not found.");
        }
        return new Context(application, job);
    }
}
