package com.app.civify.config;

import com.app.civify.entity.PlanFeature;
import com.app.civify.entity.PlanFeatureLimit;
import com.app.civify.entity.PlanPricing;
import com.app.civify.entity.SubscriptionPlan;
import com.app.civify.entity.enums.BillingCycle;
import com.app.civify.entity.enums.FeatureType;
import com.app.civify.repository.PlanFeatureRepository;
import com.app.civify.repository.SubscriptionPlanRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Initializes plan features and free plan on application startup
 * Ensures idempotent operation - safe to run multiple times
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class PlanDataInitializer {

    private final PlanFeatureRepository planFeatureRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    private static final String FREE_PLAN_NAME = "free";

    @PostConstruct
    @Transactional
    public void initializePlanData() {
        log.info("Starting plan data initialization...");

        try {
            // Step 1: Initialize all plan features
            initializeFeatures();

            // Step 2: Initialize free plan
            initializeFreePlan();

            log.info("✓ Plan data initialization completed successfully");
        } catch (Exception e) {
            log.error("Failed to initialize plan data: {}", e.getMessage(), e);
            // Don't throw exception - allow app to start even if initialization fails
            // The createDefaultFreeSubscription method will throw a more specific error when needed
        }
    }

    /**
     * Initialize all plan features based on FeatureType enum
     * Only creates features that don't already exist
     */
    private void initializeFeatures() {
        log.info("Initializing plan features...");

        int createdCount = 0;
        int existingCount = 0;

        for (FeatureType featureType : FeatureType.values()) {
            Optional<PlanFeature> existing = planFeatureRepository.findByFeatureType(featureType);

            if (existing.isPresent()) {
                existingCount++;
                continue;
            }

            // Create new feature
            PlanFeature feature = new PlanFeature();
            feature.setFeatureType(featureType);
            feature.setName(featureType.getDisplayName());
            feature.setDescription(featureType.getDescription());
            feature.setHasLimit(featureType.hasLimit());
            feature.setActive(true);
            feature.setDisplayOrder(featureType.ordinal());

            // Set appropriate unit for limited features
            if (featureType.hasLimit()) {
                feature.setUnit(getUnitForFeatureType(featureType));
            }

            planFeatureRepository.save(feature);
            createdCount++;
            log.debug("Created feature: {}", featureType.getDisplayName());
        }

        log.info("Features initialized: {} created, {} already existed", createdCount, existingCount);
    }

    /**
     * Initialize the free plan with appropriate feature limits
     * Only creates the plan if it doesn't already exist
     */
    private void initializeFreePlan() {
        log.info("Initializing free plan...");

        Optional<SubscriptionPlan> existing = subscriptionPlanRepository.findByName(FREE_PLAN_NAME);

        if (existing.isPresent()) {
            log.info("Free plan already exists, skipping initialization");
            return;
        }

        // Create free plan
        SubscriptionPlan freePlan = new SubscriptionPlan();
        freePlan.setName(FREE_PLAN_NAME);
        freePlan.setDescription("Free plan with basic features");
        freePlan.setDetails("Get started with our free plan including limited CV generation, API access, and basic export formats.");
        
        // Legacy fields (for backward compatibility)
        freePlan.setCvLimit(5);
        freePlan.setApiLimit(10);
        freePlan.setWatermark(true); // Has watermark
        freePlan.setMonthlyPrice(BigDecimal.ZERO);
        freePlan.setAnnualPrice(BigDecimal.ZERO);
        freePlan.setActive(true);

        // Add feature limits
        addFeatureLimit(freePlan, FeatureType.CV_GENERATION, 5, true);
        addFeatureLimit(freePlan, FeatureType.API_ACCESS, 10, true);
        addFeatureLimit(freePlan, FeatureType.WATERMARK_REMOVAL, null, false);
        addFeatureLimit(freePlan, FeatureType.DASHBOARD_ACCESS, null, false);
        addFeatureLimit(freePlan, FeatureType.PREMIUM_TEMPLATES, null, false);
        addFeatureLimit(freePlan, FeatureType.EXPORT_FORMATS, null, true); // PDF export
        addFeatureLimit(freePlan, FeatureType.AI_MODEL_ACCESS, null, false);
        addFeatureLimit(freePlan, FeatureType.PRIORITY_SUPPORT, null, false);
        addFeatureLimit(freePlan, FeatureType.CUSTOM_BRANDING, null, false);
        addFeatureLimit(freePlan, FeatureType.TEAM_COLLABORATION, 0, false);
        addFeatureLimit(freePlan, FeatureType.STORAGE_SPACE, 1, true); // 1 GB
        addFeatureLimit(freePlan, FeatureType.INTEGRATION_ACCESS, null, false);

        // Add pricing options
        addPricingOption(freePlan, BillingCycle.MONTHLY, BigDecimal.ZERO);
        addPricingOption(freePlan, BillingCycle.ANNUAL, BigDecimal.ZERO);

        // Save the plan with all relationships
        subscriptionPlanRepository.save(freePlan);

        log.info("✓ Free plan created successfully with {} features and {} pricing options",
                freePlan.getFeatureLimits().size(),
                freePlan.getPricingOptions().size());
    }

    /**
     * Add a feature limit to a plan
     */
    private void addFeatureLimit(SubscriptionPlan plan, FeatureType featureType, Integer limitValue, boolean enabled) {
        Optional<PlanFeature> featureOpt = planFeatureRepository.findByFeatureType(featureType);

        if (featureOpt.isEmpty()) {
            log.warn("Feature {} not found, skipping", featureType);
            return;
        }

        PlanFeatureLimit featureLimit = new PlanFeatureLimit();
        featureLimit.setPlan(plan);
        featureLimit.setFeature(featureOpt.get());
        featureLimit.setLimitValue(limitValue);
        featureLimit.setEnabled(enabled);
        featureLimit.setDisplayOrder(featureType.ordinal());

        plan.addFeatureLimit(featureLimit);
    }

    /**
     * Add a pricing option to a plan
     */
    private void addPricingOption(SubscriptionPlan plan, BillingCycle billingCycle, BigDecimal price) {
        PlanPricing pricing = new PlanPricing();
        pricing.setPlan(plan);
        pricing.setBillingCycle(billingCycle);
        pricing.setPrice(price);
        pricing.setOriginalPrice(price);
        pricing.setDiscountPercentage(BigDecimal.ZERO);
        pricing.setCurrency("USD");
        pricing.setActive(true);
        pricing.setRecommended(billingCycle == BillingCycle.ANNUAL);
        pricing.setDisplayOrder(billingCycle.ordinal());

        plan.addPricingOption(pricing);
    }

    /**
     * Get appropriate unit for feature types with limits
     */
    private String getUnitForFeatureType(FeatureType featureType) {
        return switch (featureType) {
            case CV_GENERATION -> "CVs";
            case API_ACCESS -> "API calls";
            case TEAM_COLLABORATION -> "users";
            case STORAGE_SPACE -> "GB";
            default -> "units";
        };
    }
}
