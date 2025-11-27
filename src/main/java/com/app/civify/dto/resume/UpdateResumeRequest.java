package com.app.civify.dto.resume;

import lombok.Data;

@Data
public class UpdateResumeRequest {
    private String title;
    private ResumeData data;
}
