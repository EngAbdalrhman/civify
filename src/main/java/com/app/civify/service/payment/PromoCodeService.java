package com.app.civify.service.payment;

import com.app.civify.entity.PromoCode;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;

import java.util.Optional;

public interface PromoCodeService {

    PagedResponse<PromoCode> filterPromoCodes(InputDataModel inputData);

    PromoCode createPromoCode(PromoCode promoCode);

    PromoCode updatePromoCode(Long id, PromoCode promoCode);

    PromoCode deactivatePromoCode(Long id);

    void deletePromoCode(Long id);

    Optional<PromoCode> getPromoCodeById(Long id);
}
