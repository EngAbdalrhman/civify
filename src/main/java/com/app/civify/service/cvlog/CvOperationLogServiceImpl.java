package com.app.civify.service.cvlog;

import com.app.civify.dto.resume.ResumeData;
import com.app.civify.entity.User;
import com.app.civify.entity.enums.CvOperationAction;
import com.app.civify.entity.logs.CvOperationLog;
import com.app.civify.repository.logs.CvOperationLogRepository;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import com.app.civify.util.persistence.CommonService;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Map;

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
    public void logUpload(User user, ResumeData resumeData, String endpoint, long processingTimeMs, boolean success, String errorMessage, String ipAddress, String userAgent, String filename, String language) {
        createAndSaveLog(user, CvOperationAction.UPLOAD, "INTERNAL", endpoint, processingTimeMs, success, errorMessage, ipAddress, userAgent,
            java.util.Map.of("filename", filename != null ? filename : "unknown", "language", language != null ? language : "unknown"));
    }

    @Override
    public void logTailor(User user, ResumeData resumeData, String endpoint, long processingTimeMs, boolean success, String errorMessage, String ipAddress, String userAgent, String jobTitle) {
        createAndSaveLog(user, com.app.civify.entity.enums.CvOperationAction.TAILOR, "INTERNAL", endpoint, processingTimeMs, success, errorMessage, ipAddress, userAgent,
            java.util.Map.of("jobTitle", jobTitle != null ? jobTitle : "unknown"));
    }

    @Override
    public void logMask(User user, ResumeData resumeData, String endpoint, long processingTimeMs, boolean success, String errorMessage, String ipAddress, String userAgent, String filename) {
        createAndSaveLog(user, CvOperationAction.MASK, "EXTERNAL_API", endpoint, processingTimeMs, success, errorMessage, ipAddress, userAgent,
            java.util.Map.of("filename", filename != null ? filename : "unknown"));
    }

    @Override
    public void logParse(User user, ResumeData resumeData, String endpoint, long processingTimeMs, boolean success, String errorMessage, String ipAddress, String userAgent, String filename, String language) {
        // Convert ResumeData to Map if present
        Map<String, Object> extractedPii = null;
        if (resumeData != null) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            try {
                extractedPii = mapper.convertValue(resumeData, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                // Ignore conversion error, just don't save PII
            }
        }

        createAndSaveLog(user, CvOperationAction.PARSE, "EXTERNAL_API", endpoint, processingTimeMs, success, errorMessage, ipAddress, userAgent,
            java.util.Map.of("filename", filename != null ? filename : "unknown", "language", language != null ? language : "unknown"), extractedPii);
    }

    private void createAndSaveLog(User user, CvOperationAction action, String source, String endpoint, long processingTimeMs, boolean success, String errorMessage, String ipAddress, String userAgent, Map<String, Object> metadata) {
        createAndSaveLog(user, action, source, endpoint, processingTimeMs, success, errorMessage, ipAddress, userAgent, metadata, null);
    }

    private void createAndSaveLog(User user, CvOperationAction action, String source, String endpoint, long processingTimeMs, boolean success, String errorMessage, String ipAddress, String userAgent, Map<String, Object> metadata, Map<String, Object> extractedPii) {
        CvOperationLog log = CvOperationLog.builder()
                .user(user)
                .action(action)
                .source(source)
                .endpoint(endpoint)
                .requestId(MDC.get("requestId"))
                .processingTimeMs(processingTimeMs)
                .success(success)
                .errorMessage(errorMessage)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .requestMetadata(metadata)
                .extractedPii(extractedPii)
                .createdAt(java.time.LocalDateTime.now())
                .build();
        repository.save(log);
    }
}
