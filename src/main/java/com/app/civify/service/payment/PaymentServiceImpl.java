package com.app.civify.service.payment;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymobClient paymobClient;

    @Override
    public String createPayment(Long userId, double amount) {
        String payload = "{\"userId\":" + userId + ",\"amount\":" + amount + "}";
        return paymobClient.createTransaction(payload);
    }

    @Override
    public boolean verifyPayment(String paymentId) {
        String payload = "{\"paymentId\":\"" + paymentId + "\"}";
        String resp = paymobClient.verifyTransaction(payload);
        return resp != null && resp.contains("success");
    }
}
