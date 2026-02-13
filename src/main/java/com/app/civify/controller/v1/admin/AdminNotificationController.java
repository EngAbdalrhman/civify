package com.app.civify.controller.v1.admin;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.admin.AdminNotificationRequest;
import com.app.civify.security.annotation.CivifyAuthorization;
import com.app.civify.service.notification.NotificationService;
import com.app.civify.util.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiV1Controller
@RequestMapping("/admin/notifications")
@RequiredArgsConstructor
@Tag(name = "Admin Notifications", description = "Admin endpoints for sending notifications")
@SecurityRequirement(name = "bearer-jwt")
public class AdminNotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Send notification (Broadcast or Directed, Push or Email)")
    @PostMapping("/send")
    @CivifyAuthorization(permission = "NOTIF.SEND")
    public ResponseEntity<CommonResponse<String>> sendNotification(@Valid @RequestBody AdminNotificationRequest request) {
        notificationService.dispatch(request);

        String message = request.isBroadcast()
                ? "Broadcast notification queued"
                : "Notification queued for " + request.getUserIds().size() + " users";

        return ResponseEntity.ok(CommonResponse.success(message));
    }
}
