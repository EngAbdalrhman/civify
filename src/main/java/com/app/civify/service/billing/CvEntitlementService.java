package com.app.civify.service.billing;

import com.app.civify.entity.billing.CvEntitlement;

import java.util.List;
import java.util.Optional;

public interface CvEntitlementService {

    /**
     * Fulfills a successful Pay-Per-CV payment transaction.
     */
    boolean fulfillPayPerCv(String orderId);

    /**
     * Checks if a specific resume is watermark-free for the user via an active entitlement.
     */
    boolean isWatermarkFree(Long userId, String resumeId);

    /**
     * Checks if a specific resume has editable Word (DOCX) export unlocked via an active entitlement.
     */
    boolean canExportDocx(Long userId, String resumeId);

    /**
     * Retrieves all entitlements for a user.
     */
    List<CvEntitlement> getUserEntitlements(Long userId);

    /**
     * Retrieves the active entitlement for a specific resume, if present.
     */
    Optional<CvEntitlement> getActiveEntitlement(Long userId, String resumeId);

    /**
     * Assigns an unassigned CV credit from a purchased pack to a specific resume.
     */
    CvEntitlement assignCreditToResume(Long userId, String resumeId);
}
