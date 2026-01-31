package com.app.civify.mapper;

import com.app.civify.dto.plan.SimplePlanDTO;
import com.app.civify.entity.plans.SubscriptionPlan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

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
    @Mapping(target = "hasApiAccess", source = "hasApiAccess")
    SimplePlanDTO toSimplePlan(SubscriptionPlan plan);

    List<SimplePlanDTO> toSimplePlans(List<SubscriptionPlan> plans);
}
