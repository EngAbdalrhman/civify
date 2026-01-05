package com.app.civify.repository.plans;

import com.app.civify.entity.plans.PlanCurrencyPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanCurrencyPriceRepository extends JpaRepository<PlanCurrencyPrice, Long> {

    Optional<PlanCurrencyPrice> findByPlanIdAndCurrencyCodeIgnoreCase(Long planId, String currencyCode);

    List<PlanCurrencyPrice> findByPlanId(Long planId);

    List<PlanCurrencyPrice> findByCurrencyCodeIgnoreCase(String currencyCode);

    void deleteByPlanIdAndCurrencyCodeIgnoreCase(Long planId, String currencyCode);
}
