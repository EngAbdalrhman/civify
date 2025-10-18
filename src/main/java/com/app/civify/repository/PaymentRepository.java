package com.app.civify.repository;

import com.app.civify.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentTransaction, String> {

    Optional<PaymentTransaction> findByPaymentKey(String paymentKey);

    Optional<PaymentTransaction> findByOrderId(String orderId);

    List<PaymentTransaction> findByUserIdAndStatus(Long userId, String status);

    List<PaymentTransaction> findByGateway(String gateway);
}
