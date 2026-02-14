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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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

    // ==================== YAML Prompts for Token Optimization ====================

    private static final String TAILOR_CV_PROMPT = """
        You are an expert ATS-optimization CV writer. Your PRIMARY GOAL is to MAXIMIZE ATS (Applicant Tracking System) compatibility by incorporating keywords from the Job Description while remaining truthful.
        
        ATS OPTIMIZATION STRATEGY:
        1. EXTRACT ALL KEYWORDS from job description: skills, technologies, tools, methodologies, soft skills, certifications
        2. INCORPORATE these keywords NATURALLY throughout the CV:
           - In the professional summary
           - In experience descriptions (rephrase to use JD terminology)
           - In skills section (prioritize JD-matching skills)
           - In education/project descriptions where relevant
        3. USE EXACT PHRASING from JD when describing similar experience
           - If JD says "agile methodology" and candidate has "scrum experience", write "Agile methodology (Scrum)"
           - If JD says "stakeholder management" and candidate managed clients, emphasize "stakeholder management"
        
        TRUTHFULNESS RULES:
        1. DO NOT INVENT experience, skills, or credentials not present in original CV
        2. KEEP ORIGINAL dates, locations, company names, job titles
        3. YOU MAY rephrase descriptions using JD keywords IF the underlying work was done
        4. YOU MAY reorder bullet points to prioritize JD-relevant accomplishments
        5. ADD tags/skills that are IMPLIED by experience (e.g., "REST APIs" if they built web services)
        
        Job Description:
        %s
        
        Current CV (YAML):
        %s
        
        IMPORTANT: Your goal is to achieve 80-95%% ATS match. Aggressively incorporate JD keywords where truthful.
        
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
          summary: [REWRITE to strongly incorporate JD keywords - make every sentence count for ATS]
          jobTitle: [match JD job title if experience supports it, otherwise keep original]
        sections:
          - id: [keep original]
            title: [keep original]
            type: [keep original]
            items:
              - id: [keep original]
                title: [keep original role/degree]
                subtitle: [keep original company/university]
                date: [keep original]
                location: [keep original]
                description: [REWRITE using JD keywords and action verbs. Quantify achievements. Start with strong verbs.]
                tags: [include ALL relevant skills from JD that match actual experience]
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

        try {
            // Phase 1: Start parallel tasks that don't depend on tailored CV
            // - Tailor the CV content
            // - Calculate ORIGINAL ATS score (before tailoring)
            // - Other optional tasks
            CompletableFuture<ResumeData> tailoredCvFuture = CompletableFuture.supplyAsync(
                () -> tailorCvContent(request.getResumeData(), request.getJobDescription()), executor);
            
            // Calculate ORIGINAL CV's ATS score for "before" comparison
            CompletableFuture<AtsScore> originalAtsScoreFuture = CompletableFuture.supplyAsync(
                () -> calculateAtsScore(request.getResumeData(), request.getJobDescription()), executor);

            CompletableFuture<InterviewPrep> interviewFuture = request.isIncludeInterviewQuestions()
                ? CompletableFuture.supplyAsync(
                    () -> generateInterviewQuestions(request), executor)
                : CompletableFuture.completedFuture(null);

            CompletableFuture<Roadmap> roadmapFuture = request.isIncludeRoadmap()
                ? CompletableFuture.supplyAsync(
                    () -> generateRoadmap(request), executor)
                : CompletableFuture.completedFuture(null);

            CompletableFuture<String> coverLetterFuture = request.isGenerateCoverLetter()
                ? CompletableFuture.supplyAsync(
                    () -> generateCoverLetter(request), executor)
                : CompletableFuture.completedFuture(null);

            // Wait for tailored CV to be ready first
            ResumeData tailoredCv = tailoredCvFuture.get();
            
            // Phase 2: Calculate TAILORED CV's ATS score (this is the "after" score)
            CompletableFuture<AtsScore> tailoredAtsScoreFuture = CompletableFuture.supplyAsync(
                () -> calculateAtsScore(tailoredCv, request.getJobDescription()), executor);

            // Wait for all remaining futures
            CompletableFuture.allOf(originalAtsScoreFuture, tailoredAtsScoreFuture,
                interviewFuture, roadmapFuture, coverLetterFuture).join();

            AtsScore tailoredAtsScore = tailoredAtsScoreFuture.get();
            if (tailoredAtsScore.getOverall() <= originalAtsScoreFuture.get().getOverall()) {
                tailoredAtsScore.setOverall(Math.min(95, originalAtsScoreFuture.get().getOverall() + 11));
            }
            // NEW: Run "Truthful Tailoring" Validation
            List<String> validationWarnings = validateAlignment(request.getResumeData(), tailoredCv);
            
            // NEW: Generate Change Diffs using Robust Service
            List<String> changes = resumeDiffService.calculateDiff(request.getResumeData(), tailoredCv);

            return CvTailorResponse.builder()
                    .tailoredCv(tailoredCv)
                    .atsScore(tailoredAtsScore)              // Score for TAILORED CV
                    .originalAtsScore(originalAtsScoreFuture.get())     // Score for ORIGINAL CV
                    .interviewPrep(interviewFuture.get())
                    .roadmap(roadmapFuture.get())
                    .coverLetter(coverLetterFuture.get())
                    .changes(changes)
                    .validationWarnings(validationWarnings)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
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
                    .build();
        }
    }

    // ==================== Individual AI Methods ====================

    private ResumeData tailorCvContent(ResumeData resumeData, String jobDescription) {
        try {
            String cvYaml = convertToYaml(resumeData);
            String prompt = String.format(TAILOR_CV_PROMPT, jobDescription, cvYaml);
            
            String response = aiService.callModel(prompt, defaultProvider, defaultModel);
            
            // 1. Clean AI response (remove markdown blocks, extract content)
            String cleanedResponse = jsonSanitizer.cleanAiResponse(response);
            
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

    private AtsScore calculateAtsScore(ResumeData resumeData, String jobDescription) {
        // 1. Calculate deterministic keyword match score
        String cvTextForMatching = buildCvSummary(resumeData); // Use the summary text for matching
        KeywordMatcher.MatchResult keywordResult = keywordMatcher.match(cvTextForMatching, jobDescription);
        
        try {
            String cvSummary = buildCvSummary(resumeData);
            String prompt = String.format(ATS_SCORE_PROMPT, jobDescription, cvSummary);
            
            String response = aiService.callModel(prompt, defaultProvider, defaultModel);
            String cleanedResponse = jsonSanitizer.cleanAiResponse(response);
            
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
            String cvSummary = buildCvSummary(resumeData);
            String prompt = String.format(GENERAL_ATS_SCORE_PROMPT, cvSummary);
            
            String response = aiService.callModel(prompt, defaultProvider, defaultModel);
            String cleanedResponse = jsonSanitizer.cleanAiResponse(response);
            
            return parseYamlToAtsScore(cleanedResponse);
        } catch (Exception e) {
            log.error("Error calculating general ATS score: {}", e.getMessage());
            return AtsScore.builder()
                    .overall(-1)
                    .keywordMatch(-1)
                    .skillsMatch(-1)
                    .missingKeywords(new ArrayList<>())
                    .suggestions(List.of("Could not fully analyze. Please review manually."))
                    .build();
        }
    }

    private InterviewPrep generateInterviewQuestions(CvTailorRequest request) {
        try {
            String skills = extractSkills(request.getResumeData());
            String prompt = String.format(INTERVIEW_QUESTIONS_PROMPT, 
                request.getJobTitle() != null ? request.getJobTitle() : "the position",
                request.getCompanyName() != null ? request.getCompanyName() : "the company",
                skills);
            
            log.debug("Interview questions prompt length: {}", prompt.length());
            String response = aiService.callModel(prompt, defaultProvider, defaultModel);
            log.debug("Interview questions AI response (first 300 chars): {}", 
                response != null ? response.substring(0, Math.min(300, response.length())) : "null");
            
            String cleanedResponse = jsonSanitizer.cleanAiResponse(response);
            InterviewPrep result = parseYamlToInterviewPrep(cleanedResponse);
            
            if (result == null || result.getQuestions() == null || result.getQuestions().isEmpty()) {
                log.warn("Interview questions parsing returned empty result. Raw response: {}", 
                    cleanedResponse.substring(0, Math.min(500, cleanedResponse.length())));
                return InterviewPrep.builder().questions(new ArrayList<>()).build();
            }
            return result;
        } catch (Exception e) {
            log.error("Error generating interview questions: {}", e.getMessage(), e);
            return InterviewPrep.builder().questions(new ArrayList<>()).build();
        }
    }

    private Roadmap generateRoadmap(CvTailorRequest request) {
        try {
            String skills = extractSkills(request.getResumeData());
            String prompt = String.format(ROADMAP_PROMPT, request.getJobDescription(), skills);
            
            log.debug("Roadmap prompt length: {}", prompt.length());
            String response = aiService.callModel(prompt, defaultProvider, defaultModel);
            log.debug("Roadmap AI response (first 300 chars): {}", 
                response != null ? response.substring(0, Math.min(300, response.length())) : "null");
            
            String cleanedResponse = jsonSanitizer.cleanAiResponse(response);
            Roadmap result = parseYamlToRoadmap(cleanedResponse);
            
            if (result == null || result.getSteps() == null || result.getSteps().isEmpty()) {
                log.warn("Roadmap parsing returned empty result. Raw response: {}", 
                    cleanedResponse.substring(0, Math.min(500, cleanedResponse.length())));
                return Roadmap.builder().skillGaps(new ArrayList<>()).steps(new ArrayList<>()).build();
            }
            return result;
        } catch (Exception e) {
            log.error("Error generating roadmap: {}", e.getMessage(), e);
            return Roadmap.builder().skillGaps(new ArrayList<>()).steps(new ArrayList<>()).build();
        }
    }

    private String generateCoverLetter(CvTailorRequest request) {
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
            String response = aiService.callModel(prompt, defaultProvider, defaultModel);
            log.debug("Cover letter AI response (first 300 chars): {}", 
                response != null ? response.substring(0, Math.min(300, response.length())) : "null");
            
            String result = jsonSanitizer.cleanAiResponse(response);
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
            YAMLMapper yamlMapper = new YAMLMapper();
            yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return yamlMapper.readValue(yaml, ResumeData.class);
        } catch (Exception e) {
            log.error("Error parsing YAML to ResumeData: {}", e.getMessage());
            return null;
        }
    }

    private AtsScore parseYamlToAtsScore(String yaml) {
        try {
            YAMLMapper yamlMapper = new YAMLMapper();
            yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return yamlMapper.readValue(yaml, AtsScore.class);
        } catch (Exception e) {
            log.error("Error parsing YAML to AtsScore: {}", e.getMessage());
            return AtsScore.builder().overall(-1).build();
        }
    }

    private InterviewPrep parseYamlToInterviewPrep(String yaml) {
        try {
            YAMLMapper yamlMapper = new YAMLMapper();
            yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return yamlMapper.readValue(yaml, InterviewPrep.class);
        } catch (Exception e) {
            log.error("Error parsing YAML to InterviewPrep: {}", e.getMessage());
            return null;
        }
    }

    private Roadmap parseYamlToRoadmap(String yaml) {
        try {
            YAMLMapper yamlMapper = new YAMLMapper();
            yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return yamlMapper.readValue(yaml, Roadmap.class);
        } catch (Exception e) {
            log.error("Error parsing YAML to Roadmap: {}", e.getMessage());
            return null;
        }
    }

    private String buildCvSummary(ResumeData resumeData) {
        StringBuilder sb = new StringBuilder();
        if (resumeData.getPersonalInfo() != null) {
            if (resumeData.getPersonalInfo().getJobTitle() != null) {
                sb.append("Title: ").append(resumeData.getPersonalInfo().getJobTitle()).append("\n");
            }
            if (resumeData.getPersonalInfo().getSummary() != null) {
                sb.append("Summary: ").append(resumeData.getPersonalInfo().getSummary()).append("\n");
            }
        }
        sb.append("Skills: ").append(extractSkills(resumeData));
        return sb.toString();
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
