package com.app.civify.mapper;

import com.app.civify.dto.plan.SimpleFeatureDTO;
import com.app.civify.dto.plan.SimplePlanDTO;
import com.app.civify.entity.plans.PlanFeatureLimit;
import com.app.civify.entity.plans.SubscriptionPlan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MapStruct mapper for SubscriptionPlan entity to SimplePlanDTO.
 */
@Mapper(componentModel = "spring")
public interface SubscriptionPlanMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "details", source = "details")
    @Mapping(target = "monthlyPrice", source = "monthlyPrice")
    @Mapping(target = "annualPrice", source = "annualPrice")
    @Mapping(target = "cvLimit", source = "cvLimit")
    @Mapping(target = "watermark", source = "watermark")
    @Mapping(target = "active", source = "active")
    @Mapping(target = "allowAnnualBilling", source = "allowAnnualBilling")
    @Mapping(target = "renewalAllowed", source = "renewalAllowed")
    @Mapping(target = "sortOrder", source = "sortOrder")
    @Mapping(target = "features", source = "featureLimits", qualifiedByName = "toSimpleFeatures")
    SimplePlanDTO toSimplePlan(SubscriptionPlan plan);

    List<SimplePlanDTO> toSimplePlans(List<SubscriptionPlan> plans);

    @Named("toSimpleFeatures")
    default List<SimpleFeatureDTO> toSimpleFeatures(List<PlanFeatureLimit> featureLimits) {
        if (featureLimits == null || featureLimits.isEmpty()) {
            return Collections.emptyList();
        }
        return featureLimits.stream()
                .filter(fl -> fl.getEnabled() != null && fl.getEnabled())
                .map(fl -> SimpleFeatureDTO.builder()
                        .code(fl.getFeature().getFeatureType().name())
                        .name(fl.getFeature().getName())
                        .limitValue(fl.getLimitValue())
                        .enabled(fl.getEnabled())
                        .build())
                .collect(Collectors.toList());
    }
}

