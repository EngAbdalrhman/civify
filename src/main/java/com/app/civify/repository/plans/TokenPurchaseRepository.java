package com.app.civify.repository.plans;

import com.app.civify.entity.plans.TokenPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenPurchaseRepository extends JpaRepository<TokenPurchase, Long> {
    Optional<TokenPurchase> findByPaymentTransactionId(String paymentTransactionId);
}
