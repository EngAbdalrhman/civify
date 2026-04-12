package com.app.civify.service.jobmatch;

import com.app.civify.dto.ai.AiModelResponse;
import com.app.civify.dto.jobmatch.ParsedJobDescription;
import com.app.civify.service.ai.AiService;
import com.app.civify.service.ai.JsonSanitizer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class JdParserService {

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

    private static final String PARSE_JD_PROMPT = """
            Analyze the following job description and extract its core components.
            
            Job Description:
            {JOB_DESCRIPTION}

            Output YAML only (strict valid YAML):
            jobTitle: [extracted title]
            companyName: [extracted company name if exists, else "null"]
            seniority: [junior, mid, senior, lead, or staff]
            remotePolicy: [remote, hybrid, or onsite]
            location: [extracted location if exists, else "null"]
            domain: [core engineering domain like "Backend", "AI/ML", "Frontend", "Data", etc.]
            minExperienceYears: [integer representing minimum years of experience. Use 0 if not specified]
            requiredSkills:
              - [skill 1]
              - [skill 2]
            preferredSkills:
              - [skill 1]
              - [skill 2]
            responsibilities:
              - [responsibility 1]
              - [responsibility 2]

            CRITICAL: DO NOT use markdown formatting, bold (**), or asterisks. Return valid YAML text only.
            """;

    public ParsedJobDescription parseJobDescription(String rawJdText) {
        log.info("Parsing job description of length: {}", rawJdText != null ? rawJdText.length() : 0);
        
        if (rawJdText == null || rawJdText.trim().isEmpty()) {
            throw new IllegalArgumentException("Job description cannot be empty");
        }

        try {
            String prompt = PARSE_JD_PROMPT.replace("{JOB_DESCRIPTION}", rawJdText);
            
            AiModelResponse response = aiService.callModel(prompt, defaultProvider, defaultModel);
            String cleanedResponse = jsonSanitizer.cleanAiResponse(response.getContent());
            
            return YAML_MAPPER.readValue(cleanedResponse, ParsedJobDescription.class);
            
        } catch (Exception e) {
            log.error("Failed to parse JD with AI: {}", e.getMessage());
            // Return a safe fallback rather than crashing
            return ParsedJobDescription.builder()
                    .jobTitle("Unknown Role")
                    .build();
        }
    }
}
