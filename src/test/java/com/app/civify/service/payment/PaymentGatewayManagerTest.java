// package com.app.civify.service.payment;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.test.util.ReflectionTestUtils;

// import java.util.List;
// import java.util.Map;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class)
// class PaymentGatewayManagerTest {

//     @Mock
//     private PaymentGateway paymobGateway;

//     @Mock
//     private PaymentGateway stripeGateway;

//     private PaymentGatewayManager gatewayManager;

//     @BeforeEach
//     void setUp() {
//         gatewayManager = new PaymentGatewayManager();
//         ReflectionTestUtils.setField(gatewayManager, "defaultGateway", "paymob");

//         // Mock gateway behaviors - only for tests that need them
//     }

//     @Test
//     void registerGateway_EnabledGateway_Success() {
//         // Given
//         when(paymobGateway.getGatewayName()).thenReturn("paymob");
//         when(paymobGateway.isEnabled()).thenReturn(true);

//         // When
//         gatewayManager.registerGateway(paymobGateway);

//         // Then
//         assertTrue(gatewayManager.isGatewayAvailable("paymob"));
//         assertEquals(paymobGateway, gatewayManager.getGateway("paymob"));
//     }

//     @Test
//     void registerGateway_DisabledGateway_NotRegistered() {
//         // Given
//         when(stripeGateway.getGatewayName()).thenReturn("stripe");
//         when(stripeGateway.isEnabled()).thenReturn(false);

//         // When
//         gatewayManager.registerGateway(stripeGateway);

//         // Then
//         assertFalse(gatewayManager.isGatewayAvailable("stripe"));
//         assertThrows(IllegalArgumentException.class, () -> gatewayManager.getGateway("stripe"));
//     }

//     @Test
//     void registerGateway_MultipleGateways_BothRegistered() {
//         // Given
//         when(paymobGateway.getGatewayName()).thenReturn("paymob");
//         when(paymobGateway.isEnabled()).thenReturn(true);
//         when(stripeGateway.getGatewayName()).thenReturn("stripe");
//         when(stripeGateway.isEnabled()).thenReturn(true);

//         // When
//         gatewayManager.registerGateway(paymobGateway);
//         gatewayManager.registerGateway(stripeGateway);

//         // Then
//         assertTrue(gatewayManager.isGatewayAvailable("paymob"));
//         assertTrue(gatewayManager.isGatewayAvailable("stripe"));
//     }

//     @Test
//     void getGateway_ExistingGateway_ReturnsGateway() {
//         // Given
//         when(paymobGateway.getGatewayName()).thenReturn("paymob");
//         when(paymobGateway.isEnabled()).thenReturn(true);
//         gatewayManager.registerGateway(paymobGateway);

//         // When
//         PaymentGateway gateway = gatewayManager.getGateway("paymob");

//         // Then
//         assertNotNull(gateway);
//         assertEquals(paymobGateway, gateway);
//     }

//     @Test
//     void getGateway_NonExistingGateway_ThrowsException() {
//         // When & Then
//         IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
//                 () -> gatewayManager.getGateway("nonexistent"));
//         assertTrue(exception.getMessage().contains("Payment gateway not found"));
//     }

//     @Test
//     void getDefaultGateway_Success() {
//         // Given
//         when(paymobGateway.getGatewayName()).thenReturn("paymob");
//         when(paymobGateway.isEnabled()).thenReturn(true);
//         gatewayManager.registerGateway(paymobGateway);

//         // When
//         PaymentGateway defaultGateway = gatewayManager.getDefaultGateway();

//         // Then
//         assertNotNull(defaultGateway);
//         assertEquals(paymobGateway, defaultGateway);
//     }

//     @Test
//     void getAllGateways_ReturnsOnlyEnabledGateways() {
//         // Given
//         when(paymobGateway.getGatewayName()).thenReturn("paymob");
//         when(paymobGateway.isEnabled()).thenReturn(true);
//         when(stripeGateway.getGatewayName()).thenReturn("stripe");
//         when(stripeGateway.isEnabled()).thenReturn(true);
//         gatewayManager.registerGateway(paymobGateway);
//         gatewayManager.registerGateway(stripeGateway);

//         // When
//         List<PaymentGateway> gateways = gatewayManager.getAllGateways();

//         // Then
//         assertEquals(2, gateways.size());
//         assertTrue(gateways.contains(paymobGateway));
//         assertTrue(gateways.contains(stripeGateway));
//     }

//     @Test
//     void getAllGateways_EmptyList() {
//         // When
//         List<PaymentGateway> gateways = gatewayManager.getAllGateways();

//         // Then
//         assertTrue(gateways.isEmpty());
//     }

//     @Test
//     void isGatewayAvailable_RegisteredAndEnabled_ReturnsTrue() {
//         // Given
//         when(paymobGateway.getGatewayName()).thenReturn("paymob");
//         when(paymobGateway.isEnabled()).thenReturn(true);
//         gatewayManager.registerGateway(paymobGateway);

//         // When
//         boolean available = gatewayManager.isGatewayAvailable("paymob");

//         // Then
//         assertTrue(available);
//     }

//     @Test
//     void isGatewayAvailable_NotRegistered_ReturnsFalse() {
//         // When
//         boolean available = gatewayManager.isGatewayAvailable("nonexistent");

//         // Then
//         assertFalse(available);
//     }

//     @Test
//     void createPayment_WithDefaultGateway_Success() {
//         // Given
//         when(paymobGateway.getGatewayName()).thenReturn("paymob");
//         when(paymobGateway.isEnabled()).thenReturn(true);
//         gatewayManager.registerGateway(paymobGateway);
//         Long userId = 1L;
//         double amount = 100.0;
//         Map<String, Object> metadata = Map.of("planId", "premium");

