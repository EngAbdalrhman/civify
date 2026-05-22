package com.app.civify.controller.v1.admin;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.watermark.PlatformWatermarkSettingsDto;
import com.app.civify.dto.watermark.WatermarkSettingsUpdateRequest;
import com.app.civify.security.annotation.CivifyAuthorization;
import com.app.civify.service.document.WatermarkSettingsService;
import com.app.civify.util.CommonResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@ApiV1Controller
@RequestMapping("/admin/watermark")
@RequiredArgsConstructor
@Tag(name = "Admin - Watermark", description = "Platform watermark configuration")
@SecurityRequirement(name = "bearerAuth")
public class AdminWatermarkController {

    private final WatermarkSettingsService watermarkSettingsService;

    @GetMapping
    @CivifyAuthorization(permission = "PLAN.VIEW")
    public ResponseEntity<CommonResponse<PlatformWatermarkSettingsDto>> getSettings() {
        return ResponseEntity.ok(CommonResponse.success(watermarkSettingsService.getPlatformSettings()));
    }

    @PutMapping
    @CivifyAuthorization(permission = "PLAN.MODIFY")
    public ResponseEntity<CommonResponse<PlatformWatermarkSettingsDto>> updateSettings(
            @Valid @RequestBody WatermarkSettingsUpdateRequest request) {
        return ResponseEntity.ok(CommonResponse.success(watermarkSettingsService.updatePlatformSettings(request)));
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @CivifyAuthorization(permission = "PLAN.MODIFY")
    public ResponseEntity<CommonResponse<PlatformWatermarkSettingsDto>> uploadLogo(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(CommonResponse.success(watermarkSettingsService.uploadPlatformLogo(file)));
    }
}
