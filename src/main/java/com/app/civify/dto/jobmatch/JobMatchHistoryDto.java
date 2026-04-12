package com.app.civify.dto.jobmatch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO projection for JobMatchHistory — avoids exposing lazy entity relations
 * (User, Resume) and prevents LazyInitializationException during serialization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobMatchHistoryDto {
    private Long id;
    private String jobTitle;
    private String companyName;
    private Integer overallScore;
    private Integer skillsScore;
    private Integer experienceScore;
    private Integer keywordScore;
    private LocalDateTime analyzedAt;
    private String resumeId;
}
