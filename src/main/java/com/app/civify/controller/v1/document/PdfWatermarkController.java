package com.app.civify.controller.v1.document;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.document.WatermarkConfigDto;
import com.app.civify.security.UserPrincipal;
import com.app.civify.service.document.WatermarkPolicyService;
import com.app.civify.service.subscription.SubscriptionService;
import com.app.civify.util.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiV1Controller
@RequestMapping("/pdf")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "PDF", description = "PDF generation helpers")
public class PdfWatermarkController {

    private final WatermarkPolicyService watermarkPolicyService;
    private final SubscriptionService subscriptionService;

    @Value("${pdf.internal.secret:}")
    private String pdfInternalSecret;

    @Operation(summary = "Resolve watermark config for the current PDF export context")
    @GetMapping("/watermark-config")
    public ResponseEntity<CommonResponse<WatermarkConfigDto>> getWatermarkConfig(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String resumeId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestHeader(value = "X-Pdf-Internal-Token", required = false) String pdfInternalToken,
            @RequestHeader(value = "X-Pdf-User-Id", required = false) Long pdfUserId) {
        WatermarkConfigDto config;
        Long userId = userPrincipal != null ? userPrincipal.getId() : null;
        if (userId == null && pdfUserId != null && isTrustedPdfInternalCall(pdfInternalToken)) {
            userId = pdfUserId;
        }

        if (userId == null) {
            config = watermarkPolicyService.resolveForGuest();
        } else {
            config = watermarkPolicyService.resolveForUserAndResume(userId, resumeId);
            String planInfo = subscriptionService.getActiveSubscription(userId)
                    .map(sub -> sub.getPlan().getName() + ", plan.watermark=" + sub.getPlan().getWatermark())
                    .orElse("no-active-subscription");
            log.info("PDF watermark-config userId={} resumeId={} plan=[{}] enabled={} imageUrl={} position={}",
                    userId,
                    resumeId,
                    planInfo,
                    config.isEnabled(),
                    config.getImageUrl() != null ? "set" : "null",
                    config.getPosition());
        }
        return ResponseEntity.ok(CommonResponse.success(config));
    }

    private boolean isTrustedPdfInternalCall(String pdfInternalToken) {
        return pdfInternalSecret != null
                && !pdfInternalSecret.isBlank()
                && pdfInternalSecret.equals(pdfInternalToken);
    }
}
