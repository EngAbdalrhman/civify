package com.app.civify.controller.v1.user;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.watermark.UserWatermarkSettingsDto;
import com.app.civify.dto.watermark.WatermarkSettingsUpdateRequest;
import com.app.civify.security.UserPrincipal;
import com.app.civify.security.annotation.CivifyAuthorization;
import com.app.civify.service.document.WatermarkSettingsService;
import com.app.civify.util.CommonResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@ApiV1Controller
@RequestMapping("/users/me/watermark-settings")
@RequiredArgsConstructor
@Tag(name = "User Watermark", description = "Custom PDF watermark branding")
@SecurityRequirement(name = "bearerAuth")
public class UserWatermarkController {

    private final WatermarkSettingsService watermarkSettingsService;

    @GetMapping
    @CivifyAuthorization(permission = "CV.CUSTOM_WATERMARK")
    public ResponseEntity<CommonResponse<UserWatermarkSettingsDto>> getSettings(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(CommonResponse.success(
                watermarkSettingsService.getUserSettings(userPrincipal.getId())));
    }

    @PutMapping
    @CivifyAuthorization(permission = "CV.CUSTOM_WATERMARK")
    public ResponseEntity<CommonResponse<UserWatermarkSettingsDto>> updateSettings(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody WatermarkSettingsUpdateRequest request) {
        return ResponseEntity.ok(CommonResponse.success(
                watermarkSettingsService.updateUserSettings(userPrincipal.getId(), request)));
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @CivifyAuthorization(permission = "CV.CUSTOM_WATERMARK")
    public ResponseEntity<CommonResponse<UserWatermarkSettingsDto>> uploadLogo(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(CommonResponse.success(
                watermarkSettingsService.uploadUserLogo(userPrincipal.getId(), file)));
    }

    @DeleteMapping("/logo")
    @CivifyAuthorization(permission = "CV.CUSTOM_WATERMARK")
    public ResponseEntity<CommonResponse<Void>> deleteLogo(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        watermarkSettingsService.deleteUserLogo(userPrincipal.getId());
        return ResponseEntity.ok(CommonResponse.success(null));
    }
}
