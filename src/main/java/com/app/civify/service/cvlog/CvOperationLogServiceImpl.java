package com.app.civify.service.cvlog;

import com.app.civify.dto.resume.ResumeData;
import com.app.civify.entity.logs.CvOperationLog;
import com.app.civify.entity.User;
import com.app.civify.entity.enums.CvOperationAction;
import com.app.civify.repository.logs.CvOperationLogRepository;
import com.app.civify.util.filter.PagedResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of CV operation logging service.
 * Logs are created asynchronously to avoid impacting request performance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CvOperationLogServiceImpl implements CvOperationLogService {

    private final CvOperationLogRepository cvOperationLogRepository;

    private static final String SOURCE_INTERNAL = "INTERNAL";
    private static final String SOURCE_EXTERNAL = "EXTERNAL_API";

    @Override
    @Async
    @Transactional
    public void logUpload(User user, ResumeData resumeData, String endpoint, Long processingMs,
                          boolean success, String errorMessage, String ipAddress, String userAgent,
                          String fileName, String language) {
        try {
            CvOperationLog logEntry = CvOperationLog.builder()
                    .user(user)
                    .action(CvOperationAction.UPLOAD)
                    .source(SOURCE_INTERNAL)
                    .endpoint(endpoint)
                    .extractedPii(extractPii(resumeData))
                    .processingTimeMs(processingMs)
                    .success(success)
                    .errorMessage(errorMessage)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .requestMetadata(buildMetadata(fileName, language, null))
                    .build();

            cvOperationLogRepository.save(logEntry);
            log.debug("CV upload operation logged for user: {}", user != null ? user.getEmail() : "guest");
        } catch (Exception e) {
            log.error("Failed to log CV upload operation: {}", e.getMessage(), e);
        }
    }

    @Override
    @Async
    @Transactional
    public void logParse(User user, ResumeData resumeData, String endpoint, Long processingMs,
                         boolean success, String errorMessage, String ipAddress, String userAgent,
                         String fileName, String language) {
        try {
            CvOperationLog logEntry = CvOperationLog.builder()
                    .user(user)
                    .action(CvOperationAction.PARSE)
                    .source(SOURCE_EXTERNAL)
                    .endpoint(endpoint)
                    .extractedPii(extractPii(resumeData))
                    .processingTimeMs(processingMs)
                    .success(success)
                    .errorMessage(errorMessage)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .requestMetadata(buildMetadata(fileName, language, null))
                    .build();

            cvOperationLogRepository.save(logEntry);
            log.debug("CV parse operation logged for user: {}", user != null ? user.getEmail() : "guest");
        } catch (Exception e) {
            log.error("Failed to log CV parse operation: {}", e.getMessage(), e);
        }
    }

    @Override
    @Async
    @Transactional
    public void logTailor(User user, ResumeData resumeData, String endpoint, Long processingMs,
                          boolean success, String errorMessage, String ipAddress, String userAgent,
                          String jobTitle) {
        try {
            CvOperationLog logEntry = CvOperationLog.builder()
                    .user(user)
                    .action(CvOperationAction.TAILOR)
                    .source(SOURCE_INTERNAL)
                    .endpoint(endpoint)
                    .extractedPii(extractPii(resumeData))
                    .processingTimeMs(processingMs)
                    .success(success)
                    .errorMessage(errorMessage)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .requestMetadata(buildMetadata(null, null, jobTitle))
                    .build();

            cvOperationLogRepository.save(logEntry);
            log.debug("CV tailor operation logged for user: {}", user != null ? user.getEmail() : "guest");
        } catch (Exception e) {
            log.error("Failed to log CV tailor operation: {}", e.getMessage(), e);
        }
    }

    @Override
    @Async
    @Transactional
    public void logMask(User user, ResumeData resumeData, String endpoint, Long processingMs,
                        boolean success, String errorMessage, String ipAddress, String userAgent,
                        String fileName) {
        try {
            CvOperationLog logEntry = CvOperationLog.builder()
                    .user(user)
                    .action(CvOperationAction.MASK)
                    .source(SOURCE_EXTERNAL)
                    .endpoint(endpoint)
                    .extractedPii(extractPii(resumeData))
                    .processingTimeMs(processingMs)
                    .success(success)
                    .errorMessage(errorMessage)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .requestMetadata(buildMetadata(fileName, null, null))
                    .build();

            cvOperationLogRepository.save(logEntry);
            log.debug("CV mask operation logged for user: {}", user != null ? user.getEmail() : "guest");
        } catch (Exception e) {
            log.error("Failed to log CV mask operation: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<CvOperationLog> getLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CvOperationLog> logPage = cvOperationLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        return toPagedResponse(logPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<CvOperationLog> getLogsByAction(CvOperationAction action, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CvOperationLog> logPage = cvOperationLogRepository.findByAction(action, pageable);
        return toPagedResponse(logPage);
    }

    /**
     * Extract PII fields from ResumeData into a map for JSON storage.
     * Includes: name, email, phone, address, city, country, linkedin, github, website
     */
    private Map<String, Object> extractPii(ResumeData resumeData) {
        Map<String, Object> pii = new HashMap<>();

        if (resumeData == null || resumeData.getPersonalInfo() == null) {
            return pii;
        }

        ResumeData.PersonalInfo info = resumeData.getPersonalInfo();

        if (info.getFullName() != null) pii.put("fullName", info.getFullName());
        if (info.getEmail() != null) pii.put("email", info.getEmail());
        if (info.getPhone() != null) pii.put("phone", info.getPhone());
        if (info.getAddress() != null) pii.put("address", info.getAddress());
        if (info.getCity() != null) pii.put("city", info.getCity());
        if (info.getCountry() != null) pii.put("country", info.getCountry());
        if (info.getLinkedin() != null) pii.put("linkedin", info.getLinkedin());
        if (info.getGithub() != null) pii.put("github", info.getGithub());
        if (info.getWebsite() != null) pii.put("website", info.getWebsite());
        if (info.getJobTitle() != null) pii.put("jobTitle", info.getJobTitle());

        return pii;
    }

    /**
     * Build request metadata map.
     */
    private Map<String, Object> buildMetadata(String fileName, String language, String jobTitle) {
        Map<String, Object> metadata = new HashMap<>();

        if (fileName != null) metadata.put("fileName", fileName);
        if (language != null) metadata.put("language", language);
        if (jobTitle != null) metadata.put("jobTitle", jobTitle);

        return metadata.isEmpty() ? null : metadata;
    }

    /**
     * Convert Page to PagedResponse.
     */
    private PagedResponse<CvOperationLog> toPagedResponse(Page<CvOperationLog> page) {
        return PagedResponse.of(page);
    }
}
