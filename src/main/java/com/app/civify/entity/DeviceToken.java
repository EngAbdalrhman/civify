package com.app.civify.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Stores user device tokens for FCM push notifications.
 * One user can have multiple devices (web, mobile, etc.).
 */
@Entity
@Table(name = "device_tokens", 
       indexes = @Index(name = "idx_device_token", columnList = "token"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 512)
    private String token; // FC M device token

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type")
    private DeviceType deviceType; // WEB, ANDROID, IOS

    @Column(name = "device_name")
    private String deviceName; // e.g., "Chrome on Windows", "iPhone 13"

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    public enum DeviceType {
        WEB, ANDROID, IOS
    }
}
