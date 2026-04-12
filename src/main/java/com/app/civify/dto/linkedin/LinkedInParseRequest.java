package com.app.civify.dto.linkedin;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkedInParseRequest {
    @NotBlank(message = "Profile text is required")
    private String profileText;
}
