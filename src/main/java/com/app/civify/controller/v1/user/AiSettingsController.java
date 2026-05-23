package com.app.civify.controller.v1.user;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.ai.credential.UpdateUserAiSettingsRequest;
import com.app.civify.dto.ai.credential.UserAiSettingsResponse;
import com.app.civify.security.UserPrincipal;
import com.app.civify.security.annotation.CivifyAuthorization;
import com.app.civify.service.ai.credential.AiCredentialService;
import com.app.civify.util.CommonResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@ApiV1Controller
@RequestMapping("/users/me/ai-settings")
@RequiredArgsConstructor
@Tag(name = "User AI Settings", description = "BYOK mode and fallback chain")
@SecurityRequirement(name = "bearerAuth")
public class AiSettingsController {

    private final AiCredentialService credentialService;

    @GetMapping
    @CivifyAuthorization(permission = "AI.BYOK_USE")
    public ResponseEntity<CommonResponse<UserAiSettingsResponse>> getSettings(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(CommonResponse.success(
                credentialService.getSettings(principal.getId())));
    }

    @PutMapping
    @CivifyAuthorization(permission = "AI.BYOK_USE")
    public ResponseEntity<CommonResponse<UserAiSettingsResponse>> updateSettings(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateUserAiSettingsRequest request) {
        return ResponseEntity.ok(CommonResponse.success(
                credentialService.updateSettings(principal.getId(), request)));
    }
}
