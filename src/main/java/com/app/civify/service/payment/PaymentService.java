package com.app.civify.service.payment;

public interface PaymentService {
    String createPayment(Long userId, double amount);
    boolean verifyPayment(String paymentId);
}
