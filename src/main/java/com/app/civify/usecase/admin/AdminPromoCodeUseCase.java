package com.app.civify.usecase.admin;

import com.app.civify.entity.plans.PromoCode;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;

public interface AdminPromoCodeUseCase {
    PagedResponse<PromoCode> filterPromoCodes(InputDataModel inputData);
    PromoCode createPromoCode(PromoCode promoCode);
    PromoCode updatePromoCode(Long id, PromoCode promoCode);
    PromoCode deactivatePromoCode(Long id);
    void deletePromoCode(Long id);
}
