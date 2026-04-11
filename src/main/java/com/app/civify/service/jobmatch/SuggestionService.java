package com.app.civify.service.jobmatch;

import com.app.civify.dto.ai.AiModelResponse;
import com.app.civify.dto.jobmatch.JobMatchResult;
import com.app.civify.dto.jobmatch.ParsedJobDescription;
import com.app.civify.dto.resume.ResumeData;
import com.app.civify.service.ai.AiService;
import com.app.civify.service.ai.JsonSanitizer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuggestionService {

    private final AiService aiService;
    private final JsonSanitizer jsonSanitizer;

    @Value("${ai.default.provider:gemini}")
    private String defaultProvider;

    @Value("${ai.default.model:gemini-pro}")
    private String defaultModel;

    private static final YAMLMapper YAML_MAPPER = new YAMLMapper();
    static {
        YAML_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private static final String GENERATE_SUGGESTIONS_PROMPT = """
            Analyze this CV against the job requirements. For the identified skill gaps, provide actionable suggestions.
            
            CV SUMMARY EXTRACT:
            {CV_TEXT}
            
            IDENTIFIED GAPS:
            {GAPS}

            Generate exactly {COUNT} highest-impact suggestions to improve the CV for this role.
            For each suggestion, provide:
            1. Which CV section to modify (e.g., "Skills", "Experience", "Summary")
            2. What specific change to make (be very brief and actionable)
            3. Why this change improves the match
            4. Priority ("HIGH", "MEDIUM", or "LOW")

            CRITICAL: Do NOT invent experience. Only suggest adding missing clear-cut skills if the user might possess them based on their summary, or suggest rewording existing generic experience to match the role keywords.
            
            Output YAML only as a list (strict valid YAML):
            - section: "Skills"
              action: "Add missing skill: Python"
              reason: "Required for backend roles."
              priority: "HIGH"
            - section: "Summary"
              action: "Mention distributed systems experience explicitly"
              reason: "Key requirement in the JD not clearly found in your summary."
              priority: "MEDIUM"

            CRITICAL: DO NOT use markdown formatting, bold (**), or asterisks. Return valid YAML array of objects only.
            """;

    public List<JobMatchResult.Suggestion> generateSuggestions(
            ResumeData resume,
            ParsedJobDescription parsedJd,
            List<JobMatchResult.SkillGap> gaps,
            int maxSuggestions
    ) {
        if (gaps == null || gaps.isEmpty()) {
            return new ArrayList<>();
        }

        // Limit the CV extract to just summary and job titles to save tokens, since this is just suggestion generation
        String cvExtract = extractSummaryContext(resume);
        String gapsList = gaps.stream()
                .map(g -> "- " + g.getSkill() + " (" + (g.isRequired() ? "Required" : "Preferred") + ")")
                .collect(Collectors.joining("\n"));

        String prompt = GENERATE_SUGGESTIONS_PROMPT
                .replace("{CV_TEXT}", cvExtract)
                .replace("{GAPS}", gapsList)
                .replace("{COUNT}", String.valueOf(maxSuggestions));

        try {
            AiModelResponse response = aiService.callModel(prompt, defaultProvider, defaultModel);
            String cleanedResponse = jsonSanitizer.cleanAiResponse(response.getContent());
            
            return YAML_MAPPER.readValue(cleanedResponse, new TypeReference<List<JobMatchResult.Suggestion>>() {});
        } catch (Exception e) {
            log.error("Failed to generate AI suggestions: {}", e.getMessage());
            return generateFallbackSuggestions(gaps, maxSuggestions);
        }
    }

    private String extractSummaryContext(ResumeData resume) {
        if (resume == null || resume.getPersonalInfo() == null) return "No summary provided.";
        String title = resume.getPersonalInfo().getJobTitle() != null ? resume.getPersonalInfo().getJobTitle() : "Unknown Role";
        String summary = resume.getPersonalInfo().getSummary() != null ? resume.getPersonalInfo().getSummary() : "";
        return "Current Title: " + title + "\nSummary: " + summary;
    }

    private List<JobMatchResult.Suggestion> generateFallbackSuggestions(List<JobMatchResult.SkillGap> gaps, int maxSuggestions) {
        return gaps.stream().limit(maxSuggestions).map(gap -> JobMatchResult.Suggestion.builder()
                .section("Skills")
                .action("Add missing skill if applicable: " + gap.getSkill())
                .reason("This is a " + (gap.isRequired() ? "required" : "preferred") + " skill for the role.")
                .priority(gap.isRequired() ? "HIGH" : "MEDIUM")
                .build()).collect(Collectors.toList());
    }
}
