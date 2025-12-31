package com.app.civify.service.cvlog;

import com.app.civify.entity.logs.CvOperationLog;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import com.app.civify.util.filter.InputDataModel;

public interface CvOperationLogService {
    PagedResponse<CvOperationLog> getLogs(InputDataModel inputData);

    void logUpload(com.app.civify.entity.User user, com.app.civify.dto.resume.ResumeData resumeData, String endpoint, long processingTimeMs, boolean success, String errorMessage, String ipAddress, String userAgent, String filename, String language);

    void logTailor(com.app.civify.entity.User user, com.app.civify.dto.resume.ResumeData resumeData, String endpoint, long processingTimeMs, boolean success, String errorMessage, String ipAddress, String userAgent, String jobTitle);

    void logMask(com.app.civify.entity.User user, com.app.civify.dto.resume.ResumeData resumeData, String endpoint, long processingTimeMs, boolean success, String errorMessage, String ipAddress, String userAgent, String filename);

    void logParse(com.app.civify.entity.User user, com.app.civify.dto.resume.ResumeData resumeData, String endpoint, long processingTimeMs, boolean success, String errorMessage, String ipAddress, String userAgent, String filename, String language);
}
