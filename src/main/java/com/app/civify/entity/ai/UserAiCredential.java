package com.app.civify.entity.ai;

import com.app.civify.entity.enums.AiCredentialStatus;
import com.app.civify.entity.enums.AiProviderType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "user_ai_credentials")
public class UserAiCredential {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AiProviderType provider;

    @Column(nullable = false, length = 64)
    private String label;

    @Column(nullable = false)
    private byte[] ciphertext;

    @Column(nullable = false)
    private byte[] iv;

    @Column(name = "key_fingerprint", nullable = false, length = 64)
    private String keyFingerprint;

    @Column(name = "default_model", length = 128)
    private String defaultModel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AiCredentialStatus status = AiCredentialStatus.ACTIVE;

    @Column(name = "last_validated_at")
    private LocalDateTime lastValidatedAt;

    @Column(name = "last_error", length = 255)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null) {
            status = AiCredentialStatus.ACTIVE;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
