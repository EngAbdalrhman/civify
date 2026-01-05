package com.app.civify.repository.currency;

import com.app.civify.entity.currency.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Long> {

    Optional<Currency> findByCodeIgnoreCase(String code);

    List<Currency> findByActiveTrue();

    Optional<Currency> findByIsDefaultTrue();

    boolean existsByCodeIgnoreCase(String code);
}

