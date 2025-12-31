package com.app.civify.controller.v1;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.entity.Notification;
import com.app.civify.security.UserPrincipal;
import com.app.civify.usecase.notification.NotificationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Map;

/**
 * SSE (Server-Sent Events) endpoint for real-time notifications.
 *
 * DEDUPLICATION:
 * - Each notification sent ONCE via SSE
 * - Marked as delivered = TRUE immediately after sending
 * - Quartz fallback skips delivered = TRUE notifications
 */
@ApiV1Controller
@RequestMapping("/notifications")
@Slf4j
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationUseCase notificationUseCase;

    /**
     * Get recent notifications (user specific + broadcast)
     */
    @GetMapping
    public ResponseEntity<Page<Notification>> getRecentNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationUseCase.getRecentNotifications(userPrincipal.getId(), pageable);
        
        return ResponseEntity.ok(notifications);
    }

    /**
     * SSE stream endpoint for real-time notifications.
     * Auto-sends any missed notifications on connection.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return notificationUseCase.subscribe(userPrincipal);
    }

    /**
     * Get undelivered notification count for user
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long count = notificationUseCase.getUnreadCount(userPrincipal.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }
}
