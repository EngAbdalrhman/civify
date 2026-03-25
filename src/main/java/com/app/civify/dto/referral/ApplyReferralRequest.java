package com.app.civify.dto.referral;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Payload to apply a referral code")
public class ApplyReferralRequest {

    @Schema(description = "The referral code to apply", required = true, example = "X7Y8Z9W0")
    private String referralCode;
}
