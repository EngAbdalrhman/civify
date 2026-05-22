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

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
     * <p>Order:
     * <ol>
     *   <li>Custom branding (enabled + own logo + permission/plan) → user logo</li>
     *   <li>Paid ({@code plan.watermark=false} or {@code CV.NO_WATERMARK}) → no watermark</li>
     *   <li>Free / no active plan / plan still has platform watermark → Civify logo</li>
     *   <li>Otherwise → no watermark (safety net; normally unreachable)</li>
     * </ol>
     */
    public WatermarkConfigDto resolveForUser(Long userId) {
        if (userId == null) {
            return resolvePlatformWatermark();
        }

        Set<String> permissions = loadPermissionNames(userId);
        Optional<UserWatermarkSettings> userSettings = userSettingsRepository.findByUserId(userId);

        // 1. Custom branding — never fall through to platform if branding is on but logo is missing.
        if (wantsCustomBranding(userSettings, userId, permissions)) {
            return fromUserSettings(userId, userSettings.get());
        }

        // 2. Paid without platform watermark.
        if (canExportWithoutPlatformWatermark(userId, permissions)) {
            return disabled();
        }

        // 3. Free tier, no subscription, or plan still requires platform watermark.
        if (shouldApplyPlatformWatermark(userId)) {
            return resolvePlatformWatermark();
        }

        // 4. Safety net (e.g. active paid plan with watermark=false already handled in step 2).
        return disabled();
    }

    private boolean wantsCustomBranding(
            Optional<UserWatermarkSettings> userSettings,
            Long userId,
            Set<String> permissions) {
        return userSettings.isPresent()
                && Boolean.TRUE.equals(userSettings.get().getEnabled())
                && !Boolean.TRUE.equals(userSettings.get().getUsePlatformWatermark())
                && userSettings.get().getLogoStorageKey() != null
                && !userSettings.get().getLogoStorageKey().isBlank()
                && hasCustomBranding(userId, permissions);
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
    private boolean canExportWithoutPlatformWatermark(Long userId, Set<String> permissions) {
        if (subscriptionService.canUserUseFeature(userId, "WATERMARK")) {
            return true;
        }
        return permissions.contains("CV.NO_WATERMARK");
    }

    private Set<String> loadPermissionNames(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> user.getGroup() != null)
                .flatMap(user -> groupRepository.findByIdWithPermissions(user.getGroup().getId()))
                .map(group -> group.getPermissions().stream()
                        .map(Permission::getName)
                        .filter(name -> name != null && !name.isBlank())
                        .map(name -> name.toUpperCase(Locale.ROOT))
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    private boolean hasCustomBranding(Long userId, Set<String> permissions) {
        if (permissions.contains("CV.CUSTOM_WATERMARK")) {
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
        // Only explicit false disables; null/true keep platform watermark enabled.
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
            // Custom branding was requested — never substitute the Civify platform logo here.
            return disabled();
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
