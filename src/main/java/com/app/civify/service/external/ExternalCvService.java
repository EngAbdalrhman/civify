package com.app.civify.service.external;

import com.app.civify.dto.cv.CvTailorRequest;
import com.app.civify.dto.cv.CvTailorResponse;
import com.app.civify.dto.document.CvParseRequest;
import com.app.civify.dto.document.CvParseResponse;
import com.app.civify.dto.document.MaskRequest;
import com.app.civify.dto.resume.ResumeData;
import com.app.civify.entity.User;
import com.app.civify.entity.enums.TokenUsageType;
import com.app.civify.service.cv.CvEnhancementService;
import com.app.civify.service.cvlog.CvOperationLogService;
import com.app.civify.service.document.CvParsingService;
import com.app.civify.service.document.masking.MaskingService;
import com.app.civify.service.document.WatermarkPolicyService;
import com.app.civify.service.document.pdf.PdfGenerationService;
import com.app.civify.service.subscription.SubscriptionService;
import com.app.civify.dto.external.ExternalUserProfileResponse;
import com.app.civify.entity.plans.UserSubscription;
import java.util.Optional;
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
    private final CvEnhancementService cvEnhancementService;
    private final MaskingService maskingService;
    private final PdfGenerationService pdfGenerationService;
    private final WatermarkPolicyService watermarkPolicyService;
    private final CvOperationLogService cvOperationLogService;
    private final SubscriptionService subscriptionService;
    private final ObjectMapper objectMapper;

    // Non-AI flat rate for mask operation (document processing, no AI involved)
    private static final int TOKENS_EXTERNAL_MASK = 200;

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

            var parseResponse = cvParsingService.parseCv(file, parseRequest, user.getId());
            long parseTime = System.currentTimeMillis() - parseStart;

            if (!parseResponse.isSuccess() || parseResponse.getResumeData() == null) {
                long processingTime = System.currentTimeMillis() - startTime;
                cvOperationLogService.logMask(
                        user, null, "/apis/v1/external/cvs/mask", processingTime,
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
            var watermark = watermarkPolicyService.resolveForUser(user.getId());
            byte[] pdfBytes = pdfGenerationService.generatePdf(maskedData, "modern", watermark);
            long pdfTime = System.currentTimeMillis() - pdfStart;

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("External Mask API Timing: Total=" + totalTime + "ms, Parse=" + parseTime + "ms, Mask=" + maskTime + "ms, PDF=" + pdfTime + "ms");

            // Log successful operation
            cvOperationLogService.logMask(
                    user, originalResumeData, "/apis/v1/external/cvs/mask", totalTime,
                    true, null,
                    getClientIp(httpRequest), httpRequest.getHeader("User-Agent"),
                    file.getOriginalFilename()
            );

            // Charge tokens for external API mask operation
            try {
                subscriptionService.chargeTokens(user.getId(), TOKENS_EXTERNAL_MASK, TokenUsageType.EXTERNAL_API,
                        "External API: CV Mask - " + file.getOriginalFilename());
                log.info("Logged token usage for external mask API: userId={}, file={}", user.getId(), file.getOriginalFilename());
            } catch (Exception e) {
                log.error("Failed to log token usage for external mask: {}", e.getMessage());
            }

            return pdfBytes;

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            cvOperationLogService.logMask(
                    user, originalResumeData, "/apis/v1/external/cvs/mask", processingTime,
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

        CvParseResponse response = cvParsingService.parseCv(file, request, user.getId());

        long processingTime = System.currentTimeMillis() - startTime;

        // Log the operation
        cvOperationLogService.logParse(
                user, response.getResumeData(), "/apis/v1/external/cvs/parse", processingTime,
                response.isSuccess(), response.isSuccess() ? null : response.getMessage(),
                getClientIp(httpRequest), httpRequest.getHeader("User-Agent"),
                file.getOriginalFilename(), language
        );

        // Log external API parse usage (charging is handled by CvParsingService)
        if (response.isSuccess()) {
            int tokensUsed = response.getTokensUsed();
            log.info("External API parse completed: userId={}, tokens={}, file={}",
                    user.getId(), tokensUsed, file.getOriginalFilename());
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

    /**
     * Tailors a CV for a job description. Combines parsing and tailoring in one API call.
     *
     * @param file           CV file to parse
     * @param jobDescription Job description text
     * @param jobTitle       Optional job title
     * @param companyName    Optional company name
     * @param language       Optional language hint
     * @param user           Authenticated user
     * @param httpRequest    HTTP request for logging
     * @return CvTailorResponse with tailored CV, ATS scores, and optional extras
     */
    public CvTailorResponse tailorCv(MultipartFile file, String jobDescription, String jobTitle,
                                      String companyName, String language, boolean generateCoverLetter,
                                      boolean includeInterviewQuestions, boolean includeRoadmap,
                                      User user, HttpServletRequest httpRequest) {
        long startTime = System.currentTimeMillis();
        ResumeData originalResumeData = null;

        try {
            // Step 1: Parse CV
            var parseRequest = CvParseRequest.builder()
                    .language(language)
                    .saveToDatabase(false)
                    .build();

            var parseResponse = cvParsingService.parseCv(file, parseRequest, user.getId());

            if (!parseResponse.isSuccess() || parseResponse.getResumeData() == null) {
                long processingTime = System.currentTimeMillis() - startTime;
                cvOperationLogService.logTailor(
                        user, null, "/apis/v1/external/cvs/tailor", processingTime,
                        false, "Failed to parse CV: " + parseResponse.getMessage(),
                        getClientIp(httpRequest), httpRequest.getHeader("User-Agent"),
                        jobTitle != null ? jobTitle : "unspecified"
                );
                throw new RuntimeException("Failed to parse CV: " + parseResponse.getMessage());
            }

            originalResumeData = parseResponse.getResumeData();

            // Step 2: Tailor CV
            var tailorRequest = CvTailorRequest.builder()
                    .resumeData(originalResumeData)
                    .jobDescription(jobDescription)
                    .jobTitle(jobTitle)
                    .companyName(companyName)
                    .generateCoverLetter(generateCoverLetter)
                    .includeInterviewQuestions(includeInterviewQuestions)
                    .includeRoadmap(includeRoadmap)
                    .build();

            CvTailorResponse response = cvEnhancementService.tailorCv(tailorRequest);

            long processingTime = System.currentTimeMillis() - startTime;
            response.setProcessingTimeMs(processingTime);

            // Log successful operation
            cvOperationLogService.logTailor(
                    user, response.getTailoredCv(), "/apis/v1/external/cvs/tailor", processingTime,
                    true, null,
                    getClientIp(httpRequest), httpRequest.getHeader("User-Agent"),
                    jobTitle != null ? jobTitle : "unspecified"
            );

            // Charge tokens for external API tailor operation
            try {
                // Use ACTUAL total tokens from the response
                int totalTokens = response.getTotalTokensUsed();
                subscriptionService.chargeTokens(user.getId(), totalTokens, TokenUsageType.EXTERNAL_API,
                        "External API: CV Tailor (Actual Tokens) - " + file.getOriginalFilename());
                log.info("Logged token usage for external tailor API: userId={}, tokens={}", user.getId(), totalTokens);
            } catch (Exception e) {
                log.error("Failed to log token usage for external tailor: {}", e.getMessage());
            }

            return response;

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            cvOperationLogService.logTailor(
                    user, originalResumeData, "/apis/v1/external/cvs/tailor", processingTime,
                    false, e.getMessage(),
                    getClientIp(httpRequest), httpRequest.getHeader("User-Agent"),
                    jobTitle != null ? jobTitle : "unspecified"
            );
            throw e;
        }
    }

    public ExternalUserProfileResponse getUserProfile(User user) {
        Optional<UserSubscription> subOpt = subscriptionService.getActiveSubscription(user.getId());

        String planName = "Free";
        int credits = 0;

        if (subOpt.isPresent()) {
            UserSubscription sub = subOpt.get();
            planName = sub.getPlan().getName();
            // Calculate total tokens: Subscription remaining + User Wallet (extraTokens)
            credits = sub.getRemainingSubscriptionTokens() + user.getExtraTokens();
        } else {
            // No active subscription, check if they have extra tokens (wallet)
            credits = user.getExtraTokens();
        }

        return ExternalUserProfileResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .planName(planName)
                .creditsRemaining(credits)
                .build();
    }
}

