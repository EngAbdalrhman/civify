package com.app.civify.dto.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExternalUserProfileResponse {
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String planName;
    private Integer creditsRemaining;
}
