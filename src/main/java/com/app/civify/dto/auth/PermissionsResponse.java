package com.app.civify.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for user permissions endpoint
 * Contains plain permissions and a JWT-signed token for verification
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
     * Plain list of permission names for frontend use
     * Example: ["CV.CREATE", "CV.EXPORT.PDF", "STATS.VIEW"]
     */
    private List<String> permissions;
    
    /**
     * JWT-signed permission token for secure verification
     * Frontend can decode this to verify permissions haven't been tampered with
     * Contains: permissions list, groupName, username, expiry (24 hours)
     */
    private String permissionToken;
    
    /**
     * User's group name for reference
     */
    private String groupName;
}