//         when(paymobGateway.createPayment(userId, amount, metadata)).thenReturn("payment_key_123");

//         // When
//         String result = gatewayManager.createPayment(userId, amount, metadata);

//         // Then
//         assertEquals("payment_key_123", result);
//         verify(paymobGateway).createPayment(userId, amount, metadata);
//     }

//     @Test
//     void createPayment_WithSpecificGateway_Success() {
//         // Given
//         when(paymobGateway.getGatewayName()).thenReturn("paymob");
//         when(paymobGateway.isEnabled()).thenReturn(true);
//         gatewayManager.registerGateway(paymobGateway);
//         Long userId = 1L;
//         double amount = 100.0;
//         Map<String, Object> metadata = Map.of("planId", "premium");

//         when(paymobGateway.createPayment(userId, amount, metadata)).thenReturn("payment_key_456");

//         // When
//         String result = gatewayManager.createPayment("paymob", userId, amount, metadata);

//         // Then
//         assertEquals("payment_key_456", result);
//         verify(paymobGateway).createPayment(userId, amount, metadata);
//     }

//     @Test
//     void createPayment_WithNonExistentGateway_ThrowsException() {
//         // Given
//         Long userId = 1L;
//         double amount = 100.0;
//         Map<String, Object> metadata = Map.of("planId", "premium");

//         // When & Then
//         assertThrows(IllegalArgumentException.class,
//                 () -> gatewayManager.createPayment("nonexistent", userId, amount, metadata));
//     }

//     @Test
//     void verifyPayment_WithDefaultGateway_Success() {
//         // Given
//         when(paymobGateway.getGatewayName()).thenReturn("paymob");
//         when(paymobGateway.isEnabled()).thenReturn(true);
//         gatewayManager.registerGateway(paymobGateway);
//         String transactionId = "txn_123";
//         when(paymobGateway.verifyPayment(transactionId)).thenReturn(true);

//         // When
//         boolean result = gatewayManager.verifyPayment(transactionId);

//         // Then
//         assertTrue(result);
//         verify(paymobGateway).verifyPayment(transactionId);
//     }

//     @Test
//     void verifyPayment_Failed_ReturnsFalse() {
//         // Given
//         when(paymobGateway.getGatewayName()).thenReturn("paymob");
//         when(paymobGateway.isEnabled()).thenReturn(true);
//         gatewayManager.registerGateway(paymobGateway);
//         String transactionId = "txn_invalid";
//         when(paymobGateway.verifyPayment(transactionId)).thenReturn(false);

//         // When
//         boolean result = gatewayManager.verifyPayment(transactionId);

//         // Then
//         assertFalse(result);
//         verify(paymobGateway).verifyPayment(transactionId);
//     }

//     @Test
//     void handleCallback_WithDefaultGateway_Success() {
//         // Given
//         when(paymobGateway.getGatewayName()).thenReturn("paymob");
//         when(paymobGateway.isEnabled()).thenReturn(true);
//         gatewayManager.registerGateway(paymobGateway);
//         Map<String, Object> callbackData = Map.of(
//                 "order_id", "order_123",
//                 "success", "true",
//                 "amount", 100.0
//         );
//         when(paymobGateway.handleCallback(callbackData)).thenReturn(true);

//         // When
//         boolean result = gatewayManager.handleCallback(callbackData);

//         // Then
//         assertTrue(result);
//         verify(paymobGateway).handleCallback(callbackData);
//     }

//     @Test
//     void handleCallback_Failed_ReturnsFalse() {
//         // Given
//         when(paymobGateway.getGatewayName()).thenReturn("paymob");
//         when(paymobGateway.isEnabled()).thenReturn(true);
//         gatewayManager.registerGateway(paymobGateway);
//         Map<String, Object> callbackData = Map.of("order_id", "invalid");
//         when(paymobGateway.handleCallback(callbackData)).thenReturn(false);

//         // When
//         boolean result = gatewayManager.handleCallback(callbackData);

//         // Then
//         assertFalse(result);
//         verify(paymobGateway).handleCallback(callbackData);
//     }

//     @Test
//     void registerGateway_ReplacesExistingGateway() {
//         // Given
//         PaymentGateway paymobV1 = mock(PaymentGateway.class);
//         PaymentGateway paymobV2 = mock(PaymentGateway.class);

//         when(paymobV1.getGatewayName()).thenReturn("paymob");
//         when(paymobV1.isEnabled()).thenReturn(true);
//         when(paymobV2.getGatewayName()).thenReturn("paymob");
//         when(paymobV2.isEnabled()).thenReturn(true);

//         // When
//         gatewayManager.registerGateway(paymobV1);
//         gatewayManager.registerGateway(paymobV2); // Replace

//         // Then
//         PaymentGateway gateway = gatewayManager.getGateway("paymob");
//         assertEquals(paymobV2, gateway);
//     }

//     @Test
//     void getAllGateways_ReturnsImmutableCopy() {
//         // Given
//         when(paymobGateway.getGatewayName()).thenReturn("paymob");
//         when(paymobGateway.isEnabled()).thenReturn(true);
//         gatewayManager.registerGateway(paymobGateway);

//         // When
//         List<PaymentGateway> gateways = gatewayManager.getAllGateways();

//         // Then - Trying to modify should throw exception
//         assertThrows(UnsupportedOperationException.class, () -> gateways.add(stripeGateway));
//     }
// }
