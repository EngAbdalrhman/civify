package com.app.civify.service.currency;

import com.app.civify.entity.currency.Currency;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service for currency management and conversion
 */
public interface CurrencyService {

    /**
     * Get all active currencies
     */
    List<Currency> getAllActiveCurrencies();

    /**
     * Get currency by code
     */
    Currency getCurrency(String code);

    /**
     * Get the default currency (USD)
     */
    Currency getDefaultCurrency();

    /**
     * Find currency for a country code (e.g., "EG" -> EGP)
     */
    Currency getCurrencyForCountry(String countryCode);

    /**
     * Convert amount between currencies
     */
    BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency);

    /**
     * Convert amount from USD to target currency
     */
    BigDecimal convertFromUsd(BigDecimal amountInUsd, String targetCurrency);

    /**
     * Convert amount to USD from source currency
     */
    BigDecimal convertToUsd(BigDecimal amount, String sourceCurrency);

    /**
     * Check if currency is supported
     */
    boolean isCurrencySupported(String currencyCode);

    /**
     * Update exchange rate for a currency (admin)
     */
    Currency updateExchangeRate(String currencyCode, BigDecimal newRate);

    /**
     * Create or update a currency (admin)
     */
    Currency saveCurrency(Currency currency);
}
