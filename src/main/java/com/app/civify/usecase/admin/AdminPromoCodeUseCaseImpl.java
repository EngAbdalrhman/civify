package com.app.civify.usecase.admin;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPromoCodeUseCaseImpl extends CommonService implements AdminPromoCodeUseCase {

    private final PromoCodeService promoCodeService;
    private final AuditService auditService;

    @Override
    public PagedResponse<PromoCode> filterPromoCodes(InputDataModel inputData) {
        log.debug("Filtering promo codes with criteria: {}", inputData);
        return promoCodeService.filterPromoCodes(inputData);
    }

    @Override
    @Transactional
    public PromoCode createPromoCode(PromoCode promoCode) {
        PromoCode created = promoCodeService.createPromoCode(promoCode);
        logAudit(created.getId(), AuditAction.CREATE, null, promoToMap(created),
                "Created promo code: " + created.getCode());
        return created;
    }

    @Override
    @Transactional
    public PromoCode updatePromoCode(Long id, PromoCode promoCode) {
        PromoCode updated = promoCodeService.updatePromoCode(id, promoCode);
        logAudit(id, AuditAction.UPDATE, null, promoToMap(updated),
                "Updated promo code: " + updated.getCode());
        return updated;
    }

    @Override
    @Transactional
    public PromoCode deactivatePromoCode(Long id) {
        PromoCode deactivated = promoCodeService.deactivatePromoCode(id);
        logAudit(id, AuditAction.UPDATE, null, null,
                "Deactivated promo code: " + deactivated.getCode());
        return deactivated;
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
