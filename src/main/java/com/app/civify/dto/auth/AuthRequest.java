package com.app.civify.dto.auth;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Authentication request payload - provide either username OR email, not both")
public class AuthRequest {

    @Schema(description = "Username for authentication (optional if email is provided)", example = "john_doe")
    private String username;

    @Schema(description = "Email address for authentication (optional if username is provided)", example = "john@example.com")
    private String email;

    @Schema(description = "Password for authentication", example = "SecurePassword123!", required = true)
    private String password;
}
