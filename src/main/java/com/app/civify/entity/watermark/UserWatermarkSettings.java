package com.app.civify.entity.watermark;

import com.app.civify.entity.enums.WatermarkPosition;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "user_watermark_settings")
public class UserWatermarkSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private Boolean enabled = false;

    @Column(name = "logo_storage_key")
    private String logoStorageKey;

    @Column(nullable = false)
    private Double opacity = 0.12;

    @Column(nullable = false)
    private Double scale = 0.35;

    @Column(nullable = false, length = 32)
    private WatermarkPosition position = WatermarkPosition.CENTER;

    private Double rotation = 0.0;

    @Column(name = "use_platform_watermark", nullable = false)
    private Boolean usePlatformWatermark = false;

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
