package com.app.civify.service.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Configuration class to register payment gateways
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class PaymentGatewayConfig {

    private final PaymentGatewayManager gatewayManager;
    private final PaymobPaymentGateway paymobGateway;

    @PostConstruct
    public void registerGateways() {
        log.info("Registering payment gateways...");

        // Register Paymob gateway
        gatewayManager.registerGateway(paymobGateway);

        // Register other gateways here as they are implemented
        // gatewayManager.registerGateway(stripeGateway);
        // gatewayManager.registerGateway(paypalGateway);
        log.info("Payment gateway registration completed");
    }
}
