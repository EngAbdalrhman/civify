package com.app.civify.service.usage;

import com.app.civify.entity.UserSubscription;
import com.app.civify.entity.enums.CvGenerationType;
import com.app.civify.entity.enums.SubscriptionStatus;
import com.app.civify.exception.BadRequestException;
import com.app.civify.repository.CvRepository;
import com.app.civify.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of usage tracking service
 * Tracks API calls, UI CV generation, and stored CVs against subscription limits
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UsageTrackingServiceImpl implements UsageTrackingService {

    private final UserSubscriptionRepository subscriptionRepository;
    private final CvRepository cvRepository;

    @Override
    public boolean canUseApi(Long userId) {
        UserSubscription subscription = getActiveSubscription(userId);
        if (subscription == null) {
            return false;
        }
        return subscription.canUseApi();
    }

    @Override
    public boolean canGenerateUiCv(Long userId) {
        UserSubscription subscription = getActiveSubscription(userId);
        if (subscription == null) {
            return false;
        }
        return subscription.canUseCv();
    }

    @Override
    public boolean canStoreCv(Long userId) {
        UserSubscription subscription = getActiveSubscription(userId);
        if (subscription == null) {
            return false;
        }

        Integer cvLimit = subscription.getPlan().getCvLimit();
        if (cvLimit == null) {
            return true; // Unlimited
        }

        long storedCount = cvRepository.countByUserId(userId);
        return storedCount < cvLimit;
    }

    @Override
    @Transactional
    public void incrementApiUsage(Long userId) {
        UserSubscription subscription = getActiveSubscription(userId);
        if (subscription == null) {
            throw new BadRequestException("No active subscription found");
        }

        if (!subscription.canUseApi()) {
            throw new BadRequestException(
                    "API call limit reached. Current: " + subscription.getCurrentApiCount() +
                    ", Limit: " + subscription.getPlan().getApiLimit()
            );
        }

        subscription.incrementApiCount();
        subscriptionRepository.save(subscription);
        
        log.debug("API usage incremented for user {}. Count: {}/{}", 
                userId, subscription.getCurrentApiCount(), subscription.getPlan().getApiLimit());
    }

    @Override
    @Transactional
    public void incrementUiCvUsage(Long userId) {
        UserSubscription subscription = getActiveSubscription(userId);
        if (subscription == null) {
            throw new BadRequestException("No active subscription found");
        }

        if (!subscription.canUseCv()) {
            throw new BadRequestException(
                    "UI CV generation limit reached. Current: " + subscription.getCurrentCvCount() +
                    ", Limit: " + subscription.getPlan().getCvLimit()
            );
        }

        subscription.incrementCvCount();
        subscriptionRepository.save(subscription);
        
        log.debug("UI CV usage incremented for user {}. Count: {}/{}", 
                userId, subscription.getCurrentCvCount(), subscription.getPlan().getCvLimit());
    }

    @Override
    public UsageStats getUserUsageStats(Long userId) {
        UserSubscription subscription = getActiveSubscription(userId);
        
        if (subscription == null) {
            return UsageStats.builder()
                    .apiUsageCount(0)
                    .apiLimit(0)
                    .apiRemaining(0)
                    .uiCvCount(0)
                    .uiCvLimit(0)
                    .uiCvRemaining(0)
                    .storedCvCount(0L)
                    .storedCvLimit(0)
                    .storedCvRemaining(0L)
                    .build();
        }

        Integer apiLimit = subscription.getPlan().getApiLimit();
        Integer cvLimit = subscription.getPlan().getCvLimit();
        long storedCvCount = cvRepository.countByUserId(userId);

        return UsageStats.builder()
                .subscriptionId(subscription.getId())
                .planName(subscription.getPlan().getName())
                .subscriptionEndDate(subscription.getEndDate())
                .apiUsageCount(subscription.getCurrentApiCount())
                .apiLimit(apiLimit)
                .apiRemaining(apiLimit != null ? Math.max(0, apiLimit - subscription.getCurrentApiCount()) : null)
                .uiCvCount(subscription.getCurrentCvCount())
                .uiCvLimit(cvLimit)
                .uiCvRemaining(cvLimit != null ? Math.max(0, cvLimit - subscription.getCurrentCvCount()) : null)
                .storedCvCount(storedCvCount)
                .storedCvLimit(cvLimit)
                .storedCvRemaining(cvLimit != null ? Math.max(0, cvLimit - storedCvCount) : null)
                .usageResetDate(subscription.getUsageResetDate())
                .build();
    }

    @Override
    public long getStoredCvCount(Long userId, CvGenerationType generationType) {
        if (generationType == null) {
            return cvRepository.countByUserId(userId);
        }
        return cvRepository.countByUserIdAndGenerationType(userId, generationType);
    }

    /**
     * Get active subscription for user via repository query (not from User entity)
     */
    private UserSubscription getActiveSubscription(Long userId) {
        return subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .stream()
                .filter(UserSubscription::isActive)
                .max((a, b) -> a.getStartDate().compareTo(b.getStartDate()))
                .orElse(null);
    }
}
