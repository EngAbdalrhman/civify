package com.app.civify.usecase.admin;

import com.app.civify.dto.plan.CreatePromoCodeRequest;
import com.app.civify.dto.plan.PromoCodeDto;
import com.app.civify.dto.plan.UpdatePromoCodeRequest;
import com.app.civify.entity.enums.AuditAction;
import com.app.civify.entity.plans.PromoCode;
import com.app.civify.service.audit.AuditService;
import com.app.civify.service.payment.PromoCodeService;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import com.app.civify.util.persistence.CommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPromoCodeUseCaseImpl extends CommonService implements AdminPromoCodeUseCase {

    private final PromoCodeService promoCodeService;
    private final AuditService auditService;
    private final com.app.civify.mapper.PromoCodeMapper promoCodeMapper;

    @Override
    public PagedResponse<PromoCodeDto> filterPromoCodes(InputDataModel inputData) {
        log.debug("Filtering promo codes with criteria: {}", inputData);
        return promoCodeService.filterPromoCodes(inputData);
    }

    @Override
    @Transactional
    public PromoCodeDto createPromoCode(CreatePromoCodeRequest request) {
        PromoCode entity = new PromoCode();
        entity.setCode(request.getCode());
        entity.setDescription(request.getDescription());
        entity.setDiscountType(request.getDiscountType());
        entity.setDiscountValue(request.getDiscountValue());
        entity.setMinimumPurchase(request.getMinimumPurchase());
        entity.setUsageLimit(request.getUsageLimit());
        entity.setExpiresAt(request.getExpiresAt());
        entity.setApplicabilityType(request.getApplicabilityType());
        entity.setMaxDiscountAmount(request.getMaxDiscountAmount());
        entity.setActive(request.isActive());

        PromoCode created = promoCodeService.createPromoCode(entity);
        logAudit(created.getId(), AuditAction.CREATE, null, promoToMap(created),
                "Created promo code: " + created.getCode());
        return promoCodeMapper.toDto(created);
    }

    @Override
    @Transactional
    public PromoCodeDto updatePromoCode(Long id, UpdatePromoCodeRequest request) {
        // We fetch first to have the object for updating, but promoCodeService.updatePromoCode takes an entity
        // We will create a dummy entity with fields to update, or better, we let service handle it.
        // Wait, promoCodeService.updatePromoCode(id, entity) likely copies non-null properties.
        // Let's create an entity with just the updated fields.
        PromoCode updateEntity = new PromoCode();
        updateEntity.setDescription(request.getDescription());
        updateEntity.setDiscountValue(request.getDiscountValue());
        updateEntity.setMinimumPurchase(request.getMinimumPurchase());
        updateEntity.setUsageLimit(request.getUsageLimit());
        updateEntity.setExpiresAt(request.getExpiresAt());
        updateEntity.setActive(request.getActive());
        updateEntity.setApplicabilityType(request.getApplicabilityType());
        updateEntity.setMaxDiscountAmount(request.getMaxDiscountAmount());
        
        PromoCode updated = promoCodeService.updatePromoCode(id, updateEntity);
        logAudit(id, AuditAction.UPDATE, null, promoToMap(updated),
                "Updated promo code: " + updated.getCode());
        return promoCodeMapper.toDto(updated);
    }

    @Override
    @Transactional
    public PromoCodeDto deactivatePromoCode(Long id) {
        PromoCode deactivated = promoCodeService.deactivatePromoCode(id);
        logAudit(id, AuditAction.UPDATE, null, null,
                "Deactivated promo code: " + deactivated.getCode());
        return promoCodeMapper.toDto(deactivated);
    }

    @Override
    @Transactional
    public void deletePromoCode(Long id) {
        promoCodeService.deletePromoCode(id);
        logAudit(id, AuditAction.DELETE, null, null, "Deleted promo code ID: " + id);
    }

    private void logAudit(Long entityId, AuditAction action, Map<String, Object> oldVal,
                          Map<String, Object> newVal, String comment) {
        try {
            auditService.logAudit("PromoCode", entityId.toString(), action, oldVal, newVal, comment);
        } catch (Exception e) {
            log.warn("Failed to log audit for PromoCode action: {}", action, e);
        }
    }

    private Map<String, Object> promoToMap(PromoCode promo) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", promo.getCode());
        map.put("discountType", promo.getDiscountType());
        map.put("discountValue", promo.getDiscountValue());
        map.put("usageLimit", promo.getUsageLimit());
        map.put("active", promo.getActive());
        return map;
    }


}
