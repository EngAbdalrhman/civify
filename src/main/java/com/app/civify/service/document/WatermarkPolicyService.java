package com.app.civify.service.document;

import com.app.civify.dto.document.WatermarkConfigDto;
import com.app.civify.entity.enums.FeatureType;
import com.app.civify.entity.plans.PlanFeatureLimit;
import com.app.civify.entity.plans.SubscriptionPlan;
import com.app.civify.entity.security.Permission;
import com.app.civify.entity.watermark.PlatformWatermarkSettings;
import com.app.civify.entity.watermark.UserWatermarkSettings;
import com.app.civify.repository.UserRepository;
import com.app.civify.repository.security.GroupRepository;
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
    private final GroupRepository groupRepository;
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

        // Custom branding first — paid "no platform watermark" plans still use the user's own logo.
        Optional<UserWatermarkSettings> userSettings = userSettingsRepository.findByUserId(userId);
        if (userSettings.isPresent()
                && Boolean.TRUE.equals(userSettings.get().getEnabled())
                && !Boolean.TRUE.equals(userSettings.get().getUsePlatformWatermark())
                && userSettings.get().getLogoStorageKey() != null
                && !userSettings.get().getLogoStorageKey().isBlank()
                && hasCustomBranding(userId)) {
            WatermarkConfigDto custom = fromUserSettings(userId, userSettings.get());
            if (custom.isEnabled()) {
                return custom;
            }
        }

        if (canExportWithoutPlatformWatermark(userId)) {
            return disabled();
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
                .map(sub -> planHasPlatformWatermark(sub.getPlan()))
                .orElse(true);
    }

    /**
     * {@code watermark=true} or unset/null on plan → apply platform watermark (free / legacy plans).
     * Only {@code watermark=false} explicitly disables it (paid).
     */
    private boolean planHasPlatformWatermark(SubscriptionPlan plan) {
        if (plan == null) {
            return true;
        }
        return !Boolean.FALSE.equals(plan.getWatermark());
    }

    /** Paid plan: no Civify platform watermark (custom branding logo may still apply). */
    private boolean canExportWithoutPlatformWatermark(Long userId) {
        if (subscriptionService.canUserUseFeature(userId, "WATERMARK")) {
            return true;
        }
        return userHasPermission(userId, "CV.NO_WATERMARK");
    }

    /**
     * Permissions from DB — API-key auth does not load {@code User#getAuthorities()}.
     */
    private boolean userHasPermission(Long userId, String permission) {
        return userRepository.findById(userId)
                .filter(user -> user.getGroup() != null)
                .flatMap(user -> groupRepository.findByIdWithPermissions(user.getGroup().getId()))
                .map(group -> group.getPermissions().stream()
                        .map(Permission::getName)
                        .anyMatch(permission::equalsIgnoreCase))
                .orElse(false);
    }

    private boolean hasCustomBranding(Long userId) {
        if (userHasPermission(userId, "CV.CUSTOM_WATERMARK")) {
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
        String position = settings != null && settings.getPosition() != null
                ? settings.getPosition().getValue()
                : defaultPosition;
        Double rotation = settings != null ? settings.getRotation() : 0.0;

        return WatermarkConfigDto.builder()
                .enabled(true)
                .imageUrl(normalizeWatermarkAssetUrl(imageUrl))
                .opacity(opacity)
                .scale(scale)
                .position(position)
                .rotation(rotation)
                .build();
    }

    private WatermarkConfigDto fromUserSettings(Long userId, UserWatermarkSettings settings) {
        String imageUrl = resolveImageUrl(settings.getLogoStorageKey(), null);
        if (imageUrl == null) {
            log.warn(
                    "Custom watermark enabled for userId={} but logo URL could not be resolved for key={}",
                    userId,
                    settings.getLogoStorageKey());
            // Do not fall back to platform watermark on paid branding plans.
            if (canExportWithoutPlatformWatermark(userId)) {
                return disabled();
            }
            return resolvePlatformWatermark();
        }
        return WatermarkConfigDto.builder()
                .enabled(true)
                .imageUrl(normalizeWatermarkAssetUrl(imageUrl))
                .opacity(settings.getOpacity())
                .scale(settings.getScale())
                .position(settings.getPosition() != null ? settings.getPosition().getValue() : defaultPosition)
                .rotation(settings.getRotation())
                .build();
    }

    private String resolveImageUrl(String storageKey, String fallbackKey) {
        String key = storageKey != null && !storageKey.isBlank() ? storageKey : fallbackKey;
        if (key == null) {
            return null;
        }
        return bucketService.resolveLogoUrlForPdf(key);
    }

    private String fallbackLogoUrl() {
        if (defaultLogoUrl != null && !defaultLogoUrl.isBlank()) {
            return normalizeWatermarkAssetUrl(defaultLogoUrl);
        }
        String base = frontendBaseUrl.endsWith("/") ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1) : frontendBaseUrl;
        return base + "/brand/watermark.png";
    }

    private String normalizeWatermarkAssetUrl(String url) {
        if (url != null && url.endsWith("/brand/watermark.svg")) {
            return url.replace("/brand/watermark.svg", "/brand/watermark.png");
        }
        return url;
    }

    private WatermarkConfigDto disabled() {
        return WatermarkConfigDto.builder().enabled(false).build();
    }
}
