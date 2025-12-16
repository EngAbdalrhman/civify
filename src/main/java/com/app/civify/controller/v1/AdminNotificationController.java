package com.app.civify.controller.v1;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.security.annotation.CivifyAuthorization;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.app.civify.service.notification.NotificationService;

import java.util.Map;

@ApiV1Controller
@RequestMapping("/admin/notifications")
@RequiredArgsConstructor
@Tag(name = "Admin Notifications", description = "Admin endpoints for sending notifications")
@SecurityRequirement(name = "bearer-jwt")
public class AdminNotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Send notification to all users")
    @PostMapping("/send-all")
    @CivifyAuthorization(permission = "NOTIF.SEND")
    public ResponseEntity<Map<String, String>> sendNotificationToAll(@RequestBody NotificationRequest request) {
        notificationService.sendToAll(request.getTitle(), request.getMessage(), request.getType());
        return ResponseEntity.ok(Map.of("message", "Notification queued for all users"));
    }

    @Data
    public static class NotificationRequest {
        private String title;
        private String message;
        private String type; // e.g., INFO, WARNING, PROMO
    }
}
