package com.app.civify.service.payment;

import java.util.Map;

public interface PaymentService {

    String createPayment(Long userId, double amount);

    boolean verifyPayment(String paymentId);

    void handlePaymentCallback(String orderId, Map<String, Object> callbackData);
}
