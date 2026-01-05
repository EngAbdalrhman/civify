package com.app.civify.service.plans;

import com.app.civify.dto.plan.PlanPriceDto;
import com.app.civify.dto.plan.PlansWithCurrencyResponse;
import com.app.civify.entity.plans.PlanCurrencyPrice;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service for plan pricing with multi-currency support
 */
public interface PlanPricingService {

    /**
     * Get plan price in a specific currency.
     * Uses override if exists, otherwise converts from USD base price.
     */
    PlanPriceDto getPlanPrice(Long planId, String currencyCode);

    /**
     * Get all active plans with prices in user's detected currency
     */
    PlansWithCurrencyResponse getAllPlansForUser(HttpServletRequest request);

    /**
     * Get all active plans with prices in specified currency
     */
    PlansWithCurrencyResponse getAllPlansInCurrency(String currencyCode);

    /**
     * Set a price override for a plan in a specific currency
     */
    PlanCurrencyPrice setPriceOverride(Long planId, String currencyCode, BigDecimal monthlyPrice, BigDecimal annualPrice);

    /**
     * Remove price override for a plan (will fall back to conversion)
     */
    void removePriceOverride(Long planId, String currencyCode);

    /**
     * Get all price overrides for a plan
     */
    List<PlanCurrencyPrice> getPriceOverrides(Long planId);
}
