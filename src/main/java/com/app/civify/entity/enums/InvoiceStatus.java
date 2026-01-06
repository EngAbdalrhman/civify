package com.app.civify.entity.enums;

/**
 * Status of an invoice in the billing lifecycle
 */
public enum InvoiceStatus {
    /**
     * Invoice created, awaiting payment
     */
    PENDING,
    
    /**
     * Payment completed successfully
     */
    PAID,
    
    /**
     * Invoice expired (subscription downgraded without payment)
     */
    EXPIRED,
    
    /**
     * Invoice cancelled (user cancelled autoRenew)
     */
    CANCELLED
}
