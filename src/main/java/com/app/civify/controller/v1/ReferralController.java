package com.app.civify.controller.v1;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.referral.ApplyReferralRequest;
import com.app.civify.dto.referral.ReferralStatsResponse;
import com.app.civify.security.UserPrincipal;
import com.app.civify.service.referral.ReferralService;
import com.app.civify.util.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiV1Controller
@RequestMapping("/referrals")
@RequiredArgsConstructor
@Tag(name = "Referrals", description = "Referral system endpoints")
public class ReferralController {

    private final ReferralService referralService;

    @Operation(summary = "Get user referral statistics", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/me")
    public ResponseEntity<ReferralStatsResponse> getMyReferralStats(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(referralService.getMyReferralStats(userPrincipal.getId()));
    }

    @Operation(summary = "Apply a referral code (for OAuth users)", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/apply")
    public ResponseEntity<CommonResponse<String>> applyReferralCode(
            @RequestBody ApplyReferralRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        referralService.applyReferralCode(userPrincipal.getUser(), request.getReferralCode());
        
        return ResponseEntity.ok(CommonResponse.success("Referral applied successfully", null));
    }
}
