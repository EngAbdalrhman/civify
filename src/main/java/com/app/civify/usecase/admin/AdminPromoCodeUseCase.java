package com.app.civify.usecase.admin;

import com.app.civify.dto.plan.CreatePromoCodeRequest;
import com.app.civify.dto.plan.PromoCodeDto;
import com.app.civify.dto.plan.UpdatePromoCodeRequest;
import com.app.civify.entity.plans.PromoCode;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;

public interface AdminPromoCodeUseCase {
    PagedResponse<PromoCodeDto> filterPromoCodes(InputDataModel inputData);
    PromoCodeDto createPromoCode(CreatePromoCodeRequest request);
    PromoCodeDto updatePromoCode(Long id, UpdatePromoCodeRequest request);
    PromoCodeDto deactivatePromoCode(Long id);
    void deletePromoCode(Long id);
}
