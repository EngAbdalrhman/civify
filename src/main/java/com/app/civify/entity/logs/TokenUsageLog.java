package com.app.civify.entity.logs;

import com.app.civify.entity.enums.TokenSource;
import com.app.civify.entity.enums.TokenUsageType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Tracks individual token consumption events.
 * Records where tokens were spent and from what source.
 */
@Entity
@Table(name = "token_usage_logs", indexes = {
    @Index(name = "idx_token_usage_user", columnList = "user_id"),
    @Index(name = "idx_token_usage_type", columnList = "usage_type"),
    @Index(name = "idx_token_usage_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type", nullable = false, length = 30, columnDefinition = "varchar(30)")
    private TokenUsageType usageType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20, columnDefinition = "varchar(20)")
    private TokenSource source;

    @Column(name = "tokens_used", nullable = false)
    private int tokensUsed;

    @Column(name = "from_subscription")
    private int fromSubscription;

    @Column(name = "from_wallet")
    private int fromWallet;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
