package com.app.civify.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for user permissions endpoint
 * Contains ONLY the JWT-signed permission token for security
 * Frontend must decode the token to get permissions - prevents tampering
 * 
 * Token payload contains: permissions, groupName, username, expiry
 * 
 * @author Abdalrhman Mostafa
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionsResponse {
    
    /**
     * JWT-signed permission token
     * Contains: permissions list, groupName, username, issued-at, expiry (24 hours)
     * Frontend decodes this to extract permissions - no plain text permissions
     */
    private String permissionToken;
}
