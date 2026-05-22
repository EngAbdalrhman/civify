package com.app.civify.controller.v1.document;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.document.WatermarkConfigDto;
import com.app.civify.security.UserPrincipal;
import com.app.civify.service.document.WatermarkPolicyService;
import com.app.civify.util.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiV1Controller
@RequestMapping("/pdf")
@RequiredArgsConstructor
@Tag(name = "PDF", description = "PDF generation helpers")
public class PdfWatermarkController {

    private final WatermarkPolicyService watermarkPolicyService;

    @Operation(summary = "Resolve watermark config for the current PDF export context")
    @GetMapping("/watermark-config")
    public ResponseEntity<CommonResponse<WatermarkConfigDto>> getWatermarkConfig(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        WatermarkConfigDto config;
        if (userPrincipal == null || userPrincipal.getId() == null) {
            config = watermarkPolicyService.resolveForGuest();
        } else {
            config = watermarkPolicyService.resolveForUser(userPrincipal.getId());
        }
        return ResponseEntity.ok(CommonResponse.success(config));
    }
}
