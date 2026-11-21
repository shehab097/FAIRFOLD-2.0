package com.fairfold.notification;

import com.fairfold.common.errors.ApiException;
import com.fairfold.common.paging.PageResponse;
import com.fairfold.notification.dto.NotificationResponse;
import com.fairfold.notification.dto.UnreadCountResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final Clock clock;

    public NotificationService(NotificationRepository notificationRepository, Clock clock) {
        this.notificationRepository = notificationRepository;
        this.clock = clock;
    }

    /** Creates an in-app notification for a user. Ignores a null user id. */
    @Transactional
    public void notify(UUID userId, String type, String title, String body, String link) {
        if (userId == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setLink(link);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(UUID userId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.of(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(UUID userId) {
        return new UnreadCountResponse(notificationRepository.countByUserIdAndReadAtIsNull(userId));
    }

    @Transactional
    public NotificationResponse markRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> ApiException.notFound("NOTIFICATION_NOT_FOUND", "Notification not found."));
        if (notification.getReadAt() == null) {
            notification.setReadAt(clock.instant());
        }
        return toResponse(notification);
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllRead(userId, clock.instant());
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(), notification.getType(), notification.getTitle(),
                notification.getBody(), notification.getLink(),
                notification.getReadAt(), notification.getCreatedAt());
    }
}
