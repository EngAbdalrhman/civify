package com.app.civify.service.cvlog;

import com.app.civify.dto.resume.ResumeData;
import com.app.civify.entity.logs.CvOperationLog;
import com.app.civify.entity.User;
import com.app.civify.entity.enums.CvOperationAction;
import com.app.civify.util.filter.PagedResponse;

/**
 * Service interface for logging CV operations.
 * Provides dedicated logging for upload, parse, tailor, and mask operations.
 */
public interface CvOperationLogService {

    /**
     * Log a CV upload operation.
     *
     * @param user          User who performed the upload (nullable for guests)
     * @param resumeData    Parsed resume data containing PII
     * @param endpoint      The endpoint path called
     * @param processingMs  Processing time in milliseconds
     * @param success       Whether the operation was successful
     * @param errorMessage  Error message if failed
     * @param ipAddress     Client IP address
     * @param userAgent     Client user agent
     * @param fileName      Name of uploaded file
     * @param language      Detected/requested language
     */
    void logUpload(User user, ResumeData resumeData, String endpoint, Long processingMs,
                   boolean success, String errorMessage, String ipAddress, String userAgent,
                   String fileName, String language);

    /**
     * Log an external CV parse operation.
     *
     * @param user          User who performed the parse (nullable for guests)
     * @param resumeData    Parsed resume data containing PII
     * @param endpoint      The endpoint path called
     * @param processingMs  Processing time in milliseconds
     * @param success       Whether the operation was successful
     * @param errorMessage  Error message if failed
     * @param ipAddress     Client IP address
     * @param userAgent     Client user agent
     * @param fileName      Name of uploaded file
     * @param language      Detected/requested language
     */
    void logParse(User user, ResumeData resumeData, String endpoint, Long processingMs,
                  boolean success, String errorMessage, String ipAddress, String userAgent,
                  String fileName, String language);

    /**
     * Log a CV tailor operation.
     *
     * @param user          User who performed the tailor (nullable for guests)
     * @param resumeData    Original resume data containing PII
     * @param endpoint      The endpoint path called
     * @param processingMs  Processing time in milliseconds
     * @param success       Whether the operation was successful
     * @param errorMessage  Error message if failed
     * @param ipAddress     Client IP address
     * @param userAgent     Client user agent
     * @param jobTitle      Job title being tailored for
     */
    void logTailor(User user, ResumeData resumeData, String endpoint, Long processingMs,
                   boolean success, String errorMessage, String ipAddress, String userAgent,
                   String jobTitle);

    /**
     * Log a CV mask operation.
     *
     * @param user          User who performed the mask (nullable for guests)
     * @param resumeData    Resume data containing PII that was masked
     * @param endpoint      The endpoint path called
     * @param processingMs  Processing time in milliseconds
     * @param success       Whether the operation was successful
     * @param errorMessage  Error message if failed
     * @param ipAddress     Client IP address
     * @param userAgent     Client user agent
     * @param fileName      Name of uploaded file
     */
    void logMask(User user, ResumeData resumeData, String endpoint, Long processingMs,
                 boolean success, String errorMessage, String ipAddress, String userAgent,
                 String fileName);

    /**
     * Get paginated CV operation logs.
     *
     * @param page Page number
     * @param size Page size
     * @return Paged response of operation logs
     */
    PagedResponse<CvOperationLog> getLogs(int page, int size);

    /**
     * Get logs filtered by action type.
     *
     * @param action Action type to filter by
     * @param page   Page number
     * @param size   Page size
     * @return Paged response of operation logs
     */
    PagedResponse<CvOperationLog> getLogsByAction(CvOperationAction action, int page, int size);
}
