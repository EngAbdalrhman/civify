package com.app.civify.usecase.cv;

import com.app.civify.dto.cv.CvTailorRequest;
import com.app.civify.dto.cv.CvTailorResponse;
import com.app.civify.dto.document.CvParseRequest;
import com.app.civify.dto.document.CvParseResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * Use case interface for CV operations (upload, parse, tailor).
 */
public interface CvUseCase {

    /**
     * Upload and parse a CV file.
     *
     * @param file the CV file to parse
     * @param request parse request with language and save options
     * @param userId the authenticated user's ID (nullable for unauthenticated)
     * @param clientIp client IP address for logging
     * @param userAgent user agent string for logging
     * @return parsed CV response
     */
    CvParseResponse uploadCv(MultipartFile file, CvParseRequest request, Long userId, 
                              String clientIp, String userAgent);

    /**
     * Tailor a CV for a specific job description.
     *
     * @param request tailor request with resume data and job details
     * @param userId the authenticated user's ID (nullable for unauthenticated)
     * @param clientIp client IP address for logging
     * @param userAgent user agent string for logging
     * @return tailored CV response
     */
    CvTailorResponse tailorCv(CvTailorRequest request, Long userId, 
                               String clientIp, String userAgent);
}
