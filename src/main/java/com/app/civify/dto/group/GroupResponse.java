package com.app.civify.dto.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for group with permissions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupResponse {
    
    private Long id;
    private String name;
    private Boolean isSystemGroup;
    private Boolean isDefault;
    private List<PermissionDto> permissions;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionDto {
        private Long id;
        private String name;
        private String description;
        private String category;
    }
}
