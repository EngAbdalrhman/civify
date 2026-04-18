package com.app.civify.service.billing;

import com.app.civify.dto.payment.PaymentMethods;
import com.app.civify.entity.User;
import com.app.civify.entity.billing.Invoice;
import com.app.civify.entity.plans.PromoCode;
import com.app.civify.entity.plans.SubscriptionPlan;
import com.app.civify.entity.plans.UserSubscription;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceService {

    /**
     * Create a new invoice for initial subscription payment.
     * Invoice is created without paymentUrl (gateway-agnostic).
     */
    Invoice createInitialInvoice(User user, SubscriptionPlan plan, BigDecimal amount, 
                                  PromoCode promoCode, BigDecimal discountAmount, 
                                  boolean isAnnual, String currency);

    /**
     * Create a renewal invoice for an existing subscription.
     * Returns empty if a PENDING invoice already exists.
     */
    Optional<Invoice> createRenewalInvoice(UserSubscription subscription, BigDecimal amount, String currency);

    /**
     * Generate payment link for an invoice.
     * Called when user selects gateway on checkout page.
     */
    Invoice processPayment(UUID invoiceUuid, String gateway, PaymentMethods method);

    /**
     * Mark invoice as paid. Called by webhook after verifying success.
     */
    void markPaid(String paymentTransactionId);

    /**
     * Mark invoice as paid by its ID (for auto-charge flows where no paymentTransactionId is available).
     */
    void markPaidById(Long invoiceId);

    /**
     * Expire all pending invoices for a subscription.
     * Called when subscription is downgraded.
     */
    void expirePendingInvoices(Long subscriptionId);

    /**
     * Cancel all pending invoices for a subscription.
     * Called when user cancels autoRenew.
     */
    void cancelPendingInvoices(Long subscriptionId);

    /**
     * Send reminder email for existing pending invoice.
     */
    void sendReminderEmail(Invoice invoice);

    /**
     * Get invoice by UUID for checkout page.
     */
    Optional<Invoice> getByUuid(UUID uuid);

    /**
     * Get all invoices for a user (billing history).
     */
    List<Invoice> getUserInvoices(Long userId);

    /**
     * Check if a pending invoice exists for subscription.
     */
    Optional<Invoice> findPendingInvoice(Long subscriptionId);

    /**
     * Handle successful payment for an invoice.
     * Marks invoice as PAID and extends subscription if it's a renewal.
     * @return true if an invoice was found and processed
     */
    boolean handleSuccessfulPayment(String paymentTransactionId);
}
