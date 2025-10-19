package com.app.civify.dto.auth;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Authentication request payload")
public class AuthRequest {

    @Schema(description = "Username for authentication", example = "john_doe")
    private String username;

    @Schema(description = "Email address for registration/login", example = "john@example.com")
    private String email;

    @Schema(description = "Password for authentication", example = "SecurePassword123!")
    private String password;
}
