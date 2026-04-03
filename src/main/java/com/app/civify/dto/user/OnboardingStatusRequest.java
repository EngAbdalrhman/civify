package com.app.civify.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update onboarding and tour completion status")
public class OnboardingStatusRequest {

    @Schema(description = "Whether the general onboarding is completed")
    private Boolean onboardingCompleted;

    @Schema(description = "JSON string representing completed tours")
    private String completedTours;

}
