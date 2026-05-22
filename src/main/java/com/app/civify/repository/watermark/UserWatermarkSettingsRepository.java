package com.app.civify.repository.watermark;

import com.app.civify.entity.watermark.UserWatermarkSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserWatermarkSettingsRepository extends JpaRepository<UserWatermarkSettings, Long> {
    Optional<UserWatermarkSettings> findByUserId(Long userId);
}
