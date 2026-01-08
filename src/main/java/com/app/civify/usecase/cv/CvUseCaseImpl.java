package com.app.civify.usecase.cv;

import com.app.civify.dto.cv.CvTailorRequest;
import com.app.civify.dto.cv.CvTailorResponse;
import com.app.civify.dto.document.CvParseRequest;
import com.app.civify.dto.document.CvParseResponse;
import com.app.civify.entity.User;
import com.app.civify.entity.enums.TokenUsageType;
import com.app.civify.repository.UserRepository;
import com.app.civify.service.cv.CvEnhancementService;
import com.app.civify.service.cvlog.CvOperationLogService;
import com.app.civify.service.document.CvParsingService;
import com.app.civify.service.subscription.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Implementation of CV use case handling upload and tailor operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CvUseCaseImpl implements CvUseCase {

    private final CvParsingService cvParsingService;
    private final CvEnhancementService cvEnhancementService;
    private final CvOperationLogService cvOperationLogService;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;

    // Estimated tokens for different operations (base values, can be adjusted)
    private static final int TOKENS_CV_TAILOR = 500;
    private static final int TOKENS_COVER_LETTER = 200;
    private static final int TOKENS_INTERVIEW_PREP = 300;
    private static final int TOKENS_ROADMAP = 250;
    private static final int TOKENS_ATS_SCORE = 100;

    @Override
    public CvParseResponse uploadCv(MultipartFile file, CvParseRequest request, Long userId,
                                     String clientIp, String userAgent) {
        long startTime = System.currentTimeMillis();
        log.info("Uploading CV: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());

        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
        CvParseResponse response = cvParsingService.parseCv(file, request, userId);

        long processingTime = System.currentTimeMillis() - startTime;

        cvOperationLogService.logUpload(
                user,
                response.getResumeData(),
                "/api/v1/cvs/upload",
                processingTime,
                response.isSuccess(),
                response.isSuccess() ? null : response.getMessage(),
                clientIp,
                userAgent,
                file.getOriginalFilename(),
                request.getLanguage()
        );

        return response;
    }

    @Override
    public CvTailorResponse tailorCv(CvTailorRequest request, Long userId,
                                      String clientIp, String userAgent) {
        long startTime = System.currentTimeMillis();
        String jobTitle = request.getJobTitle() != null ? request.getJobTitle() : "unspecified";
        log.info("Tailoring CV for job: {}", jobTitle);

        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
        CvTailorResponse response = cvEnhancementService.tailorCv(request);

        long processingTime = System.currentTimeMillis() - startTime;

        // Log the CV tailor operation
        cvOperationLogService.logTailor(
                user,
                request.getResumeData(),
                "/api/v1/cvs/tailor",
                processingTime,
                true,
                null,
                clientIp,
                userAgent,
                jobTitle
        );

        // Charge tokens for each AI operation used
        if (userId != null) {
            try {
                // Base tailor operation
                subscriptionService.chargeTokens(userId, TOKENS_CV_TAILOR, TokenUsageType.CV_TAILOR,
                        "CV Tailor for: " + jobTitle);
                
                // Optional: Cover letter generation
                if (request.isGenerateCoverLetter() && response.getCoverLetter() != null) {
                    subscriptionService.chargeTokens(userId, TOKENS_COVER_LETTER, TokenUsageType.COVER_LETTER,
                            "Cover Letter for: " + jobTitle);
                }
                
                // Optional: Interview prep
                if (request.isIncludeInterviewQuestions() && response.getInterviewPrep() != null) {
                    subscriptionService.chargeTokens(userId, TOKENS_INTERVIEW_PREP, TokenUsageType.INTERVIEW_PREP,
                            "Interview Prep for: " + jobTitle);
                }
                
                // Optional: Roadmap generation
                if (request.isIncludeRoadmap() && response.getRoadmap() != null) {
                    subscriptionService.chargeTokens(userId, TOKENS_ROADMAP, TokenUsageType.ROADMAP,
                            "Career Roadmap for: " + jobTitle);
                }
                
                // ATS Score is always calculated (2 times - before and after)
                subscriptionService.chargeTokens(userId, TOKENS_ATS_SCORE * 2, TokenUsageType.ATS_SCORE,
                        "ATS Score Analysis for: " + jobTitle);
                
                log.info("Logged token usage for CV tailor operation: userId={}, job={}", userId, jobTitle);
            } catch (Exception e) {
                log.error("Failed to log token usage for tailor operation: {}", e.getMessage());
                // Don't fail the request if token logging fails
            }
        }

        return response;
    }
}

