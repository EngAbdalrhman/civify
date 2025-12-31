package com.app.civify.service.cvlog;

import com.app.civify.entity.logs.CvOperationLog;
import com.app.civify.repository.logs.CvOperationLogRepository;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import com.app.civify.util.persistence.CommonService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CvOperationLogServiceImpl extends CommonService implements CvOperationLogService {

    private final CvOperationLogRepository repository;

    @Override
    public PagedResponse<CvOperationLog> getLogs(InputDataModel inputData) {
        Page<CvOperationLog> page = filterEntities(repository, inputData);
        
        PagedResponse<CvOperationLog> response = new PagedResponse<>();
        response.setContent(page.getContent());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        
        return response;
    }

    @Override
    public void logUpload(com.app.civify.entity.User user, com.app.civify.dto.resume.ResumeData resumeData, String endpoint, long processingTimeMs, boolean success, String errorMessage, String ipAddress, String userAgent, String filename, String language) {
        createAndSaveLog(user, com.app.civify.entity.enums.CvOperationAction.UPLOAD, "INTERNAL", endpoint, processingTimeMs, success, errorMessage, ipAddress, userAgent, 
            java.util.Map.of("filename", filename != null ? filename : "unknown", "language", language != null ? language : "unknown"));
    }

    @Override
    public void logTailor(com.app.civify.entity.User user, com.app.civify.dto.resume.ResumeData resumeData, String endpoint, long processingTimeMs, boolean success, String errorMessage, String ipAddress, String userAgent, String jobTitle) {
        createAndSaveLog(user, com.app.civify.entity.enums.CvOperationAction.TAILOR, "INTERNAL", endpoint, processingTimeMs, success, errorMessage, ipAddress, userAgent,
            java.util.Map.of("jobTitle", jobTitle != null ? jobTitle : "unknown"));
    }

    @Override
    public void logMask(com.app.civify.entity.User user, com.app.civify.dto.resume.ResumeData resumeData, String endpoint, long processingTimeMs, boolean success, String errorMessage, String ipAddress, String userAgent, String filename) {
        createAndSaveLog(user, com.app.civify.entity.enums.CvOperationAction.MASK, "EXTERNAL_API", endpoint, processingTimeMs, success, errorMessage, ipAddress, userAgent,
            java.util.Map.of("filename", filename != null ? filename : "unknown"));
    }

    @Override
    public void logParse(com.app.civify.entity.User user, com.app.civify.dto.resume.ResumeData resumeData, String endpoint, long processingTimeMs, boolean success, String errorMessage, String ipAddress, String userAgent, String filename, String language) {
        createAndSaveLog(user, com.app.civify.entity.enums.CvOperationAction.PARSE, "EXTERNAL_API", endpoint, processingTimeMs, success, errorMessage, ipAddress, userAgent,
            java.util.Map.of("filename", filename != null ? filename : "unknown", "language", language != null ? language : "unknown"));
    }

    private void createAndSaveLog(com.app.civify.entity.User user, com.app.civify.entity.enums.CvOperationAction action, String source, String endpoint, long processingTimeMs, boolean success, String errorMessage, String ipAddress, String userAgent, java.util.Map<String, Object> metadata) {
        CvOperationLog log = CvOperationLog.builder()
                .user(user)
                .action(action)
                .source(source)
                .endpoint(endpoint)
                .processingTimeMs(processingTimeMs)
                .success(success)
                .errorMessage(errorMessage)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .requestMetadata(metadata)
                .createdAt(java.time.LocalDateTime.now())
                .build();
        repository.save(log);
    }
}
