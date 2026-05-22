package com.app.civify.repository.billing;

import com.app.civify.entity.billing.CurrencyConversionRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CurrencyConversionRateRepository extends JpaRepository<CurrencyConversionRate, Long> {

    Optional<CurrencyConversionRate> findByFromCurrencyAndToCurrency(String fromCurrency, String toCurrency);
}
