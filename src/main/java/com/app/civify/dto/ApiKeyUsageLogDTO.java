package com.app.civify.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for API Key Usage Log responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyUsageLogDTO {
    private Long id;
    private Long userId;
    private String userEmail;
    private String endpoint;
    private String method;
    private Integer statusCode;
    private boolean success;
    private String ipAddress;
    private String errorMessage;
    private LocalDateTime timestamp;
    private Long apiKeyId;
}

