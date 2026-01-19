package com.app.civify.service.plans;

import com.app.civify.dto.payment.PaymentCreationResult;
import com.app.civify.dto.payment.PaymentMethods;
import com.app.civify.dto.token.TokenBalanceResponse;
import com.app.civify.entity.User;
import com.app.civify.entity.enums.AuditAction;
import com.app.civify.entity.enums.PromoApplicabilityType;
import com.app.civify.entity.enums.SubscriptionStatus;
import com.app.civify.entity.plans.PromoCode;
import com.app.civify.entity.plans.TokenPackage;
import com.app.civify.entity.plans.TokenPurchase;
import com.app.civify.entity.plans.UserPromoUsage;
import com.app.civify.exception.ResourceNotFoundException;
import com.app.civify.repository.UserRepository;
import com.app.civify.repository.plans.TokenPackageRepository;
import com.app.civify.repository.plans.TokenPurchaseRepository;
import com.app.civify.repository.plans.UserPromoUsageRepository;
import com.app.civify.repository.plans.UserSubscriptionRepository;
import com.app.civify.service.audit.AuditService;
import com.app.civify.service.payment.PaymentService;
import com.app.civify.service.payment.PromoCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final UserRepository userRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final AuditService auditService;
    private final PromoCodeService promoCodeService;
    private final TokenPurchaseRepository tokenPurchaseRepository;
    private final UserPromoUsageRepository userPromoUsageRepository;
    private final com.app.civify.service.i18n.MessageService messageService;

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
        int monthlyUsed = 0;
        int monthlyRemaining = 0;
        
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
                .orElseThrow(() -> new RuntimeException("token.package.notfound"));
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
    public PaymentCreationResult purchasePackage(Long userId, Long packageId, String promoCode, String gateway, PaymentMethods methods) {
        TokenPackage tokenPackage = tokenPackageRepository.findById(packageId)
                .orElseThrow(() -> new RuntimeException("token.package.notfound"));

        if (!tokenPackage.getActive()) {
            throw new RuntimeException("token.package.unavailable");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("user.notfound"));

        BigDecimal finalPrice = BigDecimal.valueOf(tokenPackage.getPrice());
        PromoCode promo = null;

        // Validate promo code
        if (promoCode != null && !promoCode.isEmpty()) {
            var promoOpt = promoCodeService.findByCode(promoCode);
            if (promoOpt.isPresent()) {
                promo = promoOpt.get();
                // Validate generic rules (expiry, usage limit)
                if (!promo.isValid()) {
                     // Or throw specific exception if strict feedback needed
                     log.warn("Invalid promo code used in token purchase: {}", promoCode);
                } 
                // Check applicability
                else if (promo.getApplicabilityType() == PromoApplicabilityType.PLAN) {
                    log.warn("Plan-only promo code used for tokens: {}", promoCode);
                }
                // Check user usage
                else if (userPromoUsageRepository.existsByUserIdAndPromoCodeId(userId, promo.getId())) {
                    log.warn("Repo already used promo code: {}", promoCode);
                } else {
                    // Apply discount
                    BigDecimal discount = promo.calculateDiscount(finalPrice);
                    finalPrice = finalPrice.subtract(discount).max(BigDecimal.ZERO);
                }
            }
        }
        // NOTE: We don't record usage here. We record it upon successful payment.

        // Create TokenPurchase PENDING
        TokenPurchase purchase = TokenPurchase.builder()
                .user(user)
                .tokenPackage(tokenPackage)
                .promoCode(promo) // can be null
                .amount(finalPrice)
                .tokens(tokenPackage.getTokens())
                .status(TokenPurchase.TokenPurchaseStatus.PENDING)
                .build();
        
        purchase = tokenPurchaseRepository.save(purchase);

        // Prepare metadata for the payment callback handler
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", "TOKEN_TOPUP");
        metadata.put("userId", userId);
        metadata.put("packageId", packageId);
        metadata.put("purchaseId", purchase.getId()); // Helper to find it back if needed, but we rely on orderId usually

        log.info("Initiating token purchase for user {} - Package: {} - Amount: {}", userId, tokenPackage.getName(), finalPrice);

        PaymentCreationResult paymentResult = paymentService.createPayment(userId, finalPrice.doubleValue(), gateway, methods, metadata);
        
        // Link purchase to payment transaction
        purchase.setPaymentTransactionId(paymentResult.getPaymentKey());
        tokenPurchaseRepository.save(purchase);
        
        return paymentResult;
    }

    /**
     * Called by Payment Callback/Handler when payment is COMPLETED.
     * Updated to use TokenPurchase entity for safety.
     * @param paymentRef can be orderId or paymentKey depending on what we get from webhook
     */
    @Transactional
    public void fulfillTokenPurchase(String paymentRef) {
        log.info("Fulfilling token purchase for payment ref: {}", paymentRef);
        
        // Find by transaction ID
        var purchaseOpt = tokenPurchaseRepository.findByPaymentTransactionId(paymentRef);
        if (purchaseOpt.isEmpty()) {
            // TODO Fallback: try finding by orderId if that's what was passed (sometimes gateways confuse terms)
            // Ideally we'd have search methods for both or normalize before calling.
            // For now, if not found, we might be receiving the 'orderId' but stored 'paymentKey' or vice versa.
            // Let's assume passed ref matches stored ref. if not found, we log error.
             log.error("TokenPurchase not found for payment ref: {}", paymentRef);
             // Logic for fallback via metadata if we passed it? 
             // Metadata has purchaseId. If we had that, we could look it up directly.
             // But signature here is generic.
             return;
        }
        
        TokenPurchase purchase = purchaseOpt.get();
        
        if (purchase.getStatus() == TokenPurchase.TokenPurchaseStatus.COMPLETED) {
            log.info("Token purchase {} already completed.", purchase.getId());
            return;
        }

        purchase.setStatus(TokenPurchase.TokenPurchaseStatus.COMPLETED);
        purchase.setCompletedAt(LocalDateTime.now());
        tokenPurchaseRepository.save(purchase);

        // Add tokens to user
        int currentExtra = userRepository.findExtraTokensByUserId(purchase.getUser().getId()).orElse(0); 
        currentExtra += purchase.getTokens();
        userRepository.updateExtraTokens(purchase.getUser().getId(), currentExtra);
        
        log.info("Tokens (+{}) added to user {} wallet. New balance: {}", purchase.getTokens(), purchase.getUser().getId(), currentExtra);
        
        // Record Promo Usage if applicable and not already recorded
        if (purchase.getPromoCode() != null) {
            recordPromoUsage(purchase.getUser(), purchase.getPromoCode());
        }
    }
    
    private void recordPromoUsage(User user, PromoCode promo) {
        if (userPromoUsageRepository.existsByUserIdAndPromoCodeId(user.getId(), promo.getId())) {
            log.warn("Promo usage already recorded for user {} code {}", user.getId(), promo.getCode());
            return;
        }

        UserPromoUsage usage = UserPromoUsage.builder()
                .user(user)
                .promoCode(promo)
                .usedAt(LocalDateTime.now())
                .build();
        userPromoUsageRepository.save(usage);

        // Increment global usage count
        promo.setUsedCount(promo.getUsedCount() + 1);
        promoCodeService.updatePromoCode(promo.getId(), promo); // Ensure count persists or use repo directly
    }
    @Transactional(readOnly = true)
    public com.app.civify.dto.payment.PurchasePreviewResponse previewTokenPurchase(Long userId, Long packageId, String promoCode) {
        TokenPackage tokenPackage = tokenPackageRepository.findById(packageId)
                .orElseThrow(() -> new RuntimeException("token.package.notfound"));

        if (!tokenPackage.getActive()) {
            throw new RuntimeException("token.package.unavailable");
        }

        BigDecimal originalPrice = BigDecimal.valueOf(tokenPackage.getPrice());
        BigDecimal finalPrice = originalPrice;
        BigDecimal discountAmount = BigDecimal.ZERO;
        String message = null;
        boolean promoApplied = false;

        if (promoCode != null && !promoCode.isEmpty()) {
            var promoOpt = promoCodeService.findByCode(promoCode);
            if (promoOpt.isPresent()) {
                PromoCode promo = promoOpt.get();
                if (!promo.isValid()) {
                    message = messageService.getMessage("promo.invalid");
                } else if (promo.getApplicabilityType() == PromoApplicabilityType.PLAN) {
                    message = messageService.getMessage("promo.notforplan"); // Reusing this key or create promo.planonly
                } else if (userPromoUsageRepository.existsByUserIdAndPromoCodeId(userId, promo.getId())) {
                    message = messageService.getMessage("promo.alreadyused");
                } else {
                    BigDecimal discount = promo.calculateDiscount(originalPrice);
                    discountAmount = discount;
                    finalPrice = originalPrice.subtract(discount).max(BigDecimal.ZERO);
                    promoApplied = true;
                    message = messageService.getMessage("promo.applied");
                }
            } else {
                message = messageService.getMessage("promo.notfound");
            }
        }

        return com.app.civify.dto.payment.PurchasePreviewResponse.builder()
                .originalPrice(originalPrice)
                .discountAmount(discountAmount)
                .finalPrice(finalPrice)
                .currency(tokenPackage.getCurrency())
                .promoCode(promoCode)
                .promoApplied(promoApplied)
                .message(message)
                .build();
    }
}
