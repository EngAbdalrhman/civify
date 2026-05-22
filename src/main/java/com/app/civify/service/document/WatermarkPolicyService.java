package com.app.civify.service.document;

import com.app.civify.dto.document.WatermarkConfigDto;
import com.app.civify.entity.User;
import com.app.civify.entity.enums.FeatureType;
import com.app.civify.entity.plans.PlanFeatureLimit;
import com.app.civify.entity.plans.SubscriptionPlan;
import com.app.civify.entity.plans.UserSubscription;
import com.app.civify.entity.watermark.PlatformWatermarkSettings;
import com.app.civify.entity.watermark.UserWatermarkSettings;
import com.app.civify.repository.UserRepository;
import com.app.civify.repository.watermark.PlatformWatermarkSettingsRepository;
import com.app.civify.repository.watermark.UserWatermarkSettingsRepository;
import com.app.civify.service.subscription.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WatermarkPolicyService {

    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;
    private final PlatformWatermarkSettingsRepository platformSettingsRepository;
    private final UserWatermarkSettingsRepository userSettingsRepository;
    private final BucketService bucketService;

    @Value("${watermark.logo-url:}")
    private String defaultLogoUrl;

    @Value("${watermark.opacity:0.12}")
    private double defaultOpacity;

    @Value("${watermark.scale:0.35}")
    private double defaultScale;

    @Value("${watermark.position:center}")
    private String defaultPosition;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    /**
     * Resolve watermark for PDF export. {@code userId} null = guest (always platform watermark).
     */
    public WatermarkConfigDto resolveForUser(Long userId) {
        if (userId == null) {
            return resolvePlatformWatermark();
        }

        if (canExportWithoutWatermark(userId)) {
            return disabled();
        }

        Optional<UserWatermarkSettings> userSettings = userSettingsRepository.findByUserId(userId);
        if (userSettings.isPresent()
                && Boolean.TRUE.equals(userSettings.get().getEnabled())
                && !Boolean.TRUE.equals(userSettings.get().getUsePlatformWatermark())
                && hasCustomBranding(userId)
                && userSettings.get().getLogoStorageKey() != null) {
            return fromUserSettings(userSettings.get());
        }

        if (shouldApplyPlatformWatermark(userId)) {
            return resolvePlatformWatermark();
        }

        return disabled();
    }

    public WatermarkConfigDto resolveForGuest() {
        return resolvePlatformWatermark();
    }

    private boolean shouldApplyPlatformWatermark(Long userId) {
        return subscriptionService.getActiveSubscription(userId)
                .map(sub -> Boolean.TRUE.equals(sub.getPlan().getWatermark()))
                .orElse(true);
    }

    private boolean canExportWithoutWatermark(Long userId) {
        if (subscriptionService.canUserUseFeature(userId, "WATERMARK")) {
            return true;
        }
        return userRepository.findById(userId)
                .map(this::hasPermission)
                .orElse(false);
    }

    private boolean hasPermission(User user, String permission) {
        if (user == null) {
            return false;
        }
        return user.getAuthorities().stream()
                .anyMatch(a -> permission.equals(a.getAuthority()));
    }

    private boolean hasCustomBranding(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isPresent() && hasPermission(user.get(), "CV.CUSTOM_WATERMARK")) {
            return true;
        }
        return subscriptionService.getActiveSubscription(userId)
                .map(sub -> planHasFeature(sub.getPlan(), FeatureType.CUSTOM_BRANDING))
                .orElse(false);
    }

    private boolean planHasFeature(SubscriptionPlan plan, FeatureType featureType) {
        if (plan == null || plan.getFeatureLimits() == null) {
            return false;
        }
        return plan.getFeatureLimits().stream()
                .filter(fl -> fl.getFeature() != null && fl.getFeature().getFeatureType() == featureType)
                .anyMatch(PlanFeatureLimit::getEnabled);
    }

    public WatermarkConfigDto resolvePlatformWatermark() {
        PlatformWatermarkSettings settings = platformSettingsRepository.findById(1L).orElse(null);
        if (settings != null && Boolean.FALSE.equals(settings.getEnabled())) {
            return disabled();
        }

        String imageUrl = resolveImageUrl(
                settings != null ? settings.getLogoStorageKey() : null,
                BucketService.PLATFORM_WATERMARK_KEY);
        if (imageUrl == null) {
            imageUrl = fallbackLogoUrl();
        }

        double opacity = settings != null ? settings.getOpacity() : defaultOpacity;
        double scale = settings != null ? settings.getScale() : defaultScale;
        String position = settings != null ? settings.getPosition() : defaultPosition;
        Double rotation = settings != null ? settings.getRotation() : 0.0;

        return WatermarkConfigDto.builder()
                .enabled(true)
                .imageUrl(imageUrl)
                .opacity(opacity)
                .scale(scale)
                .position(position)
                .rotation(rotation)
                .build();
    }

    private WatermarkConfigDto fromUserSettings(UserWatermarkSettings settings) {
        String imageUrl = resolveImageUrl(settings.getLogoStorageKey(), null);
        if (imageUrl == null) {
            return resolvePlatformWatermark();
        }
        return WatermarkConfigDto.builder()
                .enabled(true)
                .imageUrl(imageUrl)
                .opacity(settings.getOpacity())
                .scale(settings.getScale())
                .position(settings.getPosition())
                .rotation(settings.getRotation())
                .build();
    }

    private String resolveImageUrl(String storageKey, String fallbackKey) {
        String key = storageKey != null && !storageKey.isBlank() ? storageKey : fallbackKey;
        if (key == null) {
            return null;
        }
        String publicUrl = bucketService.buildPublicLogoUrl(key);
        if (publicUrl != null) {
            return publicUrl;
        }
        return null;
    }

    private String fallbackLogoUrl() {
        if (defaultLogoUrl != null && !defaultLogoUrl.isBlank()) {
            return defaultLogoUrl;
        }
        String base = frontendBaseUrl.endsWith("/") ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1) : frontendBaseUrl;
        return base + "/brand/watermark.svg";
    }

    private WatermarkConfigDto disabled() {
        return WatermarkConfigDto.builder().enabled(false).build();
    }
}
