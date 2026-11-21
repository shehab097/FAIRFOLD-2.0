package com.fairfold.notification;

import com.fairfold.common.paging.PageResponse;
import com.fairfold.common.security.CurrentUser;
import com.fairfold.notification.dto.NotificationResponse;
import com.fairfold.notification.dto.UnreadCountResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "In-app notifications for the signed-in user")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "List the current user's notifications")
    public PageResponse<NotificationResponse> list(@AuthenticationPrincipal CurrentUser user,
                                                   @PageableDefault(size = 20) Pageable pageable) {
        return notificationService.list(user.id(), pageable);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Number of unread notifications")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal CurrentUser user) {
        return notificationService.unreadCount(user.id());
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark one notification read")
    public NotificationResponse markRead(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        return notificationService.markRead(user.id(), id);
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all notifications read")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal CurrentUser user) {
        notificationService.markAllRead(user.id());
        return ResponseEntity.noContent().build();
    }
}
