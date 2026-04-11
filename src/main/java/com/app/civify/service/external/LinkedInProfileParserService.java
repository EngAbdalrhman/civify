package com.app.civify.service.external;

import com.app.civify.dto.linkedin.LinkedInParseResponse;
import com.app.civify.dto.resume.ResumeData;
import com.app.civify.service.ai.AiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkedInProfileParserService {

    private final AiService aiService;
    private final ObjectMapper objectMapper;

    @Value("${ai.default.provider:gemini}")
    private String defaultProvider;

    @Value("${ai.default.model:gemini-pro}")
    private String defaultModel;

    private static final String SYSTEM_PROMPT = """
        You are an expert ATS resume parser. Parse this raw LinkedIn profile text into structured resume data.
        Extract: personal info (name, headline, location, summary),
        experience (title, company, dates, description),
        education (degree, school, dates),
        skills (list), certifications, languages.
        
        Return ONLY valid JSON matching this schema exactly, do not add any markdown formatting or extra text.
        Make sure to generate unique IDs for each section and each item (e.g. exp-1, edu-1).
        {
          "personalInfo": { 
             "fullName": "str", 
             "jobTitle": "str", 
             "summary": "str", 
             "linkedin": "str",
             "email": "str",
             "phone": "str",
             "address": "str"
          },
          "sections": [
            { "id": "exp-1", "title": "Work Experience", "type": "experience",
              "items": [{ "id": "item-1", "title": "Role", "subtitle": "Company", "date": "Date Range", "description": "Details" }] },
            { "id": "edu-1", "title": "Education", "type": "education",
              "items": [{ "id": "item-2", "title": "Degree", "subtitle": "School", "date": "Date Range", "description": "Details" }] },
            { "id": "skills-1", "title": "Skills", "type": "skills",
              "items": [{ "id": "item-3", "title": "Skill Name" }] }
          ]
        }
        
        Profile text:
        """;

    public LinkedInParseResponse parseProfile(String profileText) {
        log.info("Parsing LinkedIn profile text of length: {}", profileText.length());
        
        try {
            String fullPrompt = SYSTEM_PROMPT + profileText;
            String jsonResult = aiService.callModel(fullPrompt, defaultProvider, defaultModel).getContent();
            
            // Extract JSON from potential markdown or conversational wrap
            int startIndex = jsonResult.indexOf('{');
            int endIndex = jsonResult.lastIndexOf('}');
            
            if (startIndex != -1 && endIndex != -1 && endIndex >= startIndex) {
                jsonResult = jsonResult.substring(startIndex, endIndex + 1);
            } else {
                throw new IllegalStateException("Could not find valid JSON object in response");
            }
            
            ResumeData resumeData = objectMapper.readValue(jsonResult, ResumeData.class);
            
            // Generate some helpful metadata
            String name = resumeData.getPersonalInfo() != null ? resumeData.getPersonalInfo().getFullName() : "";
            String headline = resumeData.getPersonalInfo() != null ? resumeData.getPersonalInfo().getJobTitle() : "";
            
            int expCount = getSectionItemCount(resumeData, "experience");
            int eduCount = getSectionItemCount(resumeData, "education");
            int skillCount = getSectionItemCount(resumeData, "skills");

            return LinkedInParseResponse.builder()
                    .resumeData(resumeData)
                    .detectedName(name)
                    .detectedHeadline(headline)
                    .experienceCount(expCount)
                    .educationCount(eduCount)
                    .skillsCount(skillCount)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse LinkedIn profile text", e);
            return LinkedInParseResponse.builder()
                    .success(false)
                    .errorMessage("Failed to parse profile data: " + e.getMessage())
                    .build();
        }
    }
    
    private int getSectionItemCount(ResumeData resumeData, String type) {
        if (resumeData.getSections() == null) return 0;
        
        return resumeData.getSections().stream()
                .filter(s -> type.equals(s.getType()))
                .mapToInt(s -> s.getItems() != null ? s.getItems().size() : 0)
                .sum();
    }
}
