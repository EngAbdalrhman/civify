package com.app.civify.usecase.user;

import com.app.civify.dto.billing.InvoiceDto;
import com.app.civify.dto.payment.PaymentMethods;

import java.util.List;
import java.util.UUID;

/**
 * UseCase interface for billing operations.
 * Handles invoice retrieval and payment processing.
 */
public interface BillingUseCase {

    /**
     * Get user's invoice history
     */
    List<InvoiceDto> getUserInvoices(Long userId);

    /**
     * Get invoice details by UUID (for checkout page)
     */
    InvoiceDto getInvoiceByUuid(UUID uuid);

    /**
     * Generate payment link for an invoice
     */

    InvoiceDto processPayment(UUID uuid, String gateway);
    InvoiceDto processPayment(UUID uuid, String gateway, PaymentMethods method);
}
