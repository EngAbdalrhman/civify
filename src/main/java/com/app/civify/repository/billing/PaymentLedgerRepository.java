package com.app.civify.repository.billing;

import com.app.civify.entity.billing.PaymentLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentLedgerRepository extends JpaRepository<PaymentLedgerEntry, Long> {

    List<PaymentLedgerEntry> findByTransactionIdOrderByCreatedAtAsc(String transactionId);

    List<PaymentLedgerEntry> findByOrderIdOrderByCreatedAtAsc(String orderId);
}
