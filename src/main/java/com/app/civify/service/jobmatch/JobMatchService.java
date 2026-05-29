package com.app.civify.service.jobmatch;

import com.app.civify.dto.cv.CvTailorResponse;
import com.app.civify.dto.jobmatch.JobMatchResult;
import com.app.civify.dto.jobmatch.ParsedJobDescription;
import com.app.civify.dto.resume.ResumeData;
import com.app.civify.service.cv.CvEnhancementService;
import com.app.civify.service.cv.KeywordMatcher;
import com.app.civify.dto.jobmatch.JobMatchRequest;
import com.app.civify.entity.enums.TokenUsageType;
import com.app.civify.exception.BadRequestException;
import com.app.civify.service.external.JdScraperService;
import com.app.civify.service.subscription.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobMatchService {

    private final JdParserService jdParserService;
    private final CvEnhancementService cvEnhancementService;
    private final KeywordMatcher keywordMatcher;
    private final JdScraperService jdScraperService;
    private final SubscriptionService subscriptionService;
    private final SuggestionService suggestionService;
    private final JobMatchHistoryService jobMatchHistoryService;

    /**
     * Performs a full job match analysis between a resume and a job description.
     */
    public JobMatchResult analyzeMatch(Long userId, JobMatchRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Starting job match analysis for user: {}", userId);

        ResumeData resumeData = request.getResumeData();
        if (resumeData == null) {
            throw new BadRequestException("Resume data is required");
        }

        String rawJobDescription;
        if (request.getJobUrl() != null && !request.getJobUrl().trim().isEmpty()) {
            rawJobDescription = jdScraperService.scrape(request.getJobUrl());
        } else if (request.getJobDescription() != null && !request.getJobDescription().trim().isEmpty()) {
            rawJobDescription = request.getJobDescription();
        } else {
            throw new BadRequestException("Either jobDescription or jobUrl must be provided.");
        }

        subscriptionService.incrementUsage(userId, "JOB_MATCH");

        // 1. Parse JD into structured format
        ParsedJobDescription parsedJd = jdParserService.parseJobDescription(rawJobDescription);
        
        // 2. We use the existing ATS calculation logic to get keyword matches
        // Extract full text of the resume for matching
        String cvText = extractFullText(resumeData).toLowerCase();
        
        // Find matched vs missing skills
        List<JobMatchResult.SkillMatch> matchedSkills = new ArrayList<>();
        List<JobMatchResult.SkillGap> missingSkills = new ArrayList<>();
        
        if (parsedJd.getRequiredSkills() != null) {
            for (String skill : parsedJd.getRequiredSkills()) {
                if (keywordMatcher.containsKeyword(cvText, skill)) {
                    matchedSkills.add(JobMatchResult.SkillMatch.builder()
                            .skill(skill)
                            .confidence(100)
                            .cvEvidence("Found in CV") // A smarter approach would extract the context sentence
                            .build());
                } else {
                    missingSkills.add(JobMatchResult.SkillGap.builder()
                            .skill(skill)
                            .required(true)
                            .suggestion("Add to Skills section or mention in Experience if applicable")
                            .build());
                }
            }
        }
        
        if (parsedJd.getPreferredSkills() != null) {
            for (String skill : parsedJd.getPreferredSkills()) {
                if (keywordMatcher.containsKeyword(cvText, skill)) {
                    matchedSkills.add(JobMatchResult.SkillMatch.builder()
                            .skill(skill)
                            .confidence(80)
                            .cvEvidence("Found in CV")
                            .build());
                } else {
                    missingSkills.add(JobMatchResult.SkillGap.builder()
                            .skill(skill)
                            .required(false)
                            .suggestion("Consider adding if you have this experience")
                            .build());
                }
            }
        }

        // 3. Call existing CV Enhancement Service to get the ATS Base Score & Missing Keywords list
        // Since we already parsed the JD, we can just pass the raw text to ATS calculator
        CvTailorResponse.AtsScore atsScore = cvEnhancementService.scoreResumeForJob(resumeData, rawJobDescription);
        
        // Calculate sub-scores (simplified logic for now)
        int totalRequired = parsedJd.getRequiredSkills() != null ? parsedJd.getRequiredSkills().size() : 0;
        int matchedRequired = (int) matchedSkills.stream().filter(m -> 
                parsedJd.getRequiredSkills() != null && parsedJd.getRequiredSkills().contains(m.getSkill())
        ).count();
        
        int skillsScore = totalRequired > 0 ? (int) Math.round(((double) matchedRequired / totalRequired) * 100) : 100;
        
        // Use AI to generate actionable suggestions based on identified gaps
        List<JobMatchResult.Suggestion> suggestions = suggestionService.generateSuggestions(resumeData, parsedJd, missingSkills, 3);

        // Calculate Overall Hybrid Score: 40% skills, 40% keywords, 20% experience
        int expScore = calculateExperienceScore(resumeData, parsedJd);
        int keywordScore = atsScore != null ? atsScore.getKeywordMatch() : 0;
        int overallScore = (int) Math.round((skillsScore * 0.4) + (keywordScore * 0.4) + (expScore * 0.2));
        
        JobMatchResult result = JobMatchResult.builder()
                .overallScore(overallScore)
                .skillsScore(skillsScore)
                .experienceScore(expScore)
                .keywordScore(keywordScore)
                .parsedJd(parsedJd)
                .matchedSkills(matchedSkills)
                .missingSkills(missingSkills)
                .matchedExperience(List.of()) // Not yet implemented
                .suggestions(suggestions)
                .atsScore(atsScore)
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .build();

        // Save history asynchronously via a separate service (Spring @Async requires cross-bean calls)
        if (userId != null) {
            jobMatchHistoryService.saveAsync(userId, request.getResumeId(), rawJobDescription, result);
        }

        // Charge AI tokens if any were used
        if (result.getTotalTokensUsed() > 0) {
            try {
                subscriptionService.chargeTokens(userId, result.getTotalTokensUsed(), TokenUsageType.CV_TAILOR, "Job Match Analysis");
            } catch (Exception e) {
                log.error("Failed to charge tokens for user {}", userId, e);
            }
        }

        return result;
    }

    private String extractFullText(ResumeData resumeData) {
        if (resumeData == null) return "";
        StringBuilder sb = new StringBuilder();
        
        if (resumeData.getPersonalInfo() != null) {
            if (resumeData.getPersonalInfo().getJobTitle() != null) sb.append(resumeData.getPersonalInfo().getJobTitle()).append(" ");
            if (resumeData.getPersonalInfo().getSummary() != null) sb.append(resumeData.getPersonalInfo().getSummary()).append(" ");
        }
        
        if (resumeData.getSections() != null) {
            for (ResumeData.ResumeSection section : resumeData.getSections()) {
                if (section.getItems() != null) {
                    for (ResumeData.SectionItem item : section.getItems()) {
                        if (item.getTitle() != null) sb.append(item.getTitle()).append(" ");
                        if (item.getSubtitle() != null) sb.append(item.getSubtitle()).append(" ");
                        if (item.getDescription() != null) sb.append(item.getDescription()).append(" ");
                        if (item.getTags() != null) sb.append(String.join(" ", item.getTags())).append(" ");
                    }
                }
            }
        }
        return sb.toString();
    }

    private int calculateExperienceScore(ResumeData resumeData, ParsedJobDescription parsedJd) {
        if (parsedJd.getMinExperienceYears() <= 0) {
            return 100; // JD doesn't specify — full marks
        }
        
        // Count experience items in the resume
        int experienceItemCount = 0;
        if (resumeData.getSections() != null) {
            for (ResumeData.ResumeSection section : resumeData.getSections()) {
                if ("experience".equalsIgnoreCase(section.getType()) && section.getItems() != null) {
                    experienceItemCount += section.getItems().size();
                }
            }
        }
        
        // Heuristic: each experience entry ≈ 2 years average tenure
        int estimatedYears = experienceItemCount * 2;
        
        if (estimatedYears >= parsedJd.getMinExperienceYears()) {
            return 100;
        }
        
        // Proportional score
        return (int) Math.round(((double) estimatedYears / parsedJd.getMinExperienceYears()) * 100);
    }
}
