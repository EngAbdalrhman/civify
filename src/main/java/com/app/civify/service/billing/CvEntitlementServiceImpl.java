package com.app.civify.service.billing;

import com.app.civify.entity.billing.BillingTransaction;
import com.app.civify.entity.billing.CvEntitlement;
import com.app.civify.entity.enums.PaymentStatus;
import com.app.civify.repository.billing.BillingTransactionRepository;
import com.app.civify.repository.billing.CvEntitlementRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CvEntitlementServiceImpl implements CvEntitlementService {

    private final CvEntitlementRepository entitlementRepository;
    private final BillingTransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    private static final int DEFAULT_ENTITLEMENT_DAYS = 30;

    @Override
    @Transactional
    public boolean fulfillPayPerCv(String orderId) {
        log.info("Attempting to fulfill Pay-Per-CV for orderId: {}", orderId);

        Optional<BillingTransaction> txOpt = transactionRepository.findByOrderId(orderId);
        if (txOpt.isEmpty()) {
            log.error("Pay-Per-CV transaction not found for orderId: {}", orderId);
            return false;
        }

        BillingTransaction transaction = txOpt.get();

        // Check if already fulfilled to avoid duplicate grants
        Optional<CvEntitlement> existing = entitlementRepository.findByOrderId(orderId);
        if (existing.isPresent()) {
            log.info("Pay-Per-CV order {} already fulfilled with entitlement id {}", orderId, existing.get().getId());
            return true;
        }

        String resumeId = null;
        String entitlementType = "SINGLE_CV";
        int credits = 1;

        if (transaction.getMetadata() != null && !transaction.getMetadata().isBlank()) {
            try {
                Map<String, Object> meta = objectMapper.readValue(
                        transaction.getMetadata(),
                        new TypeReference<Map<String, Object>>() {}
                );
                if (meta.containsKey("resumeId") && meta.get("resumeId") != null) {
                    resumeId = String.valueOf(meta.get("resumeId"));
                }
                if (meta.containsKey("entitlementType") && meta.get("entitlementType") != null) {
                    entitlementType = String.valueOf(meta.get("entitlementType"));
                }
                if (meta.containsKey("credits") && meta.get("credits") != null) {
                    credits = Integer.parseInt(String.valueOf(meta.get("credits")));
                }
            } catch (Exception e) {
                log.warn("Failed to parse metadata JSON for transaction {}: {}", transaction.getId(), e.getMessage());
            }
        }

        Instant expiresAt = Instant.now().plus(DEFAULT_ENTITLEMENT_DAYS, ChronoUnit.DAYS);

        CvEntitlement entitlement = CvEntitlement.builder()
                .userId(transaction.getUserId())
                .resumeId(resumeId)
                .transactionId(transaction.getId())
                .orderId(transaction.getOrderId())
                .entitlementType(entitlementType)
                .creditsRemaining(credits)
                .watermarkFree(true)
                .allowDocExport(true)
                .unlimitedEditsUntil(expiresAt)
                .status("ACTIVE")
                .build();

        entitlementRepository.save(entitlement);
        log.info("Granted {} entitlement (id: {}) to user {} for resume {} until {}",
                entitlementType, entitlement.getId(), transaction.getUserId(), resumeId, expiresAt);

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isWatermarkFree(Long userId, String resumeId) {
        if (userId == null || resumeId == null || resumeId.isBlank()) {
            return false;
        }
        return entitlementRepository.existsActiveForResume(userId, resumeId, Instant.now());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canExportDocx(Long userId, String resumeId) {
        if (userId == null || resumeId == null || resumeId.isBlank()) {
            return false;
        }
        return entitlementRepository.existsActiveForResume(userId, resumeId, Instant.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CvEntitlement> getUserEntitlements(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return entitlementRepository.findByUserIdAndStatus(userId, "ACTIVE");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CvEntitlement> getActiveEntitlement(Long userId, String resumeId) {
        if (userId == null || resumeId == null) {
            return Optional.empty();
        }
        return entitlementRepository.findByUserIdAndResumeIdAndStatus(userId, resumeId, "ACTIVE")
                .filter(CvEntitlement::isValid);
    }

    @Override
    @Transactional
    public CvEntitlement assignCreditToResume(Long userId, String resumeId) {
        if (userId == null || resumeId == null || resumeId.isBlank()) {
            throw new IllegalArgumentException("User ID and Resume ID cannot be null or blank");
        }

        List<CvEntitlement> unassigned = entitlementRepository.findUnassignedCredits(userId, Instant.now());
        if (unassigned.isEmpty()) {
            throw new IllegalStateException("No available unassigned CV credits found");
        }

        CvEntitlement source = unassigned.get(0);
        if (source.getResumeId() == null || source.getResumeId().isBlank()) {
            source.setResumeId(resumeId);
            source.setCreditsRemaining(source.getCreditsRemaining() - 1);
            source.setUnlimitedEditsUntil(Instant.now().plus(DEFAULT_ENTITLEMENT_DAYS, ChronoUnit.DAYS));
            return entitlementRepository.save(source);
        }

        // If it's a multi-pack, create a separate assigned entitlement child
        source.setCreditsRemaining(source.getCreditsRemaining() - 1);
        entitlementRepository.save(source);

        CvEntitlement assigned = CvEntitlement.builder()
                .userId(userId)
                .resumeId(resumeId)
                .transactionId(source.getTransactionId())
                .orderId(source.getOrderId())
                .entitlementType("PACK_ITEM")
                .creditsRemaining(0)
                .watermarkFree(true)
                .allowDocExport(true)
                .unlimitedEditsUntil(Instant.now().plus(DEFAULT_ENTITLEMENT_DAYS, ChronoUnit.DAYS))
                .status("ACTIVE")
                .build();

        return entitlementRepository.save(assigned);
    }
}
