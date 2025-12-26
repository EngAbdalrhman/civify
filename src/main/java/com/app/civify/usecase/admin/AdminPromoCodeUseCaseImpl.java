package com.app.civify.usecase.admin;

import com.app.civify.entity.plans.PromoCode;
import com.app.civify.service.payment.PromoCodeService;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import com.app.civify.util.persistence.CommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPromoCodeUseCaseImpl extends CommonService implements AdminPromoCodeUseCase {

    private final PromoCodeService promoCodeService;

    @Override
    public PagedResponse<PromoCode> filterPromoCodes(InputDataModel inputData) {
        log.debug("Filtering promo codes with criteria: {}", inputData);
        // Assuming direct delegation or using CommonService's filterEntities if we wanted to bypass service
        // But PromoCodeService seems to have specific logic, so we delegate.
        return promoCodeService.filterPromoCodes(inputData);
    }

    @Override
    public PromoCode createPromoCode(PromoCode promoCode) {
        return promoCodeService.createPromoCode(promoCode);
    }

    @Override
    public PromoCode updatePromoCode(Long id, PromoCode promoCode) {
        return promoCodeService.updatePromoCode(id, promoCode);
    }

    @Override
    public PromoCode deactivatePromoCode(Long id) {
        return promoCodeService.deactivatePromoCode(id);
    }

    @Override
    public void deletePromoCode(Long id) {
        promoCodeService.deletePromoCode(id);
    }
}
