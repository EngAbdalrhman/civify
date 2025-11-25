package com.app.civify.dto.job;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicationRequest {
    private String cvId;
    private String jobTitle;
    private String company;
    private String jobDescription;
    private String tailoredCV;
    private String coverLetter;
    private String applicationStatus;
    private String notes;
}
