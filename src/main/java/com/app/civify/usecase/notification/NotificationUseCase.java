package com.app.civify.usecase.notification;

import com.app.civify.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.app.civify.security.UserPrincipal;

public interface NotificationUseCase {
    Page<Notification> getRecentNotifications(Long userId, Pageable pageable);
    Long getUnreadCount(Long userId);
    SseEmitter subscribe(UserPrincipal userPrincipal);
}
