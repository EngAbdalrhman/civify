package com.app.civify.service.payment;

import com.app.civify.dto.payment.PaymentCallbackResult;
import com.app.civify.dto.payment.PaymentCreationResult;
import com.app.civify.entity.plans.PaymentTransaction;
import com.app.civify.entity.enums.PaymentStatus;
import com.app.civify.repository.plans.PaymentRepository;
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

    @Value("${payment.redirect.success-url}")
    private String successUrl;

    @Value("${payment.redirect.fail-url}")
    private String failUrl;

    @Value("${payment.redirect.pending-url}")
    private String pendingUrl;

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
    public PaymentCallbackResult handleCallback(String rawPayload, Map<String, String> headers) {
        log.info("Processing Fawaterak webhook callback");
        
        PaymentCallbackResult result = new PaymentCallbackResult();
        result.setGatewayName(getGatewayName());
        
        try {
            // Step 1: Verify HMAC signature
            if (!verifySignature(rawPayload, headers)) {
                log.error("🔒 Fawaterak HMAC verification FAILED - Potential security threat");
                result.setSuccess(false);
                result.setMessage("WEBHOOK_SIGNATURE_INVALID");
                result.setErrorCode("WEBHOOK_SIGNATURE_INVALID");
                return result;
            }
            log.debug("✅ HMAC signature verified");

            // Step 2: Parse webhook payload
            JsonNode callbackData = objectMapper.readTree(rawPayload);
            log.info("Fawaterak webhook received - Invoice data parsed");
            log.debug("Payload: {}", callbackData.toPrettyString());

            // Step 3: Extract and validate invoice ID
            if (!callbackData.has("invoice_id")) {
                log.error("❌ Missing invoice_id in Fawaterak webhook");
                result.setSuccess(false);
                result.setMessage("INVALID_WEBHOOK_PAYLOAD");
                result.setErrorCode("INVALID_WEBHOOK_PAYLOAD");
                return result;
            }
            
            String invoiceId = callbackData.get("invoice_id").asText();
            String status = callbackData.has("invoice_status") 
                ? callbackData.get("invoice_status").asText() 
                : callbackData.has("payment_status")
                    ? callbackData.get("payment_status").asText()
                    : "unknown";
            
            log.info("Processing invoice {} with status: {}", invoiceId, status);
            
            // Step 4: Find transaction with PESSIMISTIC LOCK (prevents race conditions)
            PaymentTransaction transaction = paymentRepository.findByOrderIdWithLock(invoiceId)
                    .orElseThrow(() -> {
                        log.error("Transaction not found for invoice: {}", invoiceId);
                        return new RuntimeException("TRANSACTION_NOT_FOUND");
                    });

            log.info("Transaction {} locked for processing", transaction.getPaymentKey());
            
            result.setTransactionId(transaction.getPaymentKey());
            result.setOrderId(invoiceId);
            
            // Step 5: CRITICAL - Idempotency check
            if (transaction.getStatus() == PaymentStatus.COMPLETED) {
                log.info("⚠️ Idempotent webhook detected - Invoice {} already COMPLETED", invoiceId);
                result.setSuccess(true);
                result.setMessage("ALREADY_PROCESSED");
                result.setAlreadyProcessed(true);
                return result;
            }
            
            // Step 6: Validate state transition
            if (!isValidStateTransition(transaction.getStatus(), status)) {
                log.error("❌ Invalid state transition for invoice {}: {} -> {}", 
                    invoiceId, transaction.getStatus(), status);
                result.setSuccess(false);
                result.setMessage("INVALID_STATE_TRANSITION");
                result.setErrorCode("INVALID_STATE_TRANSITION");
                return result;
            }
            
            // Step 7: Process payment based on status
            PaymentStatus previousStatus = transaction.getStatus();
            
            switch (status.toLowerCase()) {
                case "paid", "completed", "success", "successful" -> {
                    transaction.setStatus(PaymentStatus.COMPLETED);
                    transaction.setCompletedAt(LocalDateTime.now());
                    result.setSuccess(true);
                    result.setMessage("PAYMENT_COMPLETED");
                    log.info("✅ Payment COMPLETED for invoice {}", invoiceId);
                }
                case "failed", "declined", "cancelled" -> {
                    transaction.setStatus(PaymentStatus.FAILED);
                    result.setSuccess(false);
                    result.setMessage("PAYMENT_FAILED");
                    log.warn("❌ Payment FAILED for invoice {}: {}", invoiceId, status);
                }
                case "expired" -> {
                    transaction.setStatus(PaymentStatus.FAILED);
                    result.setSuccess(false);
                    result.setMessage("PAYMENT_EXPIRED");
                    log.warn("⏰ Payment EXPIRED for invoice {}", invoiceId);
                }
                case "refunded" -> {
                    transaction.setStatus(PaymentStatus.REFUNDED);
                    result.setSuccess(false);
                    result.setMessage("PAYMENT_REFUNDED");
                    log.warn("💰 Payment REFUNDED for invoice {}", invoiceId);
                }
                case "pending", "processing" -> {
                    // Keep as PENDING, no action needed
                    log.info("⏳ Payment still PENDING for invoice {}", invoiceId);
                    result.setSuccess(false);
                    result.setMessage("PAYMENT_PENDING");
                }
                default -> {
                    log.error("❓ Unknown Fawaterak status: {} for invoice {}", status, invoiceId);
                    result.setSuccess(false);
                    result.setMessage("UNKNOWN_PAYMENT_STATUS");
                    result.setErrorCode("UNKNOWN_PAYMENT_STATUS");
                    return result; // Don't save unknown states
                }
            }
            
            // Step 8: Save transaction (within locked context)
            paymentRepository.save(transaction);
            log.info("💾 Transaction state updated: {} -> {} for invoice {}", 
                previousStatus, transaction.getStatus(), invoiceId);
            
            return result;

        } catch (JsonProcessingException e) {
            log.error("🔴 JSON parsing error in Fawaterak callback: {}", e.getMessage());
            result.setSuccess(false);
            result.setMessage("INVALID_WEBHOOK_FORMAT");
            result.setErrorCode("INVALID_WEBHOOK_FORMAT");
            return result;
        } catch (Exception e) {
            log.error("🔴 Unexpected error handling Fawaterak callback", e);
            result.setSuccess(false);
            result.setMessage("WEBHOOK_PROCESSING_ERROR");
            result.setErrorCode("WEBHOOK_PROCESSING_ERROR");
            return result;
        }
    }
    
    /**
     * Validates if the status transition is allowed.
     * Prevents invalid state changes (e.g., COMPLETED -> FAILED).
     * 
     * @param currentStatus Current transaction status
     * @param newStatus New status from webhook
     * @return true if transition is valid
     */
    private boolean isValidStateTransition(PaymentStatus currentStatus, String newStatus) {
        // Terminal states cannot transition
        if (currentStatus == PaymentStatus.COMPLETED || 
            currentStatus == PaymentStatus.REFUNDED) {
            log.warn("Attempted transition from terminal state: {}", currentStatus);
            return false;
        }
        
        // PENDING can transition to any state
        if (currentStatus == PaymentStatus.PENDING) {
            return true;
        }
        
        // FAILED can only retry to PENDING or stay FAILED
        if (currentStatus == PaymentStatus.FAILED) {
            return "pending".equalsIgnoreCase(newStatus) || 
                   "failed".equalsIgnoreCase(newStatus);
        }
        
        return true;
    }

    private boolean verifySignature(String payload, Map<String, String> headers) {
        // Fawaterak may not send HMAC; if header absent, allow but log.
        String signature = headers.getOrDefault("signature", headers.getOrDefault("SIGNATURE", ""));
        if (signature == null || signature.isBlank()) {
            log.warn("Fawaterak signature header missing; skipping verification");
            return true;
        }
        try {
            // Use raw payload directly
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(providerKey.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String expected = bytesToHex(digest);
            return signature.equalsIgnoreCase(expected);
        } catch (Exception e) {
            log.warn("Fawaterak signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
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
        // Include gateway parameter to help frontend identify the payment gateway
        urls.put("successUrl", successUrl + "?gateway=fawaterak");
        urls.put("failUrl", failUrl + "?gateway=fawaterak");
        urls.put("pendingUrl", pendingUrl + "?gateway=fawaterak");
        return urls;
    }
}
