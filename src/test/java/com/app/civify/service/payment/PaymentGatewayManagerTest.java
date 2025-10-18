package com.app.civify.service.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayManagerTest {

    @Mock
    private PaymobPaymentGateway paymobGateway;

    @Mock
    private PaymentGateway stripeGateway;

    private PaymentGatewayManager gatewayManager;

    @BeforeEach
    void setUp() {
        gatewayManager = new PaymentGatewayManager();

        // Mock gateway behaviors
        when(paymobGateway.getGatewayName()).thenReturn("paymob");
        when(paymobGateway.isEnabled()).thenReturn(true);

        when(stripeGateway.getGatewayName()).thenReturn("stripe");
        when(stripeGateway.isEnabled()).thenReturn(false); // Disabled for testing
    }

    @Test
    void testRegisterGateway_EnabledGateway() {
        gatewayManager.registerGateway(paymobGateway);

        assertTrue(gatewayManager.isGatewayAvailable("paymob"));
        assertEquals(paymobGateway, gatewayManager.getGateway("paymob"));
    }

    @Test
    void testRegisterGateway_DisabledGateway() {
        gatewayManager.registerGateway(stripeGateway);

        assertFalse(gatewayManager.isGatewayAvailable("stripe"));
        assertThrows(IllegalArgumentException.class, () -> gatewayManager.getGateway("stripe"));
    }

    @Test
    void testGetDefaultGateway() {
        gatewayManager.registerGateway(paymobGateway);

        PaymentGateway defaultGateway = gatewayManager.getDefaultGateway();
        assertEquals(paymobGateway, defaultGateway);
    }

    @Test
    void testGetGateway_NotFound() {
        assertThrows(IllegalArgumentException.class, () -> gatewayManager.getGateway("nonexistent"));
    }

    @Test
    void testCreatePayment_DefaultGateway() {
        gatewayManager.registerGateway(paymobGateway);

        Long userId = 1L;
        double amount = 100.0;
        Map<String, Object> metadata = Map.of("planId", "premium");

        when(paymobGateway.createPayment(userId, amount, metadata)).thenReturn("payment_key_123");

        String result = gatewayManager.createPayment(userId, amount, metadata);

        assertEquals("payment_key_123", result);
        verify(paymobGateway).createPayment(userId, amount, metadata);
    }

    @Test
    void testCreatePayment_SpecificGateway() {
        gatewayManager.registerGateway(paymobGateway);

        Long userId = 1L;
        double amount = 100.0;
        Map<String, Object> metadata = Map.of("planId", "premium");

        when(paymobGateway.createPayment(userId, amount, metadata)).thenReturn("payment_key_456");

        String result = gatewayManager.createPayment("paymob", userId, amount, metadata);

        assertEquals("payment_key_456", result);
        verify(paymobGateway).createPayment(userId, amount, metadata);
    }

    @Test
    void testVerifyPayment_DefaultGateway() {
        gatewayManager.registerGateway(paymobGateway);

        String transactionId = "txn_123";
        when(paymobGateway.verifyPayment(transactionId)).thenReturn(true);

        boolean result = gatewayManager.verifyPayment(transactionId);

        assertTrue(result);
        verify(paymobGateway).verifyPayment(transactionId);
    }

    @Test
    void testHandleCallback_DefaultGateway() {
        gatewayManager.registerGateway(paymobGateway);

        Map<String, Object> callbackData = Map.of("order_id", "order_123", "success", "true");
        when(paymobGateway.handleCallback(callbackData)).thenReturn(true);

        boolean result = gatewayManager.handleCallback(callbackData);

        assertTrue(result);
        verify(paymobGateway).handleCallback(callbackData);
    }

    @Test
    void testGetAllGateways() {
        gatewayManager.registerGateway(paymobGateway);
        gatewayManager.registerGateway(stripeGateway); // Disabled, won't be added

        var gateways = gatewayManager.getAllGateways();

        assertEquals(1, gateways.size());
        assertTrue(gateways.contains(paymobGateway));
    }
}
