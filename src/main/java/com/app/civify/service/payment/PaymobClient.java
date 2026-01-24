package com.app.civify.service.payment;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "paymobClient", url = "${payment.paymob.base-url:https://accept.paymob.com}")
public interface PaymobClient {

    @PostMapping(value = "/v1/intention/", consumes = "application/json")
    String createIntention(@RequestHeader("Authorization") String authToken, @RequestBody String payload);

    @PostMapping(value = "/api/auth/tokens", consumes = "application/json")
    String authenticate(@RequestBody String payload);

    @PostMapping(value = "/api/ecommerce/orders", consumes = "application/json")
    String createOrder(@RequestHeader("Authorization") String authToken, @RequestBody String payload);

    @GetMapping(value = "/api/ecommerce/orders/{id}")
    String getOrder(@RequestHeader("Authorization") String authToken, @PathVariable String id);

    @PostMapping(value = "/api/acceptance/payment_keys", consumes = "application/json")
    String createPaymentKey(@RequestHeader("Authorization") String authToken, @RequestBody String payload);

    @PostMapping(value = "/api/acceptance/payments/pay", consumes = "application/json")
    String processPayment(@RequestBody String payload);

    // Legacy methods for backward compatibility
    @PostMapping(value = "/payment/transactions", consumes = "application/json")
    String createTransaction(@RequestBody String payload);

    @PostMapping(value = "/payment/verify", consumes = "application/json")
    String verifyTransaction(@RequestBody String payload);
}
