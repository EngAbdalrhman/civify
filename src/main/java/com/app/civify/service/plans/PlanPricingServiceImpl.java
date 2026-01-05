package com.app.civify.service.plans;

import com.app.civify.config.PaymentGatewayConfig;
import com.app.civify.dto.plan.PlanPriceDto;
import com.app.civify.dto.plan.PlansWithCurrencyResponse;
import com.app.civify.entity.currency.Currency;
import com.app.civify.entity.plans.PlanCurrencyPrice;
import com.app.civify.entity.plans.SubscriptionPlan;
import com.app.civify.exception.ResourceNotFoundException;
import com.app.civify.repository.plans.PlanCurrencyPriceRepository;
import com.app.civify.repository.plans.SubscriptionPlanRepository;
import com.app.civify.service.currency.CurrencyService;
import com.app.civify.service.geo.GeoLocationResult;
import com.app.civify.service.geo.GeoLocationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanPricingServiceImpl implements PlanPricingService {

    private final SubscriptionPlanRepository planRepository;
    private final PlanCurrencyPriceRepository currencyPriceRepository;
    private final CurrencyService currencyService;
    private final GeoLocationService geoLocationService;
    private final PaymentGatewayConfig paymentGatewayConfig;

    @Override
    @Cacheable(value = "planPrice", key = "#planId + ':' + #currencyCode.toUpperCase()")
    public PlanPriceDto getPlanPrice(Long planId, String currencyCode) {
        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + planId));

        Currency currency = currencyService.getCurrency(currencyCode);

        // Check for override price
        Optional<PlanCurrencyPrice> override = currencyPriceRepository
                .findByPlanIdAndCurrencyCodeIgnoreCase(planId, currencyCode);

        if (override.isPresent()) {
            PlanCurrencyPrice priceOverride = override.get();
            return PlanPriceDto.builder()
                    .planId(planId)
                    .planName(plan.getName())
                    .monthlyPrice(priceOverride.getMonthlyPrice())
                    .annualPrice(priceOverride.getAnnualPrice())
                    .currencyCode(currency.getCode())
                    .currencySymbol(currency.getSymbol())
                    .isOverride(true)
                    .build();
        }

        // No override - convert from USD (base prices are in USD)
        BigDecimal monthlyConverted = currencyService.convertFromUsd(plan.getMonthlyPrice(), currencyCode);
        BigDecimal annualConverted = currencyService.convertFromUsd(plan.getAnnualPrice(), currencyCode);

        return PlanPriceDto.builder()
                .planId(planId)
                .planName(plan.getName())
                .monthlyPrice(monthlyConverted)
                .annualPrice(annualConverted)
                .currencyCode(currency.getCode())
                .currencySymbol(currency.getSymbol())
                .isOverride(false)
                .build();
    }

    @Override
    public PlansWithCurrencyResponse getAllPlansForUser(HttpServletRequest request) {
        // Detect user's country from IP
        GeoLocationResult location = geoLocationService.getLocationFromRequest(request);
        String countryCode = location.getCountryCode();

        log.debug("Detected country code: {} for IP: {}", countryCode, location.getIp());

        // Get currency for country
        Currency currency = currencyService.getCurrencyForCountry(countryCode);

        return getAllPlansInCurrency(currency.getCode());
    }

    @Override
    @Cacheable(value = "plansWithCurrency", key = "#currencyCode.toUpperCase()")
    public PlansWithCurrencyResponse getAllPlansInCurrency(String currencyCode) {
        Currency currency = currencyService.getCurrency(currencyCode);

        List<SubscriptionPlan> activePlans = planRepository.findByActiveTrue();

        List<PlanPriceDto> planPrices = activePlans.stream()
                .map(plan -> getPlanPrice(plan.getId(), currencyCode))
                .collect(Collectors.toList());

        List<String> availableGateways = paymentGatewayConfig.getAvailableGatewaysForCurrency(currencyCode);

        return PlansWithCurrencyResponse.builder()
                .currency(PlansWithCurrencyResponse.CurrencyInfo.builder()
                        .code(currency.getCode())
                        .symbol(currency.getSymbol())
                        .name(currency.getName())
                        .build())
                .plans(planPrices)
                .availableGateways(availableGateways)
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = {"planPrice", "plansWithCurrency"}, allEntries = true)
    public PlanCurrencyPrice setPriceOverride(Long planId, String currencyCode, BigDecimal monthlyPrice, BigDecimal annualPrice) {
        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + planId));

        // Validate currency exists
        currencyService.getCurrency(currencyCode);

        // Find or create override
        PlanCurrencyPrice override = currencyPriceRepository
                .findByPlanIdAndCurrencyCodeIgnoreCase(planId, currencyCode)
                .orElse(PlanCurrencyPrice.builder()
                        .plan(plan)
                        .currencyCode(currencyCode.toUpperCase())
                        .build());

        override.setMonthlyPrice(monthlyPrice);
        override.setAnnualPrice(annualPrice);

        PlanCurrencyPrice saved = currencyPriceRepository.save(override);
        log.info("Set price override for plan {} in {}: monthly={}, annual={}",
                planId, currencyCode, monthlyPrice, annualPrice);

        return saved;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"planPrice", "plansWithCurrency"}, allEntries = true)
    public void removePriceOverride(Long planId, String currencyCode) {
        currencyPriceRepository.deleteByPlanIdAndCurrencyCodeIgnoreCase(planId, currencyCode);
        log.info("Removed price override for plan {} in {}", planId, currencyCode);
    }

    @Override
    public List<PlanCurrencyPrice> getPriceOverrides(Long planId) {
        return currencyPriceRepository.findByPlanId(planId);
    }
}
