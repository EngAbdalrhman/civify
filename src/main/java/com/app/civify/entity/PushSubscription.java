package com.app.civify.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity storing Web Push subscription information for a user.
 * Used to send push notifications when user is offline.
 */
@Entity
@Table(name = "push_subscriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 500)
    private String endpoint;

    @Column(name = "p256dh_key", nullable = false, length = 500)
    private String p256dhKey;

    @Column(name = "auth_key", nullable = false, length = 500)
    private String authKey;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Builder.Default
    @Column(name = "active")
    private Boolean active = true;

    public void markAsUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.active = false;
    }
}
