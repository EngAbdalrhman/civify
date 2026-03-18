package com.app.civify.service.usage;

import com.app.civify.entity.plans.UserSubscription;
import com.app.civify.entity.enums.CvGenerationType;
import com.app.civify.entity.enums.SubscriptionStatus;
import com.app.civify.exception.BadRequestException;
import com.app.civify.repository.resume.ResumeRepository;
import com.app.civify.repository.plans.UserSubscriptionRepository;
import com.app.civify.service.i18n.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of usage tracking service
 * Tracks CV generation and stored resumes against subscription limits
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UsageTrackingServiceImpl implements UsageTrackingService {

    private final UserSubscriptionRepository subscriptionRepository;
    private final ResumeRepository resumeRepository;
    private final MessageService messageService;

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
        // Delegate to canStoreResume since we now only track Resume entities
        return canStoreResume(userId);
    }

    @Override
    public boolean canStoreResume(Long userId) {
        UserSubscription subscription = getActiveSubscription(userId);
        if (subscription == null) {
            return false;
        }

        Integer cvLimit = subscription.getPlan().getCvLimit();
        if (cvLimit == null) {
            return true; // Unlimited
        }

        long storedCount = resumeRepository.countByUserId(userId);
        return storedCount < cvLimit;
    }

    @Override
    @Transactional
    public void incrementUiCvUsage(Long userId) {
        UserSubscription subscription = getActiveSubscription(userId);
        if (subscription == null) {
            throw new BadRequestException("subscription.notfound");
        }

        if (!subscription.canUseCv()) {
            throw new BadRequestException(
                    messageService.getMessage("subscription.cv.limit.reached") + " (" + subscription.getCurrentCvCount() + "/" + subscription.getPlan().getCvLimit() + ")"
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
                    .uiCvCount(0)
                    .uiCvLimit(0)
                    .uiCvRemaining(0)
                    .storedCvCount(0L)
                    .storedCvLimit(0)
                    .storedCvRemaining(0L)
                    .build();
        }

        Integer cvLimit = subscription.getPlan().getCvLimit();
        long storedResumeCount = resumeRepository.countByUserId(userId);

        return UsageStats.builder()
                .subscriptionId(subscription.getId())
                .planName(subscription.getPlan().getName())
                .subscriptionEndDate(subscription.getEndDate())
                .uiCvCount(subscription.getCurrentCvCount())
                .uiCvLimit(cvLimit)
                .uiCvRemaining(cvLimit != null ? Math.max(0, cvLimit - subscription.getCurrentCvCount()) : null)
                .storedCvCount(storedResumeCount)
                .storedCvLimit(cvLimit)
                .storedCvRemaining(cvLimit != null ? Math.max(0, cvLimit - storedResumeCount) : null)
                .usageResetDate(subscription.getUsageResetDate())
                .build();
    }

    @Override
    public long getStoredCvCount(Long userId, CvGenerationType generationType) {
        // Since we removed Cv entity, we just count resumes
        // The generationType parameter is kept for interface compatibility but ignored
        return resumeRepository.countByUserId(userId);
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

