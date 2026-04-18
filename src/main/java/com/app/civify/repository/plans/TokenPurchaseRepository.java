package com.app.civify.repository.plans;

import com.app.civify.entity.plans.TokenPurchase;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TokenPurchaseRepository extends JpaRepository<TokenPurchase, Long> {
    Optional<TokenPurchase> findByPaymentTransactionId(String paymentTransactionId);

    /**
     * Find by payment transaction ID with pessimistic lock to prevent
     * double-fulfillment from concurrent webhook + verify calls.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT tp FROM TokenPurchase tp WHERE tp.paymentTransactionId = :ref")
    Optional<TokenPurchase> findByPaymentTransactionIdForUpdate(@Param("ref") String ref);

    long countByStatus(TokenPurchase.TokenPurchaseStatus status);

    @Query("SELECT COALESCE(SUM(tp.amount), 0) FROM TokenPurchase tp WHERE tp.status = 'COMPLETED'")
    BigDecimal sumCompletedRevenue();

    @Query("SELECT COALESCE(SUM(tp.amount), 0) FROM TokenPurchase tp WHERE tp.status = 'COMPLETED' AND tp.completedAt BETWEEN :start AND :end")
    BigDecimal sumCompletedRevenueByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
