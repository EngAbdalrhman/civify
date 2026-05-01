package com.app.civify.dto.plan;

import com.app.civify.entity.plans.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between Plan entities and DTOs
 */
@Component
public class PlanMapper {

    /**
     * Convert SubscriptionPlan entity to PlanDetailResponse DTO
     */
    public PlanDetailResponse toDetailResponse(SubscriptionPlan plan) {
        if (plan == null) {
            return null;
        }

        return PlanDetailResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .details(plan.getDetails())
                .cvLimit(plan.getCvLimit())
                .jobMatchLimit(plan.getJobMatchLimit())
                .tokenLimit(plan.getTokenLimit())
                .watermark(plan.getWatermark())
                .monthlyPrice(plan.getMonthlyPrice())
                .annualPrice(plan.getAnnualPrice())
                .active(plan.getActive())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                // Billing restrictions
                .allowAnnualBilling(plan.getAllowAnnualBilling() != null ? plan.getAllowAnnualBilling() : false)
                .renewalAllowed(plan.getRenewalAllowed() != null ? plan.getRenewalAllowed() : false)
                .maxBillingCycles(plan.getMaxBillingCycles())
                // Display settings
                .isFeatured(plan.getIsFeatured())
                .sortOrder(plan.getSortOrder())
                .hasApiAccess(plan.getHasApiAccess())
                .allowTranslation(plan.getAllowTranslation())
                // Linked group
                .linkedGroupId(plan.getLinkedGroup() != null ? plan.getLinkedGroup().getId() : null)
                .linkedGroupName(plan.getLinkedGroup() != null ? plan.getLinkedGroup().getName() : null)
                // Collections
                .features(toFeatureLimitDTOs(plan.getFeatureLimits()))
                .pricingOptions(toPricingDTOs(plan.getPricingOptions()))
                .discounts(toDiscountDTOs(plan.getDiscounts()))
                .integrations(toIntegrationDTOs(plan.getIntegrations()))
                .build();
    }

    /**
     * Convert list of PlanFeatureLimit to FeatureLimitDTO list
     */
    public List<FeatureLimitDTO> toFeatureLimitDTOs(List<PlanFeatureLimit> featureLimits) {
        if (featureLimits == null) {
            return new ArrayList<>();
        }

        return featureLimits.stream()
                .map(this::toFeatureLimitDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convert PlanFeatureLimit to FeatureLimitDTO
     */
    public FeatureLimitDTO toFeatureLimitDTO(PlanFeatureLimit featureLimit) {
        if (featureLimit == null) {
            return null;
        }

        PlanFeature feature = featureLimit.getFeature();

        return FeatureLimitDTO.builder()
                .id(featureLimit.getId())
                .featureId(feature.getId())
                .featureType(feature.getFeatureType())
                .featureName(feature.getName())
                .featureDescription(feature.getDescription())
                .limitValue(featureLimit.getLimitValue())
                .enabled(featureLimit.getEnabled())
                .unit(feature.getUnit())
                .metadata(featureLimit.getMetadata())
                .displayOrder(featureLimit.getDisplayOrder())
                .hasLimit(feature.getHasLimit())
                .build();
    }

    /**
     * Convert list of PlanPricing to PricingDTO list
     */
    public List<PricingDTO> toPricingDTOs(List<PlanPricing> pricingOptions) {
        if (pricingOptions == null) {
            return new ArrayList<>();
        }

        return pricingOptions.stream()
                .map(this::toPricingDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convert PlanPricing to PricingDTO
     */
    public PricingDTO toPricingDTO(PlanPricing pricing) {
        if (pricing == null) {
            return null;
        }

        return PricingDTO.builder()
                .id(pricing.getId())
                .billingCycle(pricing.getBillingCycle())
                .price(pricing.getPrice())
                .originalPrice(pricing.getOriginalPrice())
                .discountPercentage(pricing.getDiscountPercentage())
                .currency(pricing.getCurrency())
                .active(pricing.getActive())
                .displayOrder(pricing.getDisplayOrder())
                .recommended(pricing.getRecommended())
                .effectiveMonthlyPrice(pricing.getEffectiveMonthlyPrice())
                .build();
    }

    /**
     * Convert list of PlanDiscount to DiscountDTO list
     */
    public List<DiscountDTO> toDiscountDTOs(List<PlanDiscount> discounts) {
        if (discounts == null) {
            return new ArrayList<>();
        }

        return discounts.stream()
                .map(this::toDiscountDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convert PlanDiscount to DiscountDTO
     */
    public DiscountDTO toDiscountDTO(PlanDiscount discount) {
        if (discount == null) {
            return null;
        }

        return DiscountDTO.builder()
                .id(discount.getId())
                .name(discount.getName())
                .description(discount.getDescription())
                .discountType(discount.getDiscountType())
                .discountValue(discount.getDiscountValue())
                .billingCycle(discount.getBillingCycle())
                .startDate(discount.getStartDate())
                .endDate(discount.getEndDate())
                .active(discount.getActive())
                .maxUses(discount.getMaxUses())
                .currentUses(discount.getCurrentUses())
                .priority(discount.getPriority())
                .isValid(discount.isValid())
                .build();
    }

    /**
     * Convert list of PlanIntegration to IntegrationDTO list
     */
    public List<IntegrationDTO> toIntegrationDTOs(List<PlanIntegration> integrations) {
        if (integrations == null) {
            return new ArrayList<>();
        }

        return integrations.stream()
                .map(this::toIntegrationDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convert PlanIntegration to IntegrationDTO
     */
    public IntegrationDTO toIntegrationDTO(PlanIntegration integration) {
        if (integration == null) {
            return null;
        }

        return IntegrationDTO.builder()
                .id(integration.getId())
                .integrationType(integration.getIntegrationType())
                .integrationName(integration.getIntegrationName())
                .description(integration.getDescription())
                .enabled(integration.getEnabled())
                .usageLimit(integration.getUsageLimit())
                .usageUnit(integration.getUsageUnit())
                .configuration(integration.getConfiguration())
                .displayOrder(integration.getDisplayOrder())
                .iconUrl(integration.getIconUrl())
                .documentationUrl(integration.getDocumentationUrl())
                .isUnlimited(integration.isUnlimited())
                .build();
    }
}
