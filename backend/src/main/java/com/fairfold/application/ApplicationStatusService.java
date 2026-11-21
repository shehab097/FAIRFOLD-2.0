package com.fairfold.application;

import com.fairfold.notification.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Central place for status changes on applications. Every transition must go
 * through here so that history rows and notifications are written consistently.
 */
@Service
public class ApplicationStatusService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final NotificationService notificationService;

    public ApplicationStatusService(ApplicationRepository applicationRepository,
                                    ApplicationStatusHistoryRepository historyRepository,
                                    NotificationService notificationService) {
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Application changeStatus(Application application, ApplicationStatus to, UUID actorId, String reason) {
        ApplicationStatus from = application.getStatus();
        if (from == to) {
            return application;
        }
        application.setStatus(to);
        applicationRepository.save(application);
        historyRepository.save(new ApplicationStatusHistory(
                application.getId(), from, to, actorId, reason));
        notificationService.notify(
                application.getCandidateId(),
                "status_changed",
                "Application status updated",
                "Your application is now " + to.name().replace('_', ' ') + ".",
                "/candidate/applications/" + application.getId());
        return application;
    }
}
