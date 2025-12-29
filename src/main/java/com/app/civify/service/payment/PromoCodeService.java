package com.app.civify.service.payment;

import com.app.civify.dto.plan.PromoCodeDto;
import com.app.civify.entity.plans.PromoCode;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;

import java.util.Optional;

public interface PromoCodeService {

    PagedResponse<PromoCodeDto> filterPromoCodes(InputDataModel inputData);

    PromoCode createPromoCode(PromoCode promoCode);

    PromoCode updatePromoCode(Long id, PromoCode promoCode);

    PromoCode deactivatePromoCode(Long id);

    void deletePromoCode(Long id);

    Optional<PromoCode> getPromoCodeById(Long id);

    /**
     * Validate a promo code for checkout
     * @param code The promo code string
     * @param amount The purchase amount
     * @return Optional containing the promo code if valid, empty if invalid
     */
    Optional<PromoCode> validatePromoCode(String code, java.math.BigDecimal amount);

    /**
     * Find promo code by code string
     */
    Optional<PromoCode> findByCode(String code);
}
