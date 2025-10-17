package com.app.civify.service.payment;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "paymobClient", url = "${payment.paymob.base-url:https://api.paymob.com}")
public interface PaymobClient {

    @PostMapping(value = "/payment/transactions", consumes = "application/json")
    String createTransaction(@RequestBody String payload);

    @PostMapping(value = "/payment/verify", consumes = "application/json")
    String verifyTransaction(@RequestBody String payload);
}
