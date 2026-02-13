package com.app.civify.dto.admin;

import com.app.civify.entity.enums.NotificationChannel;
import com.app.civify.entity.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AdminNotificationRequest {
    @NotBlank
    private String title;
    
    @NotBlank
    private String message;
    
    @NotNull
    private NotificationType type;
    
    @NotNull
    private NotificationChannel channel = NotificationChannel.PUSH;
    
    private boolean broadcast = true;
    
    private List<Long> userIds;
}
