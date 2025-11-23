package com.app.civify.service.payment;

import com.app.civify.dto.payment.PaymentCallbackResult;
import com.app.civify.dto.payment.PaymentCreationResult;
import com.app.civify.entity.PaymentTransaction;
import com.app.civify.entity.enums.PaymentStatus;
import com.app.civify.repository.PaymentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class FawaterakPaymentGateway implements PaymentGateway {

    private final FawaterakClient fawaterakClient;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;

    @Value("${payment.fawaterak.api-key:}")
    private String apiKey;

    @Value("${payment.fawaterak.provider-key:}")
    private String providerKey; // Used for some integrations, but API key is main auth

    @Override
    public String getGatewayName() {
        return "fawaterak";
    }

    @Override
    public boolean isEnabled() {
        return apiKey != null && !apiKey.isEmpty();
    }

    @Override
    @Transactional
    public PaymentCreationResult createPayment(Long userId, double amount, Map<String, Object> metadata) {
        if (!isEnabled()) {
            throw new RuntimeException("Fawaterak gateway is not configured properly.");
        }

        try {
            log.info("Creating Fawaterak payment for user {} with amount {} EGP", userId, amount);

            // Prepare payload
            Map<String, Object> cartItem = new HashMap<>();
            cartItem.put("name", "Subscription Plan");
            cartItem.put("price", amount);
            cartItem.put("quantity", "1");

            List<Map<String, Object>> cartItems = new ArrayList<>();
            cartItems.add(cartItem);

            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("payment_method_id", 2); // 2 for credit card, 3 for Fawry
            payloadMap.put("cartTotal", amount);
            payloadMap.put("currency", "EGP");
            payloadMap.put("customer", buildCustomerData(metadata));
            payloadMap.put("redirectionUrls", buildRedirectionUrls());
            payloadMap.put("cartItems", cartItems);

            String payload = objectMapper.writeValueAsString(payloadMap);
            String response = fawaterakClient.createInvoice("Bearer " + apiKey, payload);
            log.info("Fawaterak response: {}", response);

            JsonNode root = objectMapper.readTree(response);
            
            // Check success
            if (root.has("status") && "success".equalsIgnoreCase(root.get("status").asText())) {
                JsonNode data = root.get("data");
                Long invoiceId = data.get("invoice_id").asLong();
                String paymentKey = data.get("invoice_key").asText();
                
                String paymentUrl = "";
                if (data.has("payment_data") && data.get("payment_data").has("redirectTo")) {
                    paymentUrl = data.get("payment_data").get("redirectTo").asText();
                } else if (data.has("payment_url")) {
                     paymentUrl = data.get("payment_url").asText();
                } else {
                    log.warn("Could not find payment URL in Fawaterak response");
                }

                // Save transaction
                PaymentTransaction transaction = new PaymentTransaction();
                transaction.setUserId(userId);
                transaction.setAmount(BigDecimal.valueOf(amount));
                transaction.setOrderId(String.valueOf(invoiceId));
                transaction.setPaymentKey(paymentKey);
                transaction.setStatus(PaymentStatus.PENDING);
                transaction.setGateway(getGatewayName());
                transaction.setCurrency("EGP");
                
                if (metadata == null) metadata = new HashMap<>();
                metadata.put("paymentUrl", paymentUrl);
                transaction.setMetadata(objectMapper.writeValueAsString(metadata));

                paymentRepository.save(transaction);

                log.info("Fawaterak payment created successfully. Invoice ID: {}", invoiceId);
                
                return PaymentCreationResult.builder()
                        .paymentKey(paymentKey)
                        .paymentUrl(paymentUrl)
                        .orderId(String.valueOf(invoiceId))
                        .build();
            } else {
                throw new RuntimeException("Fawaterak API returned error: " + response);
            }

        } catch (Exception e) {
            log.error("Error creating Fawaterak payment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create Fawaterak payment", e);
        }
    }

    @Override
    @Transactional
    public boolean verifyPayment(String paymentKey) {
        try {
            log.info("Verifying Fawaterak payment with key: {}", paymentKey);
            
            PaymentTransaction transaction = paymentRepository.findByPaymentKey(paymentKey)
                    .orElseThrow(() -> new RuntimeException("Transaction not found"));

            Long invoiceId = Long.parseLong(transaction.getOrderId());
            String response = fawaterakClient.getInvoiceStatus("Bearer " + apiKey, invoiceId);
            
            JsonNode root = objectMapper.readTree(response);
            if (root.has("status") && "success".equalsIgnoreCase(root.get("status").asText())) {
                JsonNode data = root.get("data");
                String invoiceStatus = data.get("payment_status").asText(); // paid, pending, expired
                
                if ("paid".equalsIgnoreCase(invoiceStatus)) {
                    transaction.setStatus(PaymentStatus.COMPLETED);
                    transaction.setCompletedAt(LocalDateTime.now());
                    paymentRepository.save(transaction);
                    return true;
                } else if ("expired".equalsIgnoreCase(invoiceStatus)) {
                    transaction.setStatus(PaymentStatus.FAILED);
                    paymentRepository.save(transaction);
                }
            }
            
            return false;

        } catch (Exception e) {
            log.error("Error verifying Fawaterak payment: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public PaymentCallbackResult handleCallback(Map<String, Object> callbackData) {
        log.info("Handling Fawaterak callback: {}", callbackData);
        
        PaymentCallbackResult result = new PaymentCallbackResult();
        result.setGatewayName(getGatewayName());
        
        try {
            // Fawaterak webhook structure
            // invoice_id, invoice_status, etc.
            
            Object invoiceIdObj = callbackData.get("invoice_id");
            if (invoiceIdObj == null) {
                result.setSuccess(false);
                result.setMessage("Missing invoice_id");
                return result;
            }
            
            String invoiceId = String.valueOf(invoiceIdObj);
            String status = (String) callbackData.get("invoice_status");
            
            PaymentTransaction transaction = paymentRepository.findByOrderId(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found for invoice: " + invoiceId));
            
            result.setTransactionId(transaction.getPaymentKey());
            result.setOrderId(invoiceId);
            
            if ("paid".equalsIgnoreCase(status)) {
                transaction.setStatus(PaymentStatus.COMPLETED);
                transaction.setCompletedAt(LocalDateTime.now());
                result.setSuccess(true);
                result.setMessage("Payment completed");
            } else if ("failed".equalsIgnoreCase(status) || "expired".equalsIgnoreCase(status)) {
                transaction.setStatus(PaymentStatus.FAILED);
                result.setSuccess(false);
                result.setMessage("Payment failed or expired");
            } else {
                result.setSuccess(false);
                result.setMessage("Unknown status: " + status);
            }
            
            paymentRepository.save(transaction);
            return result;

        } catch (Exception e) {
            log.error("Error handling Fawaterak callback: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("Error: " + e.getMessage());
            return result;
        }
    }

    @Override
    public String getPaymentUrl(String paymentKey) {
        // For Fawaterak, the URL is generated at creation time.
        // We stored it in metadata or we can reconstruct it if possible.
        // But typically we return the paymentKey and frontend redirects.
        // However, Fawaterak returns a full URL.
        // Let's try to retrieve it from transaction metadata.
        
        return paymentRepository.findByPaymentKey(paymentKey)
                .map(t -> {
                    try {
                        JsonNode meta = objectMapper.readTree(t.getMetadata());
                        if (meta.has("paymentUrl")) {
                            return meta.get("paymentUrl").asText();
                        }
                    } catch (Exception e) {
                        log.warn("Could not parse metadata for payment URL");
                    }
                    return "";
                })
                .orElse("");
    }

    private Map<String, String> buildCustomerData(Map<String, Object> metadata) {
        Map<String, String> customer = new HashMap<>();
        customer.put("first_name", "Customer");
        customer.put("last_name", "User");
        customer.put("email", "customer@example.com");
        customer.put("phone", "01000000000"); // Required format
        
        if (metadata != null) {
            if (metadata.containsKey("firstName")) customer.put("first_name", (String) metadata.get("firstName"));
            if (metadata.containsKey("lastName")) customer.put("last_name", (String) metadata.get("lastName"));
            if (metadata.containsKey("email")) customer.put("email", (String) metadata.get("email"));
            if (metadata.containsKey("phone")) customer.put("phone", (String) metadata.get("phone"));
        }
        return customer;
    }

    private Map<String, String> buildRedirectionUrls() {
        Map<String, String> urls = new HashMap<>();
        // TODO: Make these configurable via application.properties
        String baseUrl = "http://localhost:9002"; 
        urls.put("successUrl", baseUrl + "/dashboard/payment/success");
        urls.put("failUrl", baseUrl + "/dashboard/payment/fail");
        urls.put("pendingUrl", baseUrl + "/dashboard/payment/pending");
        return urls;
    }
}
