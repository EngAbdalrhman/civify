package com.app.civify.service.currency;

import com.app.civify.entity.currency.Currency;
import com.app.civify.exception.ResourceNotFoundException;
import com.app.civify.repository.currency.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyServiceImpl implements CurrencyService {

    private final CurrencyRepository currencyRepository;

    private static final String DEFAULT_CURRENCY = "USD";

    @Override
    @Cacheable(value = "currencies", key = "'all'")
    public List<Currency> getAllActiveCurrencies() {
        return currencyRepository.findByActiveTrue();
    }

    @Override
    @Cacheable(value = "currency", key = "#code.toUpperCase()")
    public Currency getCurrency(String code) {
        return currencyRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("Currency not found: " + code));
    }

    @Override
    @Cacheable(value = "currency", key = "'default'")
    public Currency getDefaultCurrency() {
        return currencyRepository.findByIsDefaultTrue()
                .orElseGet(() -> currencyRepository.findByCodeIgnoreCase(DEFAULT_CURRENCY)
                        .orElseThrow(() -> new ResourceNotFoundException("Default currency not found")));
    }

    @Override
    public Currency getCurrencyForCountry(String countryCode) {
        if (countryCode == null || countryCode.isEmpty()) {
            return getDefaultCurrency();
        }

        // First try to find currency with this country code
        return currencyRepository.findByActiveTrue().stream()
                .filter(c -> c.isUsedInCountry(countryCode))
                .findFirst()
                .orElseGet(this::getDefaultCurrency);
    }

    @Override
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null || fromCurrency == null || toCurrency == null) {
            return amount;
        }

        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return amount;
        }

        // Convert to USD first, then to target
        BigDecimal inUsd = convertToUsd(amount, fromCurrency);
        return convertFromUsd(inUsd, toCurrency);
    }

    @Override
    public BigDecimal convertFromUsd(BigDecimal amountInUsd, String targetCurrency) {
        if (amountInUsd == null || targetCurrency == null) {
            return amountInUsd;
        }

        if (DEFAULT_CURRENCY.equalsIgnoreCase(targetCurrency)) {
            return amountInUsd.setScale(2, RoundingMode.HALF_UP);
        }

        Currency target = getCurrency(targetCurrency);
        return target.fromUsd(amountInUsd);
    }

    @Override
    public BigDecimal convertToUsd(BigDecimal amount, String sourceCurrency) {
        if (amount == null || sourceCurrency == null) {
            return amount;
        }

        if (DEFAULT_CURRENCY.equalsIgnoreCase(sourceCurrency)) {
            return amount.setScale(4, RoundingMode.HALF_UP);
        }

        Currency source = getCurrency(sourceCurrency);
        return source.toUsd(amount);
    }

    @Override
    public boolean isCurrencySupported(String currencyCode) {
        if (currencyCode == null) return false;
        return currencyRepository.existsByCodeIgnoreCase(currencyCode);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"currency", "currencies"}, allEntries = true)
    public Currency updateExchangeRate(String currencyCode, BigDecimal newRate) {
        Currency currency = getCurrency(currencyCode);
        currency.setRateToUsd(newRate);
        Currency saved = currencyRepository.save(currency);
        log.info("Updated exchange rate for {}: {}", currencyCode, newRate);
        return saved;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"currency", "currencies"}, allEntries = true)
    public Currency saveCurrency(Currency currency) {
        return currencyRepository.save(currency);
    }
}
