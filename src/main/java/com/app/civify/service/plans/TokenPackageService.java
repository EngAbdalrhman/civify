package com.app.civify.service.plans;

import com.app.civify.dto.payment.PaymentCreationResult;
import com.app.civify.dto.token.TokenBalanceResponse;
import com.app.civify.entity.User;
import com.app.civify.entity.plans.TokenPackage;

import com.app.civify.entity.enums.AuditAction;
import com.app.civify.entity.enums.SubscriptionStatus;
import com.app.civify.exception.ResourceNotFoundException;
import com.app.civify.repository.UserRepository;
import com.app.civify.repository.plans.TokenPackageRepository;
import com.app.civify.repository.plans.UserSubscriptionRepository;
import com.app.civify.service.audit.AuditService;
import com.app.civify.service.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenPackageService {

    private final TokenPackageRepository tokenPackageRepository;
    private final PaymentService paymentService;
    private final UserRepository userRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final AuditService auditService;

    public List<TokenPackage> getAllActivePackages() {
        return tokenPackageRepository.findByActiveTrue();
    }

    public List<TokenPackage> getAllPackages() {
        return tokenPackageRepository.findAll();
    }

    /**
     * Get user's token balance including wallet and subscription info.
     * Use efficient projection queries.
     */
    @Transactional(readOnly = true)
    public TokenBalanceResponse getUserTokenBalance(Long userId) {
        // 1. Fetch wallet tokens efficiently
        // Returns 0 if user not found or tokens null, effectively handling "no wallet" case
        int walletTokens = userRepository.findExtraTokensByUserId(userId).orElse(0); 
        
        // 2. Fetch active subscription info efficiently
        var tokenInfoOpt = subscriptionRepository.findTokenInfoByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE);
        
        Integer monthlyLimit = null;
        Integer monthlyUsed = 0;
        Integer monthlyRemaining = 0;
        
        if (tokenInfoOpt.isPresent()) {
            var info = tokenInfoOpt.get();
            monthlyLimit = info.getTokenLimit();
            monthlyUsed = info.getCurrentTokenUsage() != null ? info.getCurrentTokenUsage() : 0;
            
            if (monthlyLimit != null) {
                monthlyRemaining = Math.max(0, monthlyLimit - monthlyUsed);
            } else {
                // Unlimited monthly tokens
                monthlyRemaining = Integer.MAX_VALUE;
            }
        }
        
        // Calculate total available
        long totalCalc = (long) monthlyRemaining + walletTokens;
        int totalAvailable = (monthlyLimit == null && tokenInfoOpt.isPresent()) 
                ? Integer.MAX_VALUE 
                : (totalCalc > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) totalCalc);
        
        return TokenBalanceResponse.builder()
                .walletTokens(walletTokens)
                .monthlyLimit(monthlyLimit)
                .monthlyUsed(monthlyUsed)
                .monthlyRemaining(monthlyRemaining)
                .totalAvailable(totalAvailable)
                .build();
    }

    public TokenPackage getPackageById(Long id) {
        return tokenPackageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Token package not found with id: " + id));
    }

    @Transactional
    public TokenPackage createPackage(TokenPackage tokenPackage) {
        tokenPackage.setActive(true);
        TokenPackage saved = tokenPackageRepository.save(tokenPackage);
        logAudit(saved.getId(), AuditAction.CREATE, null, saved,
                "Created token package: " + saved.getName() + " (" + saved.getTokens() + " tokens)");
        return saved;
    }

    @Transactional
    public TokenPackage updatePackage(Long id, TokenPackage updated) {
        TokenPackage existing = getPackageById(id);
        
        // Capture old state for audit
        TokenPackage oldState = new TokenPackage();
        oldState.setName(existing.getName());
        oldState.setTokens(existing.getTokens());
        oldState.setPrice(existing.getPrice());
        oldState.setCurrency(existing.getCurrency());
        oldState.setDescription(existing.getDescription());
        oldState.setActive(existing.getActive());
        
        // Apply updates
        existing.setName(updated.getName());
        existing.setTokens(updated.getTokens());
        existing.setPrice(updated.getPrice());
        existing.setCurrency(updated.getCurrency());
        existing.setDescription(updated.getDescription());
        if (updated.getActive() != null) {
            existing.setActive(updated.getActive());
        }
        
        TokenPackage saved = tokenPackageRepository.save(existing);
        logAudit(saved.getId(), AuditAction.UPDATE, oldState, saved,
                "Updated token package: " + saved.getName());
        return saved;
    }

    @Transactional
    public void deactivatePackage(Long id) {
        TokenPackage existing = getPackageById(id);
        existing.setActive(false);
        tokenPackageRepository.save(existing);
        logAudit(id, AuditAction.DELETE, null, null,
                "Deactivated token package: " + existing.getName());
    }

    private void logAudit(Long entityId, AuditAction action, TokenPackage oldVal, TokenPackage newVal, String comment) {
        try {
            Map<String, Object> oldMap = oldVal != null ? toMap(oldVal) : null;
            Map<String, Object> newMap = newVal != null ? toMap(newVal) : null;
            auditService.logAudit("TokenPackage", entityId.toString(), action, oldMap, newMap, comment);
        } catch (Exception e) {
            log.warn("Failed to log audit for TokenPackage action: {}", action, e);
        }
    }

    private Map<String, Object> toMap(TokenPackage pkg) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", pkg.getName());
        map.put("tokens", pkg.getTokens());
        map.put("price", pkg.getPrice());
        map.put("currency", pkg.getCurrency());
        map.put("active", pkg.getActive());
        return map;
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
     * Adds tokens to user's wallet (extraTokens on User entity).
     */
    @Transactional
    public void fulfillTokenPurchase(Long userId, Integer tokens) {
        log.info("Fulfilling token purchase for user {}: +{} tokens", userId, tokens);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Add tokens to user's wallet
        Integer currentExtra = user.getExtraTokens();
        if (currentExtra == null) currentExtra = 0;
        user.setExtraTokens(currentExtra + tokens);
        
        userRepository.save(user);
        log.info("Tokens added to user {} wallet. New balance: {}", userId, user.getExtraTokens());
    }
}
