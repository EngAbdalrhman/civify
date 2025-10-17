package com.app.civify.controller.v1;

import com.app.civify.repository.UserRepository;
import com.app.civify.service.payment.PaymentService;
import com.app.civify.service.subscription.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@ApiV1Controller
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;

    @PostMapping("/initiate")
    @PreAuthorize("hasAnyRole('USER', 'LICENSED_USER', 'ADMIN')")
    public ResponseEntity<Map<String, String>> initiatePayment(
            @RequestParam Long planId,
            @RequestParam(required = false) String promoCode,
            @RequestParam(defaultValue = "false") boolean isAnnual,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            String paymentKey = subscriptionService.initiatePayment(userId, planId, isAnnual, promoCode);
            
            return ResponseEntity.ok(Map.of(
                    "paymentKey", paymentKey,
                    "message", "Payment initiated successfully"
            ));
        } catch (Exception e) {
            log.error("Error initiating payment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handlePaymentWebhook(@RequestBody Map<String, Object> payload) {
        try {
            log.info("Received payment webhook: {}", payload);
            
            String orderId = (String) payload.get("order_id");
            String success = (String) payload.get("success");
            
            if ("true".equals(success)) {
                // Find the payment transaction and activate subscription
                String paymentKey = (String) payload.get("payment_key");
                subscriptionService.activateSubscription(paymentKey);
            }
            
            paymentService.handlePaymentCallback(orderId, payload);
            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            log.error("Error processing payment webhook: {}", e.getMessage());
            return ResponseEntity.ok("Webhook received"); // Return 200 to avoid retries
        }
    }

    @GetMapping("/verify/{paymentId}")
    @PreAuthorize("hasAnyRole('USER', 'LICENSED_USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> verifyPayment(@PathVariable String paymentId) {
        try {
            boolean isVerified = paymentService.verifyPayment(paymentId);
            
            if (isVerified) {
                subscriptionService.activateSubscription(paymentId);
            }
            
            return ResponseEntity.ok(Map.of(
                    "verified", isVerified,
                    "message", isVerified ? "Payment verified successfully" : "Payment verification failed"
            ));
        } catch (Exception e) {
            log.error("Error verifying payment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("User not authenticated");
        }
        
        String username = authentication.getName();
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return user.getId();
    }
}
