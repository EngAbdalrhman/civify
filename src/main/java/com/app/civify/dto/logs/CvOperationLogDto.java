package com.app.civify.dto.logs;

import com.app.civify.dto.user.UserSummaryDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CvOperationLogDto {
    private Long id;
    private UserSummaryDto user;
    private String action;
    private String source;
    private String endpoint;
    private String requestId;
    private Map<String, Object> extractedPii;
    private Long processingTimeMs;
    private Boolean success;
    private String errorMessage;
    private String ipAddress;
    private String userAgent;
    private Map<String, Object> requestMetadata;
    private LocalDateTime createdAt;
}
