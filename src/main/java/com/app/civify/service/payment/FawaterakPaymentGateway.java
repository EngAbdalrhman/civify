package com.app.civify.service.payment;

import com.app.civify.dto.payment.PaymentCallbackResult;
import com.app.civify.dto.payment.PaymentCreationResult;
import com.app.civify.dto.payment.PaymentMethods;
import com.app.civify.entity.enums.BillingAuditType;
import com.app.civify.entity.enums.PaymentStatus;
import com.app.civify.entity.billing.BillingTransaction;
import com.app.civify.repository.billing.BillingTransactionRepository;
import com.app.civify.service.logs.BillingAuditService;
import com.app.civify.service.notification.NotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
@Slf4j
@RequiredArgsConstructor
public class FawaterakPaymentGateway implements PaymentGateway {

    private final FawaterakClient fawaterakClient;
    private final BillingTransactionRepository paymentRepository;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final BillingAuditService billingAuditService;

    @Lazy
    @Autowired
    private PaymentFulfillmentService fulfillmentService;

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

    @Value("${payment.gateway.fawaterak.enabled:true}")
    private boolean enabled;

    @Override
    public String getGatewayName() {
        return "fawaterak";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    @Transactional
    public PaymentCreationResult createPayment(Long userId, double amount, PaymentMethods method, com.app.civify.entity.enums.TransactionType type, Map<String, Object> metadata) {
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
            payloadMap.put("payment_method_id", getPaymentMethod(method));
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
                BillingTransaction transaction = new BillingTransaction();
                transaction.setUserId(userId);
                transaction.setAmount(BigDecimal.valueOf(amount));
                transaction.setOrderId(String.valueOf(invoiceId));
                transaction.setPaymentKey(paymentKey);
                transaction.setStatus(PaymentStatus.PENDING);
                transaction.setType(type);
                transaction.setGateway(getGatewayName());
                transaction.setCurrency("EGP");

                // Create mutable copy of metadata (in case immutable Map.of() was passed)
                Map<String, Object> mutableMetadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
                mutableMetadata.put("paymentUrl", paymentUrl);
                transaction.setMetadata(objectMapper.writeValueAsString(mutableMetadata));

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
        log.info("Verifying Fawaterak payment with key/order: {}", paymentKey);

        BillingTransaction transaction = paymentRepository.findByOrderId(paymentKey)
                .orElseGet(() -> paymentRepository.findByPaymentKey(paymentKey)
                .orElseThrow(() -> new RuntimeException("Transaction not found")));

        Long invoiceId = Long.parseLong(transaction.getOrderId());
        String response = fawaterakClient.getInvoiceStatus("Bearer " + apiKey, invoiceId);

        try {
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
        } catch (Exception e) {
             throw new RuntimeException("Error parsing Fawaterak response", e);
        }

        return false;
    }

    @Override
    @Transactional
    public PaymentCallbackResult handleCallback(String rawPayload, Map<String, String> headers) {
        log.info("Processing Fawaterak webhook callback");

        PaymentCallbackResult result = new PaymentCallbackResult();
        result.setGatewayName(getGatewayName());

        try {
            // Step 0: Parse payload (JSON or Form Data)
            JsonNode callbackData = parsePayload(rawPayload);
            log.info("Fawaterak webhook received - Invoice data parsed");
            log.debug("Payload: {}", callbackData.toPrettyString());

            // Step 1: Verify HMAC signature
            if (!verifySignature(callbackData)) {
                log.error("🔒 Fawaterak HMAC verification FAILED - Potential security threat");
                result.setSuccess(false);
                result.setMessage("WEBHOOK_SIGNATURE_INVALID");
                result.setErrorCode("WEBHOOK_SIGNATURE_INVALID");
                return result;
            }
            log.debug("✅ HMAC signature verified");

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
            BillingTransaction transaction = paymentRepository.findByOrderIdWithLock(invoiceId)
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
                    
                    try {
                        notificationService.sendToUser(transaction.getUserId(), "Payment Successful", 
                                "Your payment was successful and your plan is active.", com.app.civify.entity.enums.NotificationType.SYSTEM);
                    } catch (Exception ex) {
                        log.error("Failed to send payment success notification: {}", ex.getMessage());
                    }
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

            // Fulfill the payment if successful (do this BEFORE audit logging)
            if (result.isSuccess()) {
                fulfillmentService.fulfillPayment(invoiceId);
            }

            // Log billing audit (non-critical - catch exceptions to prevent rollback)
            try {
                String paymentMethodValue = callbackData.has("payment_method") 
                        ? callbackData.get("payment_method").asText() : null;
                BillingAuditType auditType = result.isSuccess() 
                        ? BillingAuditType.WEBHOOK_SUCCESS 
                        : BillingAuditType.WEBHOOK_FAILED;
                // Convert callbackData to JSON string (rawPayload may be form-encoded which fails JSON storage)
                String webhookPayloadJson = objectMapper.writeValueAsString(callbackData);
                billingAuditService.logWebhook(
                    getGatewayName(),
                    transaction.getUserId(),
                    null, // invoiceId - we don't have our invoice ID here
                    transaction.getPaymentKey(),
                    invoiceId,
                    status,
                    paymentMethodValue,
                    result.getMessage(),
                    webhookPayloadJson,
                    null,
                    auditType
                );
            } catch (Exception auditEx) {
                log.warn("Failed to log billing audit for Fawaterak callback (non-critical): {}", auditEx.getMessage());
            }

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
     * @param newStatus     New status from webhook
     * @return true if transition is valid
     */
    private boolean isValidStateTransition(PaymentStatus currentStatus, String newStatus) {
        // Allow COMPLETED -> REFUNDED
        if (currentStatus == PaymentStatus.COMPLETED && "refunded".equalsIgnoreCase(newStatus)) {
            return true;
        }

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

    private boolean verifySignature(JsonNode root) {
        try {
            if (!root.has("hashKey")) {
                log.error("❌ Fawaterak hashKey missing in body");
                return false;
            }
            String signature = root.get("hashKey").asText();
            
            String invoiceId = root.path("invoice_id").asText("");
            String invoiceKey = root.path("invoice_key").asText("");
            String paymentMethod = root.path("payment_method").asText("");

            // Normalize keys
            String cleanProviderKey = (providerKey != null) ? providerKey.trim() : "";
            String cleanApiKey = (apiKey != null) ? apiKey.trim() : "";

            // Format 1: Standard (InvoiceId + InvoiceKey + PaymentMethod)
            String queryParam1 = String.format("InvoiceId=%s&InvoiceKey=%s&PaymentMethod=%s",
                    invoiceId, invoiceKey, paymentMethod);
            
            // Format 2: Legacy (InvoiceId + InvoiceKey)
            String queryParam2 = String.format("InvoiceId=%s&InvoiceKey=%s",
                    invoiceId, invoiceKey);

            // Try all combinations
            if (checkHash(signature, queryParam1, cleanProviderKey, "ProviderKey-Standard")) return true;
            if (checkHash(signature, queryParam1, cleanApiKey, "ApiKey-Standard")) return true;
            if (checkHash(signature, queryParam2, cleanProviderKey, "ProviderKey-Legacy")) return true;
            if (checkHash(signature, queryParam2, cleanApiKey, "ApiKey-Legacy")) return true;

            log.error("❌ Signature verification failed.");
            return false;
        } catch (Exception e) {
            log.error("Fawaterak signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean checkHash(String expected, String data, String key, String method) {
        if (key == null || key.isEmpty()) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String calculated = bytesToHex(digest);
            
            if (expected.equalsIgnoreCase(calculated)) {
                log.info("✅ Signature verify SUCCESS using method: {}", method);
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private JsonNode parsePayload(String rawPayload) {
        try {
            // First try valid JSON
            if (rawPayload != null && rawPayload.trim().startsWith("{")) {
                return objectMapper.readTree(rawPayload);
            }
        } catch (Exception ignored) {
            // Ignore JSON parse error, try form data
        }

        // Fallback to form-data parsing
        ObjectNode node = objectMapper.createObjectNode();

        if (rawPayload == null || rawPayload.isEmpty()) {
            return node;
        }

        String[] pairs = rawPayload.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                try {
                    String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                    String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                    node.put(key, value);
                } catch (Exception e) {
                    log.warn("Failed to decode form data pair: {}", pair);
                }
            } else if (idx < 0 && !pair.isEmpty()) {
                // key only
                try {
                    String key = URLDecoder.decode(pair, StandardCharsets.UTF_8);
                    node.put(key, "");
                } catch (Exception e) {
                }
            }
        }
        return node;
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
        
        // Defaults
        String firstName = "Customer";
        String lastName = "User";
        String email = "customer@example.com";
        String phone = "01000000000";

        if (metadata != null) {
            if (metadata.containsKey("firstName") && metadata.get("firstName") != null) 
                firstName = (String) metadata.get("firstName");
                
            if (metadata.containsKey("lastName") && metadata.get("lastName") != null) 
                lastName = (String) metadata.get("lastName");
                
            if (metadata.containsKey("email") && metadata.get("email") != null) 
                email = (String) metadata.get("email");
                
            if (metadata.containsKey("phone") && metadata.get("phone") != null) 
                phone = (String) metadata.get("phone");
        }

        customer.put("first_name", firstName);
        customer.put("last_name", lastName);
        customer.put("email", email);
        customer.put("phone", phone);
        
        return customer;
    }

    private Map<String, String> buildRedirectionUrls() {
        Map<String, String> urls = new HashMap<>();
        // Include gateway parameter to help frontend identify the payment gateway
        urls.put("successUrl", successUrl);
        urls.put("failUrl", failUrl);
        urls.put("pendingUrl", pendingUrl);
        return urls;
    }

    @Override
    public boolean isRedirectSuccess(Map<String, String> params) {
        if (params == null) return false;
        // Fawaterak might send status in 'invoice_status' or 'payment_status' or 'status'
        String status = params.getOrDefault("invoice_status", 
                       params.getOrDefault("payment_status", 
                       params.getOrDefault("status", ""))).toLowerCase();
        
        return "paid".equals(status) || "success".equals(status) || "successful".equals(status) || "completed".equals(status);
    }

    @Override
    public Long getPaymentMethod(PaymentMethods paymentMethod){
        switch (paymentMethod){
            case CARD -> {
                return 2L;
            }
            case WALLET -> {
                return 3L;
            }
        }
        return -1L;
    }
}
