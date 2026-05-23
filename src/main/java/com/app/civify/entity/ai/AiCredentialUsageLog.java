package com.app.civify.entity.ai;

import com.app.civify.entity.enums.AiCredentialSource;
import com.app.civify.entity.enums.TokenUsageType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_credential_usage_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCredentialUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "credential_id", columnDefinition = "uuid")
    private UUID credentialId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AiCredentialSource source;

    @Column(nullable = false, length = 32)
    private String provider;

    @Column(nullable = false, length = 128)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_usage_type", nullable = false, length = 30)
    private TokenUsageType tokenUsageType;

    @Column(name = "prompt_tokens", nullable = false)
    private int promptTokens;

    @Column(name = "completion_tokens", nullable = false)
    private int completionTokens;

    @Column(name = "total_tokens", nullable = false)
    private int totalTokens;

    @Column(name = "latency_ms", nullable = false)
    private int latencyMs;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
