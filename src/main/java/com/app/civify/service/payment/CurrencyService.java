package com.app.civify.service.payment;

import com.app.civify.repository.billing.CurrencyConversionRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Precision-safe currency conversion service for multi-gateway pricing.
 * 
 * Reads conversion rates from the database first (currency_conversion_rates table).
 * Falls back to application.properties if no DB row exists.
 * All arithmetic uses BigDecimal with RoundingMode.HALF_UP — no floating point ever touches money.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyService {

    private final CurrencyConversionRateRepository conversionRateRepository;

    @Value("${payment.currency.usd-to-egp-rate:50.00}")
    private String usdToEgpRateFallback;

    /**
     * Convert USD amount to EGP using the current rate.
     * Result is rounded to 2 decimal places (standard currency precision).
     */
    public BigDecimal usdToEgp(BigDecimal usdAmount) {
        if (usdAmount == null) {
            return BigDecimal.ZERO;
        }
        return usdAmount.multiply(getUsdToEgpRate())
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Convert EGP amount to USD using the current rate.
     * Result is rounded to 2 decimal places.
     */
    public BigDecimal egpToUsd(BigDecimal egpAmount) {
        if (egpAmount == null) {
            return BigDecimal.ZERO;
        }
        return egpAmount.divide(getUsdToEgpRate(), 2, RoundingMode.HALF_UP);
    }

    /**
     * Get the native currency code for a given gateway.
     */
    public String getGatewayCurrency(String gatewayName) {
        if ("dodo".equalsIgnoreCase(gatewayName)) {
            return "USD";
        }
        return "EGP"; // Paymob, Fawaterak — all MENA gateways use EGP
    }

    /**
     * Convert a USD-denominated plan price to the gateway's native currency.
     * Dodo → returns USD as-is. Paymob → converts to EGP.
     */
    public BigDecimal convertForGateway(BigDecimal usdAmount, String gatewayName) {
        if ("dodo".equalsIgnoreCase(gatewayName)) {
            return usdAmount.setScale(2, RoundingMode.HALF_UP);
        }
        return usdToEgp(usdAmount);
    }

    /**
     * Get the current USD to EGP conversion rate dynamically.
     */
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

