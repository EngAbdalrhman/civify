package com.app.civify.repository.ai;

import com.app.civify.entity.ai.UserAiSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAiSettingsRepository extends JpaRepository<UserAiSettings, Long> {
}
