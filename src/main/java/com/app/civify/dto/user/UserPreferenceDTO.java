package com.app.civify.dto.user;

import com.app.civify.entity.enums.UserPreferredLanguage;
import com.app.civify.entity.enums.UserPreferredTheme;

/**
 * DTO for user preference updates
 */
public record UserPreferenceDTO(
    UserPreferredLanguage preferredLanguage,
    UserPreferredTheme preferredTheme,
    Boolean autoSaveEnabled
) {}
