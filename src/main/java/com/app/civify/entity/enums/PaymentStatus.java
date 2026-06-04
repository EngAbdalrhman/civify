package com.app.civify.entity.enums;

/**
 * Status of a payment transaction
 */
public enum PaymentStatus {
    /**
     * Checkout/intention created at payment initiation
     */
    INITIATED,

    /**
     * Card authorized but not yet captured
     */
    AUTHORIZED,

    /**
     * Payment is pending (awaiting customer or gateway)
     */
    PENDING,
    
    /**
     * Payment completed successfully
     */
    COMPLETED,
    
    /**
     * Payment failed or was declined
     */
    FAILED,
    
    /**
     * Payment was refunded
     */
    REFUNDED,
    
    /**
     * Payment was cancelled by user
     */
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == REFUNDED || this == CANCELLED;
    }

    public boolean isInProgress() {
        return this == INITIATED || this == AUTHORIZED || this == PENDING;
    }

    /**
     * Maps ledger status to the coarse transaction status stored on {@code billing_transactions}.
     */
    public static PaymentStatus fromLedgerStatus(PaymentLedgerStatus ledgerStatus) {
        if (ledgerStatus == null) {
            return PENDING;
        }
        return switch (ledgerStatus) {
            case INITIATED -> INITIATED;
            case AUTHORIZED -> AUTHORIZED;
            case PENDING -> PENDING;
            case COMPLETED -> COMPLETED;
            case FAILED -> FAILED;
            case REFUNDED -> REFUNDED;
            case CANCELLED -> CANCELLED;
        };
    }
}
