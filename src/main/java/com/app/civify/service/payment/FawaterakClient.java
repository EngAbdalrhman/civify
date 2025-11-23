package com.app.civify.service.payment;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "fawaterakClient", url = "${payment.fawaterak.base-url:https://app.fawaterk.com/api/v2}")
public interface FawaterakClient {

    @PostMapping(value = "/invoiceInitPay", consumes = "application/json")
    String createInvoice(@RequestHeader("Authorization") String authToken, @RequestBody String payload);

    @GetMapping(value = "/getInvoiceStatus/{invoiceId}")
    String getInvoiceStatus(@RequestHeader("Authorization") String authToken, @PathVariable Long invoiceId);
}
