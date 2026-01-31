package com.app.civify.service.user;

import com.app.civify.dto.user.UserPreferenceDTO;
import com.app.civify.entity.User;
import com.app.civify.entity.enums.UserPreferredLanguage;
import com.app.civify.entity.enums.UserPreferredTheme;
import com.app.civify.exception.ResourceNotFoundException;
import com.app.civify.repository.UserRepository;
import com.app.civify.service.cache.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * Service for managing user preferences with caching support
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserPreferenceService {

    private static final String PREF_CACHE_PREFIX = "user:pref:";
    private static final Duration PREF_CACHE_TTL = Duration.ofHours(24);

    private final UserRepository userRepository;
    private final CacheService cacheService;

    /**
     * Get user preferred theme with caching
     */
    public UserPreferredTheme getUserTheme(Long userId) {
        String cacheKey = PREF_CACHE_PREFIX + userId + ":theme";
        String cached = cacheService.get(cacheKey);
        
        if (cached != null) {
            try {
                return UserPreferredTheme.valueOf(cached);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid cached theme value: {}", cached);
            }
        }
        
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return UserPreferredTheme.LIGHT; // Default
        }
        
        UserPreferredTheme theme = user.getPreferredTheme() != null 
            ? user.getPreferredTheme() 
            : UserPreferredTheme.LIGHT;
        
        cacheService.set(cacheKey, theme.name(), PREF_CACHE_TTL);
        return theme;
    }

    /**
     * Get user preferred language with caching
     */
    public UserPreferredLanguage getUserLanguage(Long userId) {
        String cacheKey = PREF_CACHE_PREFIX + userId + ":lang";
        String cached = cacheService.get(cacheKey);
        
        if (cached != null) {
            try {
                return UserPreferredLanguage.valueOf(cached);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid cached language value: {}", cached);
            }
        }
        
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return UserPreferredLanguage.EN; // Default
        }
        
        UserPreferredLanguage language = user.getPreferredLanguage() != null 
            ? user.getPreferredLanguage() 
            : UserPreferredLanguage.EN;
        
        cacheService.set(cacheKey, language.name(), PREF_CACHE_TTL);
        return language;
    }

    /**
     * Get both user preferences
     */
    public UserPreferenceDTO getUserPreferences(Long userId) {
        return new UserPreferenceDTO(
            getUserLanguage(userId),
            getUserTheme(userId)
        );
    }

    /**
     * Update user preferences and invalidate cache
     */
    @Transactional
    public UserPreferenceDTO updatePreferences(Long userId, UserPreferenceDTO preferences) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        UserPreferredLanguage newLanguage =
            preferences.preferredLanguage() != null ? preferences.preferredLanguage() : user.getPreferredLanguage();
        
        UserPreferredTheme newTheme =
            preferences.preferredTheme() != null ? preferences.preferredTheme() : user.getPreferredTheme();
            
        // Use direct update to bypass Audit Listener
        userRepository.updateUserPreferences(userId, newLanguage, newTheme);
        
        // Update the entity object for return value (though it won't be re-fetched)
        user.setPreferredLanguage(newLanguage);
        user.setPreferredTheme(newTheme);
        
        // Invalidate cache
        invalidateCache(userId);
        
        log.info("Updated preferences for user {}: language={}, theme={}", 
            userId, user.getPreferredLanguage(), user.getPreferredTheme());
        
        return new UserPreferenceDTO(
            user.getPreferredLanguage(),
            user.getPreferredTheme()
        );
    }

    /**
     * Invalidate cached preferences for a user
     */
    public void invalidateCache(Long userId) {
        cacheService.delete(PREF_CACHE_PREFIX + userId + ":theme");
        cacheService.delete(PREF_CACHE_PREFIX + userId + ":lang");
    }
}
