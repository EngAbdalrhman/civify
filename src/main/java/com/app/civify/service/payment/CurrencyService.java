package com.app.civify.service.payment;

import com.app.civify.repository.billing.CurrencyConversionRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Precision-safe currency conversion for multi-gateway pricing.
 * Base currency is configured via {@code payment.pricing.base-currency} (USD or EGP).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyService {

    private final CurrencyConversionRateRepository conversionRateRepository;

    @Value("${payment.currency.usd-to-egp-rate:50.00}")
    private String usdToEgpRateFallback;

    @Value("${payment.pricing.base-currency:USD}")
    private String pricingBaseCurrency;

    public BigDecimal usdToEgp(BigDecimal usdAmount) {
        if (usdAmount == null) {
            return BigDecimal.ZERO;
        }
        return usdAmount.multiply(getUsdToEgpRate())
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal egpToUsd(BigDecimal egpAmount) {
        if (egpAmount == null) {
            return BigDecimal.ZERO;
        }
        return egpAmount.divide(getUsdToEgpRate(), 2, RoundingMode.HALF_UP);
    }

    public String getGatewayCurrency(String gatewayName) {
        if ("dodo".equalsIgnoreCase(gatewayName)) {
            return "USD";
        }
        return "EGP";
    }

    /**
     * Converts a plan/package price stored in {@link #getPricingBaseCurrency()} to the gateway's charge currency.
     */
    public BigDecimal convertForGateway(BigDecimal baseAmount, String gatewayName) {
        if (baseAmount == null) {
            return BigDecimal.ZERO;
        }
        if (isUsdBase()) {
            if ("dodo".equalsIgnoreCase(gatewayName)) {
                return baseAmount.setScale(2, RoundingMode.HALF_UP);
            }
            return usdToEgp(baseAmount);
        }
        if ("dodo".equalsIgnoreCase(gatewayName)) {
            return egpToUsd(baseAmount);
        }
        return baseAmount.setScale(2, RoundingMode.HALF_UP);
    }

    public boolean isUsdBase() {
        return "USD".equalsIgnoreCase(pricingBaseCurrency);
    }

    public String getPricingBaseCurrency() {
        return isUsdBase() ? "USD" : "EGP";
    }

    public BigDecimal getUsdToEgpRate() {
        try {
            return conversionRateRepository
                    .findByFromCurrencyAndToCurrency("USD", "EGP")
                    .map(rate -> rate.getRate())
                    .orElseGet(() -> new BigDecimal(usdToEgpRateFallback));
        } catch (Exception e) {
            log.error("Failed to query database for USD→EGP rate: {}. Using properties fallback.", e.getMessage());
            return new BigDecimal(usdToEgpRateFallback);
        }
    }
}
