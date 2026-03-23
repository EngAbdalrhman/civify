package com.app.civify.dto.referral;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User's referral statistics")
public class ReferralStatsResponse {
    @Schema(description = "User's unique referral code", example = "A1B2C3D4")
    private String referralCode;

    @Schema(description = "Number of successful referrals", example = "5")
    private int referralCount;

    @Schema(description = "Total tokens earned from referrals", example = "500")
    private int tokensEarnedFromReferrals;
}
