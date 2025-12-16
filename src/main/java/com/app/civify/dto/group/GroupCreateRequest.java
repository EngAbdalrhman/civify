package com.app.civify.dto.group;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating a new user group
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupCreateRequest {
    
    @NotBlank(message = "Group name is required")
    private String name;
    
    /**
     * List of permission IDs to assign to this group
     */
    private List<Long> permissionIds;
}
