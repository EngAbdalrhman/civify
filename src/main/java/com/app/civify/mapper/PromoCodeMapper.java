package com.app.civify.mapper;

import com.app.civify.dto.plan.PromoCodeDto;
import com.app.civify.entity.plans.PromoCode;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PromoCodeMapper {
    PromoCodeDto toDto(PromoCode promoCode);
}
