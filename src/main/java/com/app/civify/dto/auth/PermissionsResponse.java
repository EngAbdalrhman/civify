package com.app.civify.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for user permissions endpoint
 * Contains both plain and encrypted permission lists
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
     * Example: ["cv.create", "cv.update", "plan.view"]
     */
    private List<String> permissions;
    
    /**
     * AES-256 encrypted and Base64 encoded permissions
     * Provides additional security layer and prevents client-side tampering
     */
    private String encryptedPermissions;
    
    /**
     * User's group name for reference
     */
    private String groupName;
}
