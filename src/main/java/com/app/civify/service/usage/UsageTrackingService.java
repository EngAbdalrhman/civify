package com.app.civify.service.usage;

import com.app.civify.entity.enums.CvGenerationType;

/**
 * Service for tracking and validating user usage against subscription limits
 * Handles API calls, UI CV generation, and stored CV limits
 */
public interface UsageTrackingService {

    /**
     * Check if user can make an API call (for CV generation via API key)
     * @param userId User ID
     * @return true if within limit
     */
    boolean canUseApi(Long userId);

    /**
     * Check if user can generate a CV via UI stepper
     * @param userId User ID
     * @return true if within limit
     */
    boolean canGenerateUiCv(Long userId);

    /**
     * Check if user can store more CVs
     * @param userId User ID
     * @return true if within limit
     */
    boolean canStoreCv(Long userId);

    /**
     * Increment API usage counter
     * @param userId User ID
     * @throws com.app.civify.exception.BadRequestException if limit reached
     */
    void incrementApiUsage(Long userId);

    /**
     * Increment UI CV generation counter
     * @param userId User ID
     * @throws com.app.civify.exception.BadRequestException if limit reached
     */
    void incrementUiCvUsage(Long userId);

    /**
     * Get current usage statistics for user
     * @param userId User ID
     * @return Usage statistics
     */
    UsageStats getUserUsageStats(Long userId);

    /**
     * Get count of stored CVs for user
     * @param userId User ID
     * @param generationType Optional filter by generation type
     * @return Count of stored CVs
     */
    long getStoredCvCount(Long userId, CvGenerationType generationType);
}
