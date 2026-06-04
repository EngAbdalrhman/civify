package com.app.civify.entity.enums;

/**
 * Fine-grained payment lifecycle status recorded in the payment ledger.
 */
public enum PaymentLedgerStatus {

    /** Checkout/intention created — payment flow started */
    INITIATED,

    /** Funds authorized but not yet captured (card auth) */
    AUTHORIZED,

    /** Awaiting customer action or gateway confirmation */
    PENDING,

    /** Payment captured and settled successfully */
    COMPLETED,

    /** Payment declined or failed */
    FAILED,

    /** Payment refunded */
    REFUNDED,

    /** Payment cancelled by user or system */
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == REFUNDED || this == CANCELLED;
    }

    public boolean isInProgress() {
        return this == INITIATED || this == AUTHORIZED || this == PENDING;
    }
}
