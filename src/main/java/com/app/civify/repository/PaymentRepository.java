package com.app.civify.repository;

import com.app.civify.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByPaymentKey(String paymentKey);

    Optional<PaymentTransaction> findByOrderId(String orderId);
}
