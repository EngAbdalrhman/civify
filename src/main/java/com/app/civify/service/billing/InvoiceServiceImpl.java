package com.app.civify.service.billing;

import com.app.civify.dto.payment.PaymentCreationResult;
import com.app.civify.entity.User;
import com.app.civify.entity.billing.Invoice;
import com.app.civify.entity.enums.InvoiceStatus;
import com.app.civify.entity.plans.PromoCode;
import com.app.civify.entity.plans.SubscriptionPlan;
import com.app.civify.entity.plans.UserSubscription;
import com.app.civify.exception.ResourceNotFoundException;
import com.app.civify.repository.billing.InvoiceRepository;
import com.app.civify.service.payment.PaymentService;
import com.app.civify.util.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentService paymentService;
    private final EmailService emailService;

    @Value("${app.frontend.base-url}")
    private String frontendUrl;

    @Override
    @Transactional
    public Invoice createInitialInvoice(User user, SubscriptionPlan plan, BigDecimal amount,
                                         PromoCode promoCode, BigDecimal discountAmount,
                                         boolean isAnnual, String currency) {
        Invoice invoice = new Invoice();
        invoice.setUser(user);
        invoice.setPlan(plan);
        invoice.setPromoCode(promoCode);
        invoice.setOriginalAmount(amount.add(discountAmount != null ? discountAmount : BigDecimal.ZERO));
        invoice.setDiscountAmount(discountAmount);
        invoice.setFinalAmount(amount);
        invoice.setCurrency(currency != null ? currency : "EGP");
        invoice.setIsAnnual(isAnnual);
        invoice.setBillingType("INITIAL");
        invoice.setDescription(String.format("%s Plan - %s", plan.getName(), isAnnual ? "Annual" : "Monthly"));
        invoice.setDueDate(LocalDateTime.now().plusDays(1));
        invoice.setStatus(InvoiceStatus.PENDING);

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Created initial invoice {} for user {} - Plan: {}, Amount: {}", 
                saved.getUuid(), user.getEmail(), plan.getName(), amount);
        return saved;
    }

    @Override
    @Transactional
    public Optional<Invoice> createRenewalInvoice(UserSubscription subscription, BigDecimal amount, String currency) {
        Optional<Invoice> existing = findPendingInvoice(subscription.getId());
        if (existing.isPresent()) {
            log.info("Pending invoice {} already exists for subscription {}", 
                    existing.get().getUuid(), subscription.getId());
            return Optional.empty();
        }

        Invoice invoice = new Invoice();
        invoice.setUser(subscription.getUser());
        invoice.setSubscription(subscription);
        invoice.setPlan(subscription.getPlan());
        invoice.setOriginalAmount(amount);
        invoice.setDiscountAmount(BigDecimal.ZERO);
        invoice.setFinalAmount(amount);
        invoice.setCurrency(currency != null ? currency : "EGP");
        invoice.setIsAnnual(isAnnualSubscription(subscription));
        invoice.setBillingType("RENEWAL");
        invoice.setDescription(String.format("%s Plan Renewal - %s", 
                subscription.getPlan().getName(), 
                invoice.getIsAnnual() ? "Annual" : "Monthly"));
        invoice.setDueDate(subscription.getEndDate());
        invoice.setStatus(InvoiceStatus.PENDING);

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Created renewal invoice {} for subscription {} - Amount: {}", 
                saved.getUuid(), subscription.getId(), amount);
        return Optional.of(saved);
    }

    @Override
    @Transactional
    public Invoice processPayment(UUID invoiceUuid, String gateway) {
        Invoice invoice = invoiceRepository.findByUuid(invoiceUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceUuid));

        if (invoice.getStatus() != InvoiceStatus.PENDING) {
            throw new IllegalStateException("Invoice is not in PENDING status");
        }

        PaymentCreationResult result = paymentService.createPayment(
                invoice.getUser().getId(),
                invoice.getFinalAmount().doubleValue(),
                gateway
        );

        invoice.setGateway(gateway);
        invoice.setPaymentUrl(result.getPaymentUrl());
        invoice.setPaymentTransactionId(result.getPaymentKey());

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Generated payment link for invoice {} via {} - URL: {}", 
                invoiceUuid, gateway, result.getPaymentUrl());
        return saved;
    }

    @Override
    @Transactional
    public void markPaid(String paymentTransactionId) {
        Optional<Invoice> invoiceOpt = invoiceRepository.findByPaymentTransactionId(paymentTransactionId);
        if (invoiceOpt.isEmpty()) {
            log.warn("No invoice found for transaction ID: {}", paymentTransactionId);
            return;
        }

        Invoice invoice = invoiceOpt.get();
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            log.info("Invoice {} already marked as PAID", invoice.getUuid());
            return;
        }

        invoice.markPaid(paymentTransactionId);
        invoiceRepository.save(invoice);
        log.info("Marked invoice {} as PAID", invoice.getUuid());
    }

    @Override
    @Transactional
    public void expirePendingInvoices(Long subscriptionId) {
        int updated = invoiceRepository.expirePendingInvoicesBySubscriptionId(subscriptionId, InvoiceStatus.EXPIRED);
        if (updated > 0) {
            log.info("Expired {} pending invoices for subscription {}", updated, subscriptionId);
        }
    }

    @Override
    @Transactional
    public void cancelPendingInvoices(Long subscriptionId) {
        int updated = invoiceRepository.cancelPendingInvoicesBySubscriptionId(subscriptionId);
        if (updated > 0) {
            log.info("Cancelled {} pending invoices for subscription {}", updated, subscriptionId);
        }
    }

    @Override
    @Transactional
    public void sendReminderEmail(Invoice invoice) {
        try {
            String checkoutUrl = frontendUrl + "/checkout?invoice=" + invoice.getUuid();
            String subject = "Reminder: Your Civify Invoice is Due";
            String bodyContent = String.format("""
                    <p>Hello <strong>%s</strong>,</p>
                    
                    <p>This is a friendly reminder that your invoice for <strong>%s</strong> is pending payment.</p>
                    
                    <div style="background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; border-radius: 4px;">
                        <p style="margin: 0;"><strong>Amount Due:</strong> %.2f %s</p>
                        <p style="margin: 5px 0 0 0;"><strong>Due Date:</strong> %s</p>
                    </div>
                    
                    <p>Please complete your payment to continue enjoying Civify services:</p>
                    
                    <p style="text-align: center; margin: 25px 0;">
                        <a href="%s" style="display: inline-block; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; font-weight: bold;">Pay Now</a>
                    </p>
                    
                    <p>Thank you for being a valued customer!</p>
                    
                    <p>Best regards,<br>The Civify Team</p>
                    """,
                    invoice.getUserName(),
                    invoice.getDescription(),
                    invoice.getFinalAmount().doubleValue(),
                    invoice.getCurrency(),
                    invoice.getDueDate() != null ? invoice.getDueDate().toLocalDate().toString() : "Soon",
                    checkoutUrl
            );

            emailService.sendHtmlEmailToUser(invoice.getUser().getId(), invoice.getUserEmail(), subject, bodyContent);
            
            // Mark reminder as sent to avoid spamming
            invoice.setReminderSent(true);
            invoiceRepository.save(invoice);
            
            log.info("Sent reminder email for invoice {} to {}", invoice.getUuid(), invoice.getUserEmail());
        } catch (Exception e) {
            log.warn("Failed to send reminder email for invoice {}: {}", invoice.getUuid(), e.getMessage());
        }
    }

    @Override
    public Optional<Invoice> getByUuid(UUID uuid) {
        return invoiceRepository.findByUuid(uuid);
    }

    @Override
    public List<Invoice> getUserInvoices(Long userId) {
        return invoiceRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public Optional<Invoice> findPendingInvoice(Long subscriptionId) {
        return invoiceRepository.findBySubscriptionIdAndStatus(subscriptionId, InvoiceStatus.PENDING);
    }

    @Override
    @Transactional
    public boolean handleSuccessfulPayment(String paymentTransactionId) {
        Optional<Invoice> invoiceOpt = invoiceRepository.findByPaymentTransactionId(paymentTransactionId);
        if (invoiceOpt.isEmpty()) {
            log.debug("No invoice found for transaction ID: {}", paymentTransactionId);
            return false;
        }

        Invoice invoice = invoiceOpt.get();
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            log.info("Invoice {} already marked as PAID", invoice.getUuid());
            return true;
        }

        // Mark invoice as paid
        invoice.markPaid(paymentTransactionId);
        invoiceRepository.save(invoice);
        log.info("Marked invoice {} as PAID", invoice.getUuid());

        // If this is a RENEWAL invoice, extend the subscription
        if ("RENEWAL".equals(invoice.getBillingType()) && invoice.getSubscription() != null) {
            UserSubscription subscription = invoice.getSubscription();
            
            // Calculate new end date based on current end date
            LocalDateTime currentEnd = subscription.getEndDate();
            LocalDateTime newEnd;
            
            if (Boolean.TRUE.equals(invoice.getIsAnnual())) {
                newEnd = currentEnd.plusYears(1);
            } else {
                newEnd = currentEnd.plusMonths(1);
            }
            
            subscription.setEndDate(newEnd);
            subscription.setAutoRenew(true); // Re-enable auto-renew after successful payment
            // Note: We don't save here as this might cause issues with lazy loading
            // The subscription change will be committed with the transaction
            
            log.info("Extended subscription {} to {} (renewal invoice {})", 
                    subscription.getId(), newEnd, invoice.getUuid());
        }

        return true;
    }

    private boolean isAnnualSubscription(UserSubscription sub) {
        if (sub.getStartDate() == null || sub.getEndDate() == null) {
            return false;
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(sub.getStartDate(), sub.getEndDate());
        return days > 180;
    }
}
