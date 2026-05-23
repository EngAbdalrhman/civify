package com.app.civify.entity.ai;

import com.app.civify.dto.ai.credential.ByokFallbackEntry;
import com.app.civify.entity.enums.ByokMode;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "user_ai_settings")
public class UserAiSettings {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "byok_mode", nullable = false, length = 32)
    private ByokMode byokMode = ByokMode.DISABLED;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fallback_chain", nullable = false, columnDefinition = "jsonb")
    private List<ByokFallbackEntry> fallbackChain = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (byokMode == null) {
            byokMode = ByokMode.DISABLED;
        }
        if (fallbackChain == null) {
            fallbackChain = new ArrayList<>();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
