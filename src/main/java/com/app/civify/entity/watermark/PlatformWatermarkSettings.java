package com.app.civify.entity.watermark;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "platform_watermark_settings")
public class PlatformWatermarkSettings {

    @Id
    private Long id = 1L;

    @Column(name = "logo_storage_key")
    private String logoStorageKey;

    @Column(nullable = false)
    private Double opacity = 0.12;

    @Column(nullable = false)
    private Double scale = 0.35;

    @Column(nullable = false, length = 32)
    private String position = "center";

    private Double rotation = 0.0;

    @Column(nullable = false)
    private Boolean enabled = true;

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
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
