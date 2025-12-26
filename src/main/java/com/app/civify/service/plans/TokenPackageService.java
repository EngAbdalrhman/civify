package com.app.civify.service.plans;

import com.app.civify.dto.payment.PaymentCreationResult;
import com.app.civify.entity.plans.TokenPackage;
import com.app.civify.entity.plans.UserSubscription;
import com.app.civify.entity.enums.AuditAction;
import com.app.civify.entity.enums.SubscriptionStatus;
import com.app.civify.repository.plans.TokenPackageRepository;
import com.app.civify.repository.plans.UserSubscriptionRepository;
import com.app.civify.service.audit.AuditService;
import com.app.civify.service.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenPackageService {

    private final TokenPackageRepository tokenPackageRepository;
    private final PaymentService paymentService;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final AuditService auditService;

    public List<TokenPackage> getAllActivePackages() {
        return tokenPackageRepository.findByActiveTrue();
    }

    public TokenPackage createPackage(TokenPackage tokenPackage) {
        TokenPackage saved = tokenPackageRepository.save(tokenPackage);
        try {
            auditService.logAudit("TokenPackage", saved.getId().toString(), 
                    AuditAction.CREATE, 
                    "Created token package: " + saved.getName() + " (" + saved.getTokens() + " tokens)");
        } catch (Exception e) {
            log.warn("Failed to log audit for token package creation", e);
        }
        return saved;
    }

    @Transactional
    public PaymentCreationResult purchasePackage(Long userId, Long packageId) {
        TokenPackage tokenPackage = tokenPackageRepository.findById(packageId)
                .orElseThrow(() -> new RuntimeException("Token package not found"));

        if (!tokenPackage.getActive()) {
            throw new RuntimeException("This package is no longer available");
        }

        // Prepare metadata for the payment callback handler
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", "TOKEN_TOPUP");
        metadata.put("userId", userId);
        metadata.put("packageId", packageId);
        metadata.put("tokens", tokenPackage.getTokens());

        log.info("Initiating token purchase for user {} - Package: {}", userId, tokenPackage.getName());

        return paymentService.createPayment(userId, tokenPackage.getPrice(), metadata);
    }

    /**
     * Called by Payment Callback/Handler when payment is COMPLETED.
     */
    @Transactional
    public void fulfillTokenPurchase(Long userId, Integer tokens) {
        log.info("Fulfilling token purchase for user {}: +{} tokens", userId, tokens);
        
        // Find active subscription to add tokens to
        // If no active subscription, maybe find the user's latest or create a "Free" one?
        // Assuming user must have a subscription (even Free) to have an App Account.
        UserSubscription subscription = userSubscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .stream().findFirst()
                .orElse(null);

        if (subscription == null) {
            // Fallback: try to find any subscription or handle error
            // ideally we reactivate Free or similar. 
            // For now, let's find the most recent one.
            log.warn("No ACTIVE subscription found for user {} for token top-up. Checking others.", userId);
            subscription = userSubscriptionRepository.findByUserId(userId).stream()
                    .max((a, b) -> a.getEndDate().compareTo(b.getEndDate()))
                    .orElseThrow(() -> new RuntimeException("User has no subscription record"));
        }

        // Add tokens
        Integer currentExtra = subscription.getExtraTokens();
        if (currentExtra == null) currentExtra = 0;
        subscription.setExtraTokens(currentExtra + tokens);
        
        userSubscriptionRepository.save(subscription);
        log.info("Tokens added. New balance: {}", subscription.getExtraTokens());
    }
}
