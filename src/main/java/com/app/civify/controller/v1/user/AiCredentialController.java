package com.app.civify.controller.v1.user;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.ai.credential.CreateUserAiCredentialRequest;
import com.app.civify.dto.ai.credential.UpdateUserAiCredentialRequest;
import com.app.civify.dto.ai.credential.UserAiCredentialResponse;
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

import java.util.List;
import java.util.UUID;

@ApiV1Controller
@RequestMapping("/users/me/ai-credentials")
@RequiredArgsConstructor
@Tag(name = "User AI Credentials", description = "BYOK API keys")
@SecurityRequirement(name = "bearerAuth")
public class AiCredentialController {

    private final AiCredentialService credentialService;

    @GetMapping
    @CivifyAuthorization(permission = "AI.BYOK_USE")
    public ResponseEntity<CommonResponse<List<UserAiCredentialResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(CommonResponse.success(
                credentialService.listCredentials(principal.getId())));
    }

    @PostMapping
    @CivifyAuthorization(permission = "AI.BYOK_USE")
    public ResponseEntity<CommonResponse<UserAiCredentialResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateUserAiCredentialRequest request) {
        return ResponseEntity.ok(CommonResponse.success(
                credentialService.createCredential(principal.getId(), request)));
    }

    @PostMapping("/{id}/validate")
    @CivifyAuthorization(permission = "AI.BYOK_USE")
    public ResponseEntity<CommonResponse<UserAiCredentialResponse>> validate(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(CommonResponse.success(
                credentialService.validateCredential(principal.getId(), id)));
    }

    @PatchMapping("/{id}")
    @CivifyAuthorization(permission = "AI.BYOK_USE")
    public ResponseEntity<CommonResponse<UserAiCredentialResponse>> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserAiCredentialRequest request) {
        return ResponseEntity.ok(CommonResponse.success(
                credentialService.updateCredential(principal.getId(), id, request)));
    }

    @DeleteMapping("/{id}")
    @CivifyAuthorization(permission = "AI.BYOK_USE")
    public ResponseEntity<CommonResponse<Void>> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        credentialService.deleteCredential(principal.getId(), id);
        return ResponseEntity.ok(CommonResponse.success(null));
    }
}
