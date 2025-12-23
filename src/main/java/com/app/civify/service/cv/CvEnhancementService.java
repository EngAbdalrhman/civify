package com.app.civify.service.cv;

import com.app.civify.dto.cv.CvTailorRequest;
import com.app.civify.dto.cv.CvTailorResponse;
import com.app.civify.dto.cv.CvTailorResponse.*;
import com.app.civify.dto.resume.ResumeData;
import com.app.civify.service.ai.AiService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    @Value("${ai.default.provider:gemini}")
    private String defaultProvider;

    @Value("${ai.default.model:gemini-pro}")
    private String defaultModel;

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    // ==================== YAML Prompts for Token Optimization ====================

    private static final String TAILOR_CV_PROMPT = """
        You are an expert CV writer. Your task is to OPTIMIZE the provided CV for the given Job Description.
        
        CRITICAL RULES:
        1. DO NOT INVENT FACTS. Do not add jobs, companies, or degrees that are not in the input CV.
        2. KEEP ORIGINAL DATES & LOCATIONS. Do not change the timeline.
        3. YOU MAY rephrase bullet points to highlight skills relevant to the JD, but they must remain truthful to the original experience.
        4. YOU MAY reorder skills or emphasize different parts of the existing description.
        5. IF the candidate lacks a specific skill mentioned in the JD, DO NOT add it.
        6. MAINTAIN the exact YAML structure of the input.
        
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
          summary: [optimize summary for JD keywords, but stick to facts]
          jobTitle: [keep original or use a generic title that matches the actual experience]
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
                description: [rewrite to emphasize impact and JD keywords, using ONLY real experience]
                tags: [select most relevant real skills from original + derived skills]
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
        Write a professional cover letter for this job application.
        
        Position: %s
        Company: %s
        
        Candidate Information:
        Name: %s
        Email: %s
        Phone: %s
        Professional Summary: %s
        Key Skills: %s
        
        Work Experience:
        %s
        
        INSTRUCTIONS:
        1. Start with "Dear [Company] Hiring Team," or "Dear Hiring Manager,"
        2. First paragraph: Express enthusiasm for the role and company
        3. Second paragraph: Highlight relevant experience that aligns with the job requirements
        4. Third paragraph: Describe specific achievements and how they relate to the position
        5. Closing paragraph: Express interest in an interview, thank them
        6. End with:
           Sincerely,
           [Name]
           [Email]
           [Phone]
        
        Write the complete cover letter as plain text, professional tone, no markdown.
        """;

    // ==================== Main Tailor Method ====================

    public CvTailorResponse tailorCv(CvTailorRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Starting CV tailoring for job: {}", 
            request.getJobTitle() != null ? request.getJobTitle() : "unspecified");

        try {
            // Parallel execution for performance
            CompletableFuture<ResumeData> tailoredCvFuture = CompletableFuture.supplyAsync(
                () -> tailorCvContent(request.getResumeData(), request.getJobDescription()), executor);
            
            CompletableFuture<AtsScore> atsScoreFuture = CompletableFuture.supplyAsync(
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

            // Wait for all futures
            CompletableFuture.allOf(tailoredCvFuture, atsScoreFuture, 
                interviewFuture, roadmapFuture, coverLetterFuture).join();

            return CvTailorResponse.builder()
                    .tailoredCv(tailoredCvFuture.get())
                    .atsScore(atsScoreFuture.get())
                    .interviewPrep(interviewFuture.get())
                    .roadmap(roadmapFuture.get())
                    .coverLetter(coverLetterFuture.get())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();

        } catch (Exception e) {
            log.error("Error tailoring CV: {}", e.getMessage(), e);
            // Return partial response with ATS score if available
            return CvTailorResponse.builder()
                    .tailoredCv(request.getResumeData()) // Return original if tailoring failed
                    .atsScore(AtsScore.builder()
                            .overall(0)
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
            String cleanedResponse = cleanAiResponse(response);
            
            return parseYamlToResumeData(cleanedResponse);
        } catch (Exception e) {
            log.error("Error tailoring CV content: {}", e.getMessage());
            return resumeData; // Return original on failure
        }
    }

    private AtsScore calculateAtsScore(ResumeData resumeData, String jobDescription) {
        try {
            String cvSummary = buildCvSummary(resumeData);
            String prompt = String.format(ATS_SCORE_PROMPT, jobDescription, cvSummary);
            
            String response = aiService.callModel(prompt, defaultProvider, defaultModel);
            String cleanedResponse = cleanAiResponse(response);
            
            return parseYamlToAtsScore(cleanedResponse);
        } catch (Exception e) {
            log.error("Error calculating ATS score: {}", e.getMessage());
            return AtsScore.builder()
                    .overall(50)
                    .keywordMatch(50)
                    .skillsMatch(50)
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
            
            String cleanedResponse = cleanAiResponse(response);
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
            
            String cleanedResponse = cleanAiResponse(response);
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
                experience);
            
            log.debug("Cover letter prompt length: {}", prompt.length());
            String response = aiService.callModel(prompt, defaultProvider, defaultModel);
            log.debug("Cover letter AI response (first 300 chars): {}", 
                response != null ? response.substring(0, Math.min(300, response.length())) : "null");
            
            String result = cleanAiResponse(response);
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

    private String cleanAiResponse(String response) {
        if (response == null) return "";
        
        String cleaned = response.trim();
        
        // Extract from Gemini response structure if present
        cleaned = extractContentFromGeminiResponse(cleaned);
        
        // Remove markdown code blocks
        if (cleaned.startsWith("```yaml")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        
        return cleaned.trim();
    }

    private String extractContentFromGeminiResponse(String response) {
        try {
            var root = objectMapper.readTree(response);
            var candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                var parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    return parts.get(0).path("text").asText();
                }
            }
            return response;
        } catch (Exception e) {
            return response;
        }
    }

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
            return AtsScore.builder().overall(50).build();
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
}
