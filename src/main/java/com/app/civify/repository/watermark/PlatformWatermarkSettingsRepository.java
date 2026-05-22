package com.app.civify.repository.watermark;

import com.app.civify.entity.watermark.PlatformWatermarkSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlatformWatermarkSettingsRepository extends JpaRepository<PlatformWatermarkSettings, Long> {
}
