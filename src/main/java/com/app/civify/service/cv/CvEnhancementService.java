package com.app.civify.service.cv;

import com.app.civify.dto.cv.CvTailorRequest;
import com.app.civify.dto.cv.CvTailorResponse;
import com.app.civify.dto.cv.CvTailorResponse.*;
import com.app.civify.dto.resume.ResumeData;
import com.app.civify.service.ai.AiService;
import com.app.civify.service.ai.JsonSanitizer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import com.app.civify.dto.ai.AiModelResponse;

/**
 * Service for CV enhancement operations including tailoring, ATS scoring,
 * interview prep, roadmap generation, and cover letter creation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CvEnhancementService {

    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private final KeywordMatcher keywordMatcher;
    private final ContentCleaner contentCleaner;
    private final ResumeDiffService resumeDiffService;
    private final JsonSanitizer jsonSanitizer;

    @Value("${ai.default.provider:gemini}")
    private String defaultProvider;

    @Value("${ai.default.model:gemini-pro}")
    private String defaultModel;

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    private static final YAMLMapper YAML_MAPPER = new YAMLMapper();
    static {
        YAML_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

    // ==================== YAML Prompts for Token Optimization ====================

    private static final String TAILOR_CV_PROMPT = """
        You are an expert CV writer. Your goal is to optimize this CV for the provided Job Description (JD) while maintaining strict truthfulness.
        
        STRATEGY:
        1. ANALYZE relevance: Does the candidate's experience actually align with the JD?
        2. IF RELEVANT: Incorporate JD specific keywords (skills, methodologies) into existing experience descriptions.
        3. IF NOT RELEVANT (e.g., mismatched industry): DO NOT force keywords. Focus on professional formatting and clarity. Improve valid verbs and metrics.
        
        TRUTHFULNESS RULES (CRITICAL):
        1. DO NOT INVENT experience, skills, or credentials.
        2. DO NOT use hyperbole like "superhero", "rockstar", "ninja", "unparalleled".
        3. KEEP ORIGINAL dates, locations, company names, job titles.
        4. YOU MAY rephrase descriptions to align with JD terminology ONLY IF the underlying work supports it.
        5. DO NOT add "implied" skills unless you are 100% certain the candidate has them based on the text.
        
        Job Description:
        %s
        
        Current CV (YAML):
        %s
        
        Output YAML only (strict valid YAML):
        personalInfo:
          fullName: [keep original]
          email: [keep original]
          phone: [keep original]
          city: [keep original]
          country: [keep original]
          linkedin: [keep original]
          github: [keep original]
          website: [keep original]
          summary: [Rewrite to be professional and concise. Incorporate JD keywords ONLY if relevant.]
          jobTitle: [match JD job title if experience supports it, otherwise keep original]
        sections:
          - id: [keep original]
            title: [keep original]
            type: [keep original]
            items:
              - id: [keep original]
                title: [keep original]
                subtitle: [keep original]
                date: [keep original]
                location: [keep original]
                description: [Rewrite for impact. Use action verbs. Quantify where possible. Do NOT force-fit irrelevant keywords.]
                tags: [Include valid skills from JD that match experience. Do NOT add skills the candidate clearly lacks.]
        """;

    private static final String ATS_SCORE_PROMPT = """
        Analyze this CV against the job description. Score ATS compatibility.
        
        Job Description:
        %s
        
        CV Summary:
        %s
        
        Output YAML only:
        overall: 0-100
        keywordMatch: 0-100
        skillsMatch: 0-100
        missingKeywords:
          - keyword1
          - keyword2
        suggestions:
          - suggestion1
          - suggestion2
        """;

    private static final String GENERAL_ATS_SCORE_PROMPT = """
        You are an expert ATS (Applicant Tracking System) analyzer. Analyze this CV for general ATS compatibility based on industry best practices.
        
        CV Content:
        %s
        
        Evaluation Criteria (score each 0-100):
        1. CONTENT QUALITY:
           - Strong action verbs (Led, Developed, Managed, etc.)
           - Quantifiable achievements (numbers, percentages, metrics)
           - Professional tone and clarity
        2. STRUCTURE:
           - Standard section headings (Experience, Education, Skills)
           - Contact information completeness (name, email, phone)
           - Logical flow and organization
        3. SKILLS PRESENTATION:
           - Relevant technical skills listed
           - Soft skills mentioned appropriately
           - Skills match typical industry requirements
        
        Output YAML only:
        overall: 0-100
        keywordMatch: 0-100 (general industry keywords)
        skillsMatch: 0-100 (skills presentation quality)
        missingKeywords:
          - common missing elements (e.g., "quantifiable metrics", "action verbs")
        suggestions:
          - specific improvement suggestions
        """;

    private static final String INTERVIEW_QUESTIONS_PROMPT = """
        Generate interview questions for this job role based on the CV.
        
        Job: %s
        Company: %s
        CV Skills: %s
        
        Output YAML only (max 5 questions):
        questions:
          - question: text
            hint: brief answer guidance
            category: behavioral|technical|situational
        """;

    private static final String ROADMAP_PROMPT = """
        Create a career roadmap to bridge gaps between CV and job requirements.
        
        Job Description:
        %s
        
        Current Skills: %s
        
        Output YAML only:
        skillGaps:
          - skill1
          - skill2
        steps:
          - step: action description
            timeframe: 1-2 weeks|1 month|2-3 months
        """;

    private static final String COVER_LETTER_PROMPT = """
            You are an expert career coach. Write a compelling, high-converting cover letter for the following candidate.
            
            Target Role: %s at %s
            
            Candidate:
            Name: %s
            Email: %s
            Phone: %s
            
            Candidate Profile:
            Summary: %s
            Top Skills: %s
            Experience Highlights: %s
            
            Job Description Analysis (Pain Points):
            %s
            
            Instructions:
            1.  **Structure**: Use the "Hook-Story-Offer" framework.
                *   **Hook**: Start with a strong opening that captures attention and shows understanding of the company's needs (Pain Points).
                *   **Story**: Connect specific achievements from the candidate's profile to solving those pain points. prove value with metrics.
                *   **Offer**: Close with a confident call to action.
            2.  **Tone**: Professional, confident, yet authentic. Avoid generic fluff.
            3.  **Formatting**: Standard business letter format.
            4.  **No Placeholders**: Do not use [Insert Company Name] placeholders; use the provided data.
            
            Output ONLY the cover letter body texts (or with header if appropriate).
            """;

    // ==================== Main Tailor Method ====================

    public CvTailorResponse tailorCv(CvTailorRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Starting CV tailoring for job: {}", 
            request.getJobTitle() != null ? request.getJobTitle() : "unspecified");

        // Thread-safe token accumulator for parallel tasks
        AtomicInteger tokenTracker = new AtomicInteger(0);

        try {
            // Phase 1: Start parallel tasks that don't depend on tailored CV
            // - Tailor the CV content
            // - Calculate ORIGINAL ATS score (before tailoring)
            // - Other optional tasks
            CompletableFuture<ResumeData> tailoredCvFuture = CompletableFuture.supplyAsync(
                () -> tailorCvContent(request.getResumeData(), request.getJobDescription(), tokenTracker), executor);
            
            // Calculate ORIGINAL CV's ATS score for "before" comparison
            CompletableFuture<AtsScore> originalAtsScoreFuture = CompletableFuture.supplyAsync(
                () -> calculateAtsScore(request.getResumeData(), request.getJobDescription(), tokenTracker), executor);

            CompletableFuture<InterviewPrep> interviewFuture = request.isIncludeInterviewQuestions()
                ? CompletableFuture.supplyAsync(
                    () -> generateInterviewQuestions(request, tokenTracker), executor)
                : CompletableFuture.completedFuture(null);

            CompletableFuture<Roadmap> roadmapFuture = request.isIncludeRoadmap()
                ? CompletableFuture.supplyAsync(
                    () -> generateRoadmap(request, tokenTracker), executor)
                : CompletableFuture.completedFuture(null);

            CompletableFuture<String> coverLetterFuture = request.isGenerateCoverLetter()
                ? CompletableFuture.supplyAsync(
                    () -> generateCoverLetter(request, tokenTracker), executor)
                : CompletableFuture.completedFuture(null);

            // Wait for tailored CV to be ready first
            ResumeData tailoringResult = tailoredCvFuture.get();
            
            // Phase 1.5: Post-Tailoring Refinement (Keyword Gap Injection)
            // Ensure we don't lose keywords that were present in the original CV
            // This guarantees tailored_score >= original_score for keyword component
            final ResumeData refinedCv = refineAfterTailoring(request.getResumeData(), tailoringResult, request.getJobDescription());
            
            // Phase 2: Calculate TAILORED CV's ATS score (this is the "after" score)
            CompletableFuture<AtsScore> tailoredAtsScoreFuture = CompletableFuture.supplyAsync(
                () -> calculateAtsScore(refinedCv, request.getJobDescription(), tokenTracker), executor);

            // Wait for all remaining futures
            CompletableFuture.allOf(originalAtsScoreFuture, tailoredAtsScoreFuture,
                interviewFuture, roadmapFuture, coverLetterFuture).join();

            AtsScore tailoredAtsScore = tailoredAtsScoreFuture.get();
        
            
            // Start: Truthfulness Validation
            List<String> validationWarnings = validateAlignment(request.getResumeData(), refinedCv);
            
            // NEW: Generate Change Diffs using Robust Service
            List<String> changes = resumeDiffService.calculateDiff(request.getResumeData(), refinedCv);

            int totalTokens = tokenTracker.get();
            log.info("CV Tailoring complete. Total tokens used: {}", totalTokens);

            return CvTailorResponse.builder()
                    .tailoredCv(refinedCv)
                    .atsScore(tailoredAtsScore)              // Score for TAILORED CV
                    .originalAtsScore(originalAtsScoreFuture.get())     // Score for ORIGINAL CV
                    .interviewPrep(interviewFuture.get())
                    .roadmap(roadmapFuture.get())
                    .coverLetter(coverLetterFuture.get())
                    .changes(changes)
                    .validationWarnings(validationWarnings)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .totalTokensUsed(totalTokens)
                    .build();

        } catch (Exception e) {
            log.error("Error tailoring CV: {}", e.getMessage(), e);
            // Return partial response with ATS score if available
            return CvTailorResponse.builder()
                    .tailoredCv(request.getResumeData()) // Return original if tailoring failed
                    .atsScore(AtsScore.builder()
                            .overall(-1)
                            .suggestions(List.of("Error processing: " + e.getMessage()))
                            .build())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .totalTokensUsed(tokenTracker.get()) // Return what we counted so far
                    .build();
        }
    }

    // ==================== Individual AI Methods ====================

    private ResumeData tailorCvContent(ResumeData resumeData, String jobDescription, AtomicInteger tokenTracker) {
        try {
            // 0. Check Relevance First
            String cvTextForMatching = extractFullText(resumeData);
            KeywordMatcher.MatchResult matchResult = keywordMatcher.match(cvTextForMatching, jobDescription);
            boolean isLowRelevance = matchResult.score() < 10;

            String cvYaml = convertToYaml(resumeData);
            String promptBody = String.format(TAILOR_CV_PROMPT, jobDescription, cvYaml);
            
            // Append strict warning if relevance is low
            if (isLowRelevance) {
                promptBody += "\n\nCRITICAL WARNING: The Job Description appears unrelated to this CV (Low Keyword Match). " +
                              "DO NOT force-fit keywords. DO NOT invent skills. Focus ONLY on professional formatting and clarity. " +
                              "If you cannot truthfully incorporate keywords, KEEP ORIGINAL CONTENT.";
                log.warn("Low keyword match ({}%) detected. Enabling strict no-hallucination mode.", matchResult.score());
            }

            AiModelResponse response = aiService.callModel(promptBody, defaultProvider, defaultModel);
            if (tokenTracker != null) {
                tokenTracker.addAndGet(response.getTotalTokens());
            }
            
            // 1. Clean AI response (remove markdown blocks, extract content)
            String cleanedResponse = jsonSanitizer.cleanAiResponse(response.getContent());
            
            // 2. Parse Valid YAML to Object
            ResumeData rawData = parseYamlToResumeData(cleanedResponse);
            
            // 3. Guard: if parsing failed, fall back to original
            if (rawData == null || jsonSanitizer.isTruncated(rawData)) {
                log.warn("Tailored CV parsing failed or resulted in truncated data, falling back to original CV");
                return resumeData;
            }
            
            // 4. Clean "AI Clichés" (Polish) from values
            return contentCleaner.clean(rawData);
        } catch (Exception e) {
            log.error("Error tailoring CV content: {}", e.getMessage());
            return resumeData; // Return original on failure
        }
    }

    private AtsScore calculateAtsScore(ResumeData resumeData, String jobDescription, AtomicInteger tokenTracker) {
        // 1. Calculate deterministic keyword match score
        String cvText = extractFullText(resumeData);
        KeywordMatcher.MatchResult keywordResult = keywordMatcher.match(cvText, jobDescription);
        
        try {
            String prompt = String.format(ATS_SCORE_PROMPT, jobDescription, cvText);
            
            AiModelResponse response = aiService.callModel(prompt, defaultProvider, defaultModel);
            if (tokenTracker != null) {
                tokenTracker.addAndGet(response.getTotalTokens());
            }

            String cleanedResponse = jsonSanitizer.cleanAiResponse(response.getContent());
            
            AtsScore aiScore = parseYamlToAtsScore(cleanedResponse);
            
            // 2. Hybrid Score Calculation
            // Weigh AI score 60% and Keyword score 40%
            int hybridOverall = (int) Math.round((aiScore.getOverall() * 0.6) + (keywordResult.score() * 0.4));
            
            // Merge missing keywords (AI + Deterministic)
            List<String> mergedMissingKeywords = new ArrayList<>();
            if (aiScore.getMissingKeywords() != null) {
                mergedMissingKeywords.addAll(aiScore.getMissingKeywords());
            }
            // Add top 10 missing keywords from deterministic matcher if not already present
            for (String kw : keywordResult.missingKeywords()) {
                 if (mergedMissingKeywords.size() >= 15) break; 
                 if (!mergedMissingKeywords.contains(kw)) {
                     mergedMissingKeywords.add(kw);
                 }
            }
            
            return AtsScore.builder()
                    .overall(hybridOverall)
                    .keywordMatch(keywordResult.score()) // Use the real keyword score here for accuracy
                    .skillsMatch(aiScore.getSkillsMatch())
                    .missingKeywords(mergedMissingKeywords)
                    .suggestions(aiScore.getSuggestions())
                    .tokensUsed(response.getTotalTokens()) // Capture tokens for this specific score op
                    .build();

        } catch (Exception e) {
            log.error("Error calculating ATS score (AI failed), falling back to keyword score: {}", e.getMessage());
            // Fallback to purely deterministic score if AI fails
            return AtsScore.builder()
                    .overall(keywordResult.score())
                    .keywordMatch(keywordResult.score())
                    .skillsMatch(0)
                    .missingKeywords(keywordResult.missingKeywords())
                    .suggestions(List.of("AI analysis failed. Score based on keyword matching only."))
                    .tokensUsed(0)
                    .build();
        }
    }

    /**
     * Calculate general ATS score for a CV without a specific job description.
     * Evaluates based on industry best practices: action verbs, metrics, structure, etc.
     *
     * @param resumeData the resume data to analyze
     * @return AtsScore with overall score and suggestions
     */
    public AtsScore calculateGeneralAtsScore(ResumeData resumeData) {
        try {
            String cvSummary = extractFullText(resumeData);
            String prompt = String.format(GENERAL_ATS_SCORE_PROMPT, cvSummary);
            
            AiModelResponse response = aiService.callModel(prompt, defaultProvider, defaultModel);
            String cleanedResponse = jsonSanitizer.cleanAiResponse(response.getContent());
            
            AtsScore score = parseYamlToAtsScore(cleanedResponse);
            score.setTokensUsed(response.getTotalTokens());
            return score;
        } catch (Exception e) {
            log.error("Error calculating general ATS score: {}", e.getMessage());
            return AtsScore.builder()
                    .overall(-1)
                    .keywordMatch(-1)
                    .skillsMatch(-1)
                    .missingKeywords(new ArrayList<>())
                    .suggestions(List.of("Could not fully analyze. Please review manually."))
                    .tokensUsed(0)
                    .build();
        }
    }

    /**
     * Refines the tailored CV by injecting keywords that were present in the original CV
     * but lost during the AI tailoring process.
     * This ensures fairness and prevents score regression.
     */
    private ResumeData refineAfterTailoring(ResumeData original, ResumeData tailored, String jd) {
        String originalText = extractFullText(original).toLowerCase();
        String tailoredText = extractFullText(tailored).toLowerCase();
        
        Set<String> jdKeywords = keywordMatcher.extractKeywords(jd);
        
        // Find keywords that are in JD but MISSING from Tailored CV
        List<String> missingFromTailored = jdKeywords.stream()
            .filter(kw -> !keywordMatcher.containsKeyword(tailoredText, kw))
            .collect(Collectors.toList());
            
        if (missingFromTailored.isEmpty()) return tailored;
        
        // Of those missing, which ones EXIST in the Original CV? (Truthful injection)
        List<String> injectableKeywords = missingFromTailored.stream()
            .filter(kw -> keywordMatcher.containsKeyword(originalText, kw))
            .collect(Collectors.toList());
            
        if (!injectableKeywords.isEmpty()) {
            log.info("Refining tailored CV: Injecting {} lost keywords: {}", injectableKeywords.size(), injectableKeywords);
            return injectMissingKeywords(tailored, injectableKeywords);
        }
        
        return tailored;
    }

    private ResumeData injectMissingKeywords(ResumeData tailored, List<String> keywordsToInject) {
        if (keywordsToInject.isEmpty()) return tailored;
        
        // 1. Find "Skills" section
        ResumeData.ResumeSection skillsSection = tailored.getSections().stream()
            .filter(s -> (s.getType() != null && "skills".equalsIgnoreCase(s.getType())) || 
                         (s.getTitle() != null && "skills".equalsIgnoreCase(s.getTitle())))
            .findFirst()
            .orElse(null);
            
        // 2. If not found, create it
        if (skillsSection == null) {
            skillsSection = new ResumeData.ResumeSection();
            skillsSection.setType("skills");
            skillsSection.setTitle("Skills");
            skillsSection.setId("skills_" + System.currentTimeMillis());
            skillsSection.setItems(new ArrayList<>());
            // Add to sections
            tailored.getSections().add(skillsSection);
        }
        
        // 3. Find or create item to hold tags
        ResumeData.SectionItem skillsItem;
        if (skillsSection.getItems() == null) {
             skillsSection.setItems(new ArrayList<>());
        }
        
        if (skillsSection.getItems().isEmpty()) {
            skillsItem = new ResumeData.SectionItem();
            skillsItem.setId("item_skills_" + System.currentTimeMillis());
            skillsItem.setTitle("Core Competencies");
            skillsItem.setTags(new ArrayList<>());
            skillsSection.getItems().add(skillsItem);
        } else {
            skillsItem = skillsSection.getItems().get(0);
            if (skillsItem.getTags() == null) {
                skillsItem.setTags(new ArrayList<>());
            }
        }
        
        // 4. Inject unique keywords
        for (String kw : keywordsToInject) {
            final String keyword = kw; // lambda constraint
            boolean exists = skillsItem.getTags().stream()
                .anyMatch(t -> t.equalsIgnoreCase(keyword));
            if (!exists) {
                skillsItem.getTags().add(kw);
            }
        }
        
        return tailored;
    }

    private InterviewPrep generateInterviewQuestions(CvTailorRequest request, AtomicInteger tokenTracker) {
        try {
            String skills = extractSkills(request.getResumeData());
            String prompt = String.format(INTERVIEW_QUESTIONS_PROMPT, 
                request.getJobTitle() != null ? request.getJobTitle() : "the position",
                request.getCompanyName() != null ? request.getCompanyName() : "the company",
                skills);
            
            log.debug("Interview questions prompt length: {}", prompt.length());
            AiModelResponse response = aiService.callModel(prompt, defaultProvider, defaultModel);
            if (tokenTracker != null) {
                tokenTracker.addAndGet(response.getTotalTokens());
            }
            
            log.debug("Interview questions AI response (first 300 chars): {}", 
                response != null && response.getContent() != null ? response.getContent().substring(0, Math.min(300, response.getContent().length())) : "null");
            
            String cleanedResponse = jsonSanitizer.cleanAiResponse(response.getContent());
            InterviewPrep result = parseYamlToInterviewPrep(cleanedResponse);
            
            if (result == null || result.getQuestions() == null || result.getQuestions().isEmpty()) {
                log.warn("Interview questions parsing returned empty result. Raw response: {}", 
                    cleanedResponse.substring(0, Math.min(500, cleanedResponse.length())));
                return InterviewPrep.builder().questions(new ArrayList<>()).tokensUsed(response.getTotalTokens()).build();
            }
            result.setTokensUsed(response.getTotalTokens());
            return result;
        } catch (Exception e) {
            log.error("Error generating interview questions: {}", e.getMessage(), e);
            return InterviewPrep.builder().questions(new ArrayList<>()).tokensUsed(0).build();
        }
    }

    private Roadmap generateRoadmap(CvTailorRequest request, AtomicInteger tokenTracker) {
        try {
            String skills = extractSkills(request.getResumeData());
            String prompt = String.format(ROADMAP_PROMPT, request.getJobDescription(), skills);
            
            log.debug("Roadmap prompt length: {}", prompt.length());
            AiModelResponse response = aiService.callModel(prompt, defaultProvider, defaultModel);
            if (tokenTracker != null) {
                tokenTracker.addAndGet(response.getTotalTokens());
            }

            log.debug("Roadmap AI response (first 300 chars): {}", 
                response != null && response.getContent() != null ? response.getContent().substring(0, Math.min(300, response.getContent().length())) : "null");
            
            String cleanedResponse = jsonSanitizer.cleanAiResponse(response.getContent());
            Roadmap result = parseYamlToRoadmap(cleanedResponse);
            
            if (result == null || result.getSteps() == null || result.getSteps().isEmpty()) {
                log.warn("Roadmap parsing returned empty result. Raw response: {}", 
                    cleanedResponse.substring(0, Math.min(500, cleanedResponse.length())));
                return Roadmap.builder().skillGaps(new ArrayList<>()).steps(new ArrayList<>()).tokensUsed(response.getTotalTokens()).build();
            }
            result.setTokensUsed(response.getTotalTokens());
            return result;
        } catch (Exception e) {
            log.error("Error generating roadmap: {}", e.getMessage(), e);
            return Roadmap.builder().skillGaps(new ArrayList<>()).steps(new ArrayList<>()).tokensUsed(0).build();
        }
    }

    private String generateCoverLetter(CvTailorRequest request, AtomicInteger tokenTracker) {
        try {
            ResumeData.PersonalInfo info = request.getResumeData().getPersonalInfo();
            String skills = extractSkills(request.getResumeData());
            String experience = buildExperienceSummary(request.getResumeData());
            
            String prompt = String.format(COVER_LETTER_PROMPT,
                request.getJobTitle() != null ? request.getJobTitle() : "the position",
                request.getCompanyName() != null ? request.getCompanyName() : "your company",
                info.getFullName() != null ? info.getFullName() : "Candidate",
                info.getEmail() != null ? info.getEmail() : "",
                info.getPhone() != null ? info.getPhone() : "",
                info.getSummary() != null ? info.getSummary() : "",
                skills,
                experience,
                // We pass the full JD here for the "Pain Points" analysis in line with the prompt
                request.getJobDescription() != null ? request.getJobDescription() : "Standard role requirements"
            );
            
            log.debug("Cover letter prompt length: {}", prompt.length());
            AiModelResponse response = aiService.callModel(prompt, defaultProvider, defaultModel);
            if (tokenTracker != null) {
                tokenTracker.addAndGet(response.getTotalTokens());
            }

            log.debug("Cover letter AI response (first 300 chars): {}", 
                response != null && response.getContent() != null ? response.getContent().substring(0, Math.min(300, response.getContent().length())) : "null");
            
            String result = jsonSanitizer.cleanAiResponse(response.getContent());
            if (result == null || result.isBlank()) {
                log.warn("Cover letter generation returned empty result");
                return "";
            }
            return result;
        } catch (Exception e) {
            log.error("Error generating cover letter: {}", e.getMessage(), e);
            return "";
        }
    }

    private String buildExperienceSummary(ResumeData resumeData) {
        if (resumeData.getSections() == null) return "";
        
        StringBuilder sb = new StringBuilder();
        resumeData.getSections().stream()
                .filter(s -> "experience".equalsIgnoreCase(s.getType()))
                .flatMap(s -> s.getItems().stream())
                .limit(3) // Top 3 experiences
                .forEach(item -> {
                    sb.append("- ").append(item.getSubtitle() != null ? item.getSubtitle() : "")
                      .append(" at ").append(item.getTitle() != null ? item.getTitle() : "")
                      .append(" (").append(item.getDate() != null ? item.getDate() : "").append(")\n");
                });
        return sb.toString();
    }

    // ==================== Helper Methods ====================

    private String convertToYaml(Object obj) {
        try {
            YAMLMapper yamlMapper = new YAMLMapper();
            return yamlMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Error converting to YAML: {}", e.getMessage());
            return "";
        }
    }

    // Local cleaning methods removed - using JsonSanitizer

    private ResumeData parseYamlToResumeData(String yaml) {
        try {
            return YAML_MAPPER.readValue(yaml, ResumeData.class);
        } catch (Exception e) {
            log.error("Error parsing YAML to ResumeData: {}", e.getMessage());
            return null;
        }
    }

    private AtsScore parseYamlToAtsScore(String yaml) {
        try {
            return YAML_MAPPER.readValue(yaml, AtsScore.class);
        } catch (Exception e) {
            log.error("Error parsing YAML to AtsScore: {}", e.getMessage());
            return AtsScore.builder().overall(-1).build();
        }
    }

    private InterviewPrep parseYamlToInterviewPrep(String yaml) {
        try {
            return YAML_MAPPER.readValue(yaml, InterviewPrep.class);
        } catch (Exception e) {
            log.error("Error parsing YAML to InterviewPrep: {}", e.getMessage());
            return null;
        }
    }

    private Roadmap parseYamlToRoadmap(String yaml) {
        try {
            return YAML_MAPPER.readValue(yaml, Roadmap.class);
        } catch (Exception e) {
            log.error("Error parsing YAML to Roadmap: {}", e.getMessage());
            return null;
        }
    }

    private String extractFullText(ResumeData resumeData) {
        StringBuilder sb = new StringBuilder();
        // 1. Personal Info
        if (resumeData.getPersonalInfo() != null) {
            appendIfNotNull(sb, resumeData.getPersonalInfo().getJobTitle());
            appendIfNotNull(sb, resumeData.getPersonalInfo().getSummary());
        }
        
        // 2. All Sections (Experience, Education, Projects, Skills)
        if (resumeData.getSections() != null) {
            for (ResumeData.ResumeSection section : resumeData.getSections()) {
                if (section.getItems() != null) {
                    for (ResumeData.SectionItem item : section.getItems()) {
                        appendIfNotNull(sb, item.getTitle());
                        appendIfNotNull(sb, item.getSubtitle());
                        appendIfNotNull(sb, item.getDescription());
                        
                        // Tags (critical for skills)
                        if (item.getTags() != null) {
                            for (String tag : item.getTags()) {
                                appendIfNotNull(sb, tag);
                            }
                        }
                    }
                }
            }
        }
        return sb.toString();
    }
    
    private void appendIfNotNull(StringBuilder sb, String text) {
        if (text != null && !text.isBlank()) {
            sb.append(text).append(" ");
        }
    }

    private String extractSkills(ResumeData resumeData) {
        if (resumeData.getSections() == null) return "";
        
        return resumeData.getSections().stream()
                .filter(s -> "skills".equalsIgnoreCase(s.getType()))
                .flatMap(s -> s.getItems().stream())
                .flatMap(item -> item.getTags() != null ? item.getTags().stream() : 
                    (item.getTitle() != null ? List.of(item.getTitle()).stream() : List.<String>of().stream()))
                .distinct()
                .limit(15)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    private List<String> validateAlignment(ResumeData original, ResumeData tailored) {
        List<String> warnings = new ArrayList<>();
        
        // 1. Validate Experience (Companies)
        Set<String> originalCompanies = extractTitlesFromSection(original, "experience");
        Set<String> tailoredCompanies = extractTitlesFromSection(tailored, "experience");
        
        for (String company : tailoredCompanies) {
            if (!originalCompanies.contains(company)) {
                warnings.add("Possible Hallucination: Company '" + company + "' found in tailored CV was not in original.");
            }
        }
        
        // 2. Validate Education (Schools/Degrees)
        Set<String> originalSchools = extractTitlesFromSection(original, "education");
        Set<String> tailoredSchools = extractTitlesFromSection(tailored, "education");
        
        for (String school : tailoredSchools) {
            if (!originalSchools.contains(school)) {
                warnings.add("Possible Hallucination: Education institution '" + school + "' found in tailored CV was not in original.");
            }
        }
        
        return warnings;
    }

    private Set<String> extractTitlesFromSection(ResumeData data, String sectionType) {
        if (data == null || data.getSections() == null) {
            return new HashSet<>();
        }
        return data.getSections().stream()
                .filter(s -> sectionType.equalsIgnoreCase(s.getType())) // Filter by type (e.g., "experience")
                .flatMap(s -> s.getItems().stream())
                .map(item -> item.getTitle() != null ? item.getTitle().trim().toLowerCase() : "")
                .filter(title -> !title.isEmpty())
                .collect(Collectors.toSet());
    }
}
