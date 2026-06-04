package com.app.civify.service.payment;

import com.app.civify.entity.billing.BillingTransaction;
import com.app.civify.entity.billing.PaymentLedgerEntry;
import com.app.civify.entity.enums.PaymentLedgerStatus;
import com.app.civify.entity.enums.PaymentStatus;
import com.app.civify.repository.billing.PaymentLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Append-only ledger for payment status transitions (init, authorized, completed, etc.).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentLedgerService {

    private final PaymentLedgerRepository ledgerRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = Exception.class)
    public void record(BillingTransaction transaction, PaymentLedgerStatus status, String gatewayStatus,
                       String source, String note) {
        if (transaction == null || transaction.getId() == null) {
            log.warn("Skipping payment ledger entry — transaction or id is null");
            return;
        }

        PaymentLedgerEntry entry = PaymentLedgerEntry.builder()
                .transactionId(transaction.getId())
                .orderId(transaction.getOrderId())
                .userId(transaction.getUserId())
                .gateway(transaction.getGateway())
                .status(status)
                .gatewayStatus(truncate(gatewayStatus, 100))
                .source(truncate(source, 50))
                .note(truncate(note, 500))
                .build();

        ledgerRepository.save(entry);
        log.info("Payment ledger: txn={} order={} status={} gatewayStatus={} source={}",
                transaction.getId(), transaction.getOrderId(), status, gatewayStatus, source);
    }

    /**
     * Records a ledger entry and updates the billing transaction when the transition is allowed.
     */
    @Transactional
    public void transition(BillingTransaction transaction, PaymentLedgerStatus ledgerStatus,
                           String gatewayStatus, String source, String note) {
        if (transaction == null) {
            return;
        }

        PaymentStatus targetStatus = PaymentStatus.fromLedgerStatus(ledgerStatus);
        if (canTransition(transaction.getStatus(), targetStatus)) {
            transaction.setStatus(targetStatus);
            if (targetStatus == PaymentStatus.COMPLETED) {
                transaction.setCompletedAt(LocalDateTime.now());
            }
        }

        record(transaction, ledgerStatus, gatewayStatus, source, note);
    }

    public void recordInitiated(BillingTransaction transaction, String source) {
        transition(transaction, PaymentLedgerStatus.INITIATED, "initiated", source, "Payment initiated");
    }

    public void recordAuthorized(BillingTransaction transaction, String gatewayStatus, String source) {
        transition(transaction, PaymentLedgerStatus.AUTHORIZED, gatewayStatus, source, "Payment authorized");
    }

    public void recordCompleted(BillingTransaction transaction, String gatewayStatus, String source) {
        transition(transaction, PaymentLedgerStatus.COMPLETED, gatewayStatus, source, "Payment completed");
    }

    public void recordFailed(BillingTransaction transaction, String gatewayStatus, String source, String note) {
        transition(transaction, PaymentLedgerStatus.FAILED, gatewayStatus, source, note);
    }

    public void recordRefunded(BillingTransaction transaction, String gatewayStatus, String source) {
        transition(transaction, PaymentLedgerStatus.REFUNDED, gatewayStatus, source, "Payment refunded");
    }

    public void recordCancelled(BillingTransaction transaction, String gatewayStatus, String source) {
        transition(transaction, PaymentLedgerStatus.CANCELLED, gatewayStatus, source, "Payment cancelled");
    }

    public List<PaymentLedgerEntry> getHistory(String transactionId) {
        return ledgerRepository.findByTransactionIdOrderByCreatedAtAsc(transactionId);
    }

    public List<PaymentLedgerEntry> getHistoryByOrderId(String orderId) {
        return ledgerRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
    }

    /**
     * Resolves Paymob callback/redirect payload fields into a ledger status.
     */
    public PaymentLedgerStatus resolvePaymobStatus(boolean success, boolean pending, boolean isAuth) {
        if (success) {
            return PaymentLedgerStatus.COMPLETED;
        }
        if (pending || isAuth) {
            return PaymentLedgerStatus.AUTHORIZED;
        }
        return PaymentLedgerStatus.FAILED;
    }

    /**
     * Resolves generic gateway status strings (Fawaterak, Dodo, etc.).
     */
    public PaymentLedgerStatus resolveGatewayStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return PaymentLedgerStatus.PENDING;
        }
        String normalized = rawStatus.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("success") || normalized.equals("paid") || normalized.equals("captured")
                || normalized.equals("completed") || normalized.equals("succeeded")) {
            return PaymentLedgerStatus.COMPLETED;
        }
        if (normalized.contains("author") || normalized.equals("auth")) {
            return PaymentLedgerStatus.AUTHORIZED;
        }
        if (normalized.contains("init")) {
            return PaymentLedgerStatus.INITIATED;
        }
        if (normalized.contains("pending") || normalized.equals("open") || normalized.equals("processing")) {
            return PaymentLedgerStatus.PENDING;
        }
        if (normalized.contains("refund")) {
            return PaymentLedgerStatus.REFUNDED;
        }
        if (normalized.contains("cancel")) {
            return PaymentLedgerStatus.CANCELLED;
        }
        if (normalized.contains("fail") || normalized.contains("declin") || normalized.equals("expired")
                || normalized.equals("voided")) {
            return PaymentLedgerStatus.FAILED;
        }
        return PaymentLedgerStatus.PENDING;
    }

    private boolean canTransition(PaymentStatus current, PaymentStatus target) {
        if (current == null) {
            return true;
        }
        if (current == target) {
            return false;
        }
        if (current.isTerminal()) {
            return target == PaymentStatus.REFUNDED
                    || (current == PaymentStatus.FAILED && target == PaymentStatus.COMPLETED);
        }
        return true;
    }

    private static String truncate(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }
}
