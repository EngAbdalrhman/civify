package com.app.civify.repository;

import com.app.civify.entity.plans.PaymentTransaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentTransaction, String> {

    Optional<PaymentTransaction> findByPaymentKey(String paymentKey);

    Optional<PaymentTransaction> findByOrderId(String orderId);

    /**
     * Find payment transaction with pessimistic write lock.
     * This ensures no concurrent webhook processing can occur for the same transaction.
     * Critical for preventing duplicate payment processing.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.orderId = :orderId")
    Optional<PaymentTransaction> findByOrderIdWithLock(@Param("orderId") String orderId);

    List<PaymentTransaction> findByUserIdAndStatus(Long userId, String status);

    List<PaymentTransaction> findByGateway(String gateway);
}
