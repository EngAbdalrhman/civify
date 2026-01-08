package com.app.civify.service.external;

import com.app.civify.dto.document.CvParseRequest;
import com.app.civify.dto.document.CvParseResponse;
import com.app.civify.dto.document.MaskRequest;
import com.app.civify.dto.resume.ResumeData;
import com.app.civify.entity.User;
import com.app.civify.entity.enums.TokenUsageType;
import com.app.civify.service.cvlog.CvOperationLogService;
import com.app.civify.service.document.CvParsingService;
import com.app.civify.service.document.masking.MaskingService;
import com.app.civify.service.document.pdf.PdfGenerationService;
import com.app.civify.service.subscription.SubscriptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalCvService {

    private final CvParsingService cvParsingService;
    private final MaskingService maskingService;
    private final PdfGenerationService pdfGenerationService;
    private final CvOperationLogService cvOperationLogService;
    private final SubscriptionService subscriptionService;
    private final ObjectMapper objectMapper;

    // Token costs for external API operations
    private static final int TOKENS_EXTERNAL_MASK = 300;
    private static final int TOKENS_EXTERNAL_PARSE = 200;

    public byte[] maskCv(MultipartFile file, String language, User user, HttpServletRequest httpRequest) {
        long startTime = System.currentTimeMillis();
        ResumeData originalResumeData = null;

        try {
            // 1. Parse
            long parseStart = System.currentTimeMillis();
            var parseRequest = CvParseRequest.builder()
                    .language(language)
                    .saveToDatabase(false)
                    .piiOnly(true) // Optimization: Only extract PII for masking
                    .build();

            var parseResponse = cvParsingService.parseCv(file, parseRequest, null);
            long parseTime = System.currentTimeMillis() - parseStart;

            if (!parseResponse.isSuccess() || parseResponse.getResumeData() == null) {
                long processingTime = System.currentTimeMillis() - startTime;
                cvOperationLogService.logMask(
                        user, null, "/api/v1/external/cvs/mask", processingTime,
                        false, "Failed to parse CV",
                        getClientIp(httpRequest), httpRequest.getHeader("User-Agent"),
                        file.getOriginalFilename()
                );
                throw new RuntimeException("Failed to parse CV");
            }

            originalResumeData = parseResponse.getResumeData();

            // 2. Mask
            long maskStart = System.currentTimeMillis();
            var maskRequest = MaskRequest.builder().build();
            var maskedData = maskingService.maskResumeData(parseResponse.getResumeData(), maskRequest);
            long maskTime = System.currentTimeMillis() - maskStart;

            // 3. Generate PDF
            long pdfStart = System.currentTimeMillis();
            byte[] pdfBytes = pdfGenerationService.generatePdf(maskedData, "modern");
            long pdfTime = System.currentTimeMillis() - pdfStart;

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("External Mask API Timing: Total=" + totalTime + "ms, Parse=" + parseTime + "ms, Mask=" + maskTime + "ms, PDF=" + pdfTime + "ms");

            // Log successful operation
            cvOperationLogService.logMask(
                    user, originalResumeData, "/api/v1/external/cvs/mask", totalTime,
                    true, null,
                    getClientIp(httpRequest), httpRequest.getHeader("User-Agent"),
                    file.getOriginalFilename()
            );

            // Charge tokens for external API mask operation
            if (user != null) {
                try {
                    subscriptionService.chargeTokens(user.getId(), TOKENS_EXTERNAL_MASK, TokenUsageType.EXTERNAL_API,
                            "External API: CV Mask - " + file.getOriginalFilename());
                    log.info("Logged token usage for external mask API: userId={}, file={}", user.getId(), file.getOriginalFilename());
                } catch (Exception e) {
                    log.error("Failed to log token usage for external mask: {}", e.getMessage());
                }
            }

            return pdfBytes;

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            cvOperationLogService.logMask(
                    user, originalResumeData, "/api/v1/external/cvs/mask", processingTime,
                    false, e.getMessage(),
                    getClientIp(httpRequest), httpRequest.getHeader("User-Agent"),
                    file.getOriginalFilename()
            );
            throw e;
        }
    }

    public CvParseResponse parseCv(MultipartFile file, String language, String userTemplate, User user, HttpServletRequest httpRequest) {
        long startTime = System.currentTimeMillis();

        if (userTemplate != null && !isValidJsonOrYaml(userTemplate)) {
            throw new IllegalArgumentException("Invalid userTemplate format. Must be valid JSON or YAML.");
        }

        var request = CvParseRequest.builder()
                .language(language)
                .saveToDatabase(false)
                .userTemplate(userTemplate)
                .build();

        CvParseResponse response = cvParsingService.parseCv(file, request, null);

        long processingTime = System.currentTimeMillis() - startTime;

        // Log the operation
        cvOperationLogService.logParse(
                user, response.getResumeData(), "/api/v1/external/cvs/parse", processingTime,
                response.isSuccess(), response.isSuccess() ? null : response.getMessage(),
                getClientIp(httpRequest), httpRequest.getHeader("User-Agent"),
                file.getOriginalFilename(), language
        );

        // Charge tokens for external API parse operation
        if (user != null && response.isSuccess()) {
            try {
                subscriptionService.chargeTokens(user.getId(), TOKENS_EXTERNAL_PARSE, TokenUsageType.EXTERNAL_API,
                        "External API: CV Parse - " + file.getOriginalFilename());
                log.info("Logged token usage for external parse API: userId={}, file={}", user.getId(), file.getOriginalFilename());
            } catch (Exception e) {
                log.error("Failed to log token usage for external parse: {}", e.getMessage());
            }
        }

        return response;
    }

    private boolean isValidJsonOrYaml(String content) {
        try {
            objectMapper.readTree(content);
            return true;
        } catch (Exception e) {
            try {
                new YAMLMapper().readTree(content);
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}

