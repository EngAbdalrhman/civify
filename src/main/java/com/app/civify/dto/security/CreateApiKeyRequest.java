package com.app.civify.dto.security;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class CreateApiKeyRequest {
    private String name;
    private String description;
    private Set<String> scopes;
    private LocalDateTime expiresAt;
}
