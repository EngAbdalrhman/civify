package com.app.civify.repository.billing;

import com.app.civify.entity.billing.BillingTransaction;
import com.app.civify.entity.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillingTransactionRepository extends JpaRepository<BillingTransaction, String> {

    Optional<BillingTransaction> findByPaymentKey(String paymentKey);

    Optional<BillingTransaction> findByOrderId(String orderId);

    /**
     * Find payment transaction with pessimistic write lock.
     * This ensures no concurrent webhook processing can occur for the same transaction.
     * Critical for preventing duplicate payment processing.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT bt FROM BillingTransaction bt WHERE bt.orderId = :orderId")
    Optional<BillingTransaction> findByOrderIdWithLock(@Param("orderId") String orderId);

    List<BillingTransaction> findByUserIdAndStatus(Long userId, PaymentStatus status);

    List<BillingTransaction> findByGateway(String gateway);

    // Unified revenue queries
    @Query("SELECT COALESCE(SUM(bt.amount), 0) FROM BillingTransaction bt WHERE bt.status = 'COMPLETED'")
    BigDecimal sumCompletedRevenue();

    @Query("SELECT COALESCE(SUM(bt.amount), 0) FROM BillingTransaction bt WHERE bt.status = 'COMPLETED' AND bt.completedAt BETWEEN :start AND :end")
    BigDecimal sumCompletedRevenueByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
