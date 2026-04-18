package com.app.civify.service.payment;

import com.app.civify.entity.enums.TransactionType;

import com.app.civify.dto.payment.PaymentCallbackResult;
import com.app.civify.dto.payment.PaymentCreationResult;
import com.app.civify.dto.payment.PaymentMethods;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class NullPaymentGateway implements PaymentGateway {

    @Override
    public PaymentCreationResult createPayment(Long userId, double amount, PaymentMethods method, TransactionType type, Map<String, Object> metadata) {
        log.warn("Attempted to create payment with NULL gateway");
        throw new IllegalStateException("Payment gateway is not available");
    }

    @Override
    public boolean verifyPayment(String transactionId) {
        log.warn("Attempted to verify payment with NULL gateway");
        return false;
    }

    @Override
    public PaymentCallbackResult handleCallback(String rawPayload, Map<String, String> headers) {
        log.warn("Attempted to handle callback with NULL gateway");
        throw new IllegalStateException("Payment gateway is not available");
    }

    @Override
    public String getPaymentUrl(String paymentKey) {
        return "";
    }

    @Override
    public PaymentCallbackResult handleRedirect(Map<String, String> params) {
        return PaymentGateway.super.handleRedirect(params);
    }

    @Override
    public boolean isRedirectSuccess(Map<String, String> params) {
        return PaymentGateway.super.isRedirectSuccess(params);
    }

    @Override
    public String getGatewayName() {
        return "NULL";
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public Long getPaymentMethod(PaymentMethods paymentMethods) {
        return -1L;
    }
}