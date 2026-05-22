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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiV1Controller
@RequestMapping("/pdf")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "PDF", description = "PDF generation helpers")
public class PdfWatermarkController {

    private final WatermarkPolicyService watermarkPolicyService;
    private final SubscriptionService subscriptionService;

    @Operation(summary = "Resolve watermark config for the current PDF export context")
    @GetMapping("/watermark-config")
    public ResponseEntity<CommonResponse<WatermarkConfigDto>> getWatermarkConfig(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        WatermarkConfigDto config;
        if (userPrincipal == null || userPrincipal.getId() == null) {
            config = watermarkPolicyService.resolveForGuest();
        } else {
            Long userId = userPrincipal.getId();
            config = watermarkPolicyService.resolveForUser(userId);
            String planInfo = subscriptionService.getActiveSubscription(userId)
                    .map(sub -> sub.getPlan().getName() + ", plan.watermark=" + sub.getPlan().getWatermark())
                    .orElse("no-active-subscription");
            log.info("PDF watermark-config userId={} plan=[{}] enabled={} imageUrl={} position={}",
                    userId,
                    planInfo,
                    config.isEnabled(),
                    config.getImageUrl() != null ? "set" : "null",
                    config.getPosition());
        }
        return ResponseEntity.ok(CommonResponse.success(config));
    }
}
