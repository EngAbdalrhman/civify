package com.app.civify.entity.enums;

/**
 * Types of billing/payment events for audit logging
 */
public enum BillingAuditType {
    /**
     * Payment gateway webhook was received
     */
    WEBHOOK_RECEIVED,
    
    /**
     * Webhook was processed successfully
     */
    WEBHOOK_SUCCESS,
    
    /**
     * Webhook processing failed
     */
    WEBHOOK_FAILED,
    
    /**
     * Payment was successfully completed
     */
    PAYMENT_COMPLETED,
    
    /**
     * Payment failed
     */
    PAYMENT_FAILED,
    
    /**
     * Invoice was cancelled by user or admin
     */
    INVOICE_CANCELLED,
    
    /**
     * Invoice expired without payment
     */
    INVOICE_EXPIRED,
    
    /**
     * Refund was initiated
     */
    REFUND_INITIATED
}
