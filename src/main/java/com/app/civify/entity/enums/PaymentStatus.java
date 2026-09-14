package com.app.civify.entity.enums;

/**
 * Status of a payment transaction
 */
public enum PaymentStatus {
    /**
     * Payment is pending (not yet completed)
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
    CANCELLED
}
