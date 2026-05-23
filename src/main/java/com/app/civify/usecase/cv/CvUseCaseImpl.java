package com.app.civify.usecase.cv;

import com.app.civify.dto.cv.CvTailorRequest;
import com.app.civify.dto.cv.CvTailorResponse;
import com.app.civify.dto.cv.CvTailorResponse.AtsScore;
import com.app.civify.dto.document.CvParseRequest;
import com.app.civify.dto.document.CvParseResponse;
import com.app.civify.dto.resume.ResumeData;
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

    // Hardcoded estimates removed in favor of actual usage


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

        if (userId != null) {
            try {
                int totalUsed = response.getTotalTokensUsed();
                if (totalUsed <= 0) {
                    return response;
                }

                int byokUsed = response.getByokTokensUsed();
                int billableTotal = Math.max(0, totalUsed - byokUsed);
                if (billableTotal == 0) {
                    log.info("Skipping Civify token charge for tailor — all {} tokens were BYOK (userId={})",
                            totalUsed, userId);
                    return response;
                }

                double billableRatio = (double) billableTotal / totalUsed;

                int interviewTokens = response.getInterviewPrep() != null ? response.getInterviewPrep().getTokensUsed() : 0;
                int roadmapTokens = response.getRoadmap() != null ? response.getRoadmap().getTokensUsed() : 0;

                int atsTokens = 0;
                if (response.getAtsScore() != null) {
                    atsTokens += response.getAtsScore().getTokensUsed();
                }
                if (response.getOriginalAtsScore() != null) {
                    atsTokens += response.getOriginalAtsScore().getTokensUsed();
                }

                int interviewBillable = scaleBillable(interviewTokens, billableRatio);
                int roadmapBillable = scaleBillable(roadmapTokens, billableRatio);
                int atsBillable = scaleBillable(atsTokens, billableRatio);
                int coreTokens = totalUsed - interviewTokens - roadmapTokens - atsTokens;
                int coreBillable = Math.max(0, billableTotal - interviewBillable - roadmapBillable - atsBillable);

                if (interviewBillable > 0) {
                    subscriptionService.chargeTokens(userId, interviewBillable, TokenUsageType.INTERVIEW_PREP,
                            "Interview Prep for: " + jobTitle);
                }
                if (roadmapBillable > 0) {
                    subscriptionService.chargeTokens(userId, roadmapBillable, TokenUsageType.ROADMAP,
                            "Career Roadmap for: " + jobTitle);
                }
                if (atsBillable > 0) {
                    subscriptionService.chargeTokens(userId, atsBillable, TokenUsageType.ATS_SCORE,
                            "ATS Score Analysis for: " + jobTitle);
                }
                if (coreBillable > 0) {
                    String desc = "CV Tailor" + (request.isGenerateCoverLetter() ? " + Cover Letter" : "")
                            + " for: " + jobTitle;
                    subscriptionService.chargeTokens(userId, coreBillable, TokenUsageType.CV_TAILOR, desc);
                }

                log.info("Logged token usage for CV tailor: userId={}, total={}, billable={}, byok={}, job={}",
                        userId, totalUsed, billableTotal, byokUsed, jobTitle);
            } catch (Exception e) {
                log.error("Failed to log token usage for tailor operation: {}", e.getMessage());
                // Don't fail the request if token logging fails
            }
        }

        return response;
    }

    @Override
    public com.app.civify.dto.cv.CvTranslateResponse translateCv(com.app.civify.dto.cv.CvTranslateRequest request, Long userId,
                                      String clientIp, String userAgent) {
        long startTime = System.currentTimeMillis();
        String targetLanguage = request.getTargetLanguage() != null ? request.getTargetLanguage() : "en";
        log.info("Translating CV to language: {}", targetLanguage);

        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
        com.app.civify.dto.cv.CvTranslateResponse response = cvEnhancementService.translateCv(request.getResumeData(), targetLanguage);

        long processingTime = System.currentTimeMillis() - startTime;

        // Log the CV operation
        cvOperationLogService.logTailor(
                user,
                request.getResumeData(),
                "/api/v1/cvs/translate",
                processingTime,
                true,
                null,
                clientIp,
                userAgent,
                "Translate to " + targetLanguage
        );

        if (userId != null && response.getTokensUsed() > 0) {
            try {
                subscriptionService.chargeTokens(userId, response.getTokensUsed(), TokenUsageType.CV_TRANSLATE,
                        "CV Translation to: " + targetLanguage);
                log.info("Logged {} tokens for CV translate: userId={}", response.getTokensUsed(), userId);
            } catch (Exception e) {
                log.error("Failed to log token usage for translate operation: {}", e.getMessage());
            }
        }

        return response;
    }

    @Override
    public AtsScore calculateScore(ResumeData resumeData, Long userId) {
        log.info("Calculating general ATS score for CV. User: {}", userId);
        
        // Charge tokens AFTER calculation to use actuals
        // (Moved closer to usage site)

        
        // Get AI-based content score
        AtsScore contentScore = cvEnhancementService.calculateGeneralAtsScore(resumeData);
        
        if (userId != null && contentScore.getTokensUsed() > 0) {
             try {
                subscriptionService.chargeTokens(userId, contentScore.getTokensUsed(), TokenUsageType.ATS_SCORE,
                        "General ATS Score Analysis");
                log.info("Charged {} actual tokens for ATS score", contentScore.getTokensUsed());
            } catch (Exception e) {
                log.error("Failed to charge tokens for ATS score: {}", e.getMessage());
            }
        }
        
        // Apply template/design heuristics
        int templatePenalty = 0;
        var suggestions = new java.util.ArrayList<>(contentScore.getSuggestions() != null 
                ? contentScore.getSuggestions() : java.util.List.of());
        
        if (resumeData.getMetadata() != null) {
            String templateId = resumeData.getMetadata().getTemplateId();
            String skillsDisplayStyle = resumeData.getMetadata().getSkillsDisplayStyle();
            String languagesDisplayStyle = resumeData.getMetadata().getLanguagesDisplayStyle();
            
            // Check template compatibility
            if ("creative".equalsIgnoreCase(templateId)) {
                templatePenalty += 10;
                suggestions.add("Creative templates may have parsing issues in some ATS systems");
            }
            
            // Check visual display styles that could cause parsing issues
            if ("dots".equals(skillsDisplayStyle) || "bar".equals(skillsDisplayStyle)) {
                templatePenalty += 5;
                suggestions.add("Visual skill bars/dots may not be parsed correctly by ATS");
            }
            if ("dots".equals(languagesDisplayStyle) || "bar".equals(languagesDisplayStyle)) {
                templatePenalty += 3;
                suggestions.add("Visual language proficiency indicators may not be parsed correctly");
            }
        }
        
        int adjustedScore = Math.max(0, contentScore.getOverall() - templatePenalty);
        
        return AtsScore.builder()
                .overall(adjustedScore)
                .keywordMatch(contentScore.getKeywordMatch())
                .skillsMatch(contentScore.getSkillsMatch())
                .missingKeywords(contentScore.getMissingKeywords())
                .suggestions(suggestions)
                .build();
    }

    private static int scaleBillable(int tokens, double billableRatio) {
        if (tokens <= 0 || billableRatio <= 0) {
            return 0;
        }
        return (int) Math.round(tokens * billableRatio);
    }
}
