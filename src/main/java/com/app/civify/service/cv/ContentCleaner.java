package com.app.civify.service.cv;

import org.springframework.stereotype.Component;

import com.app.civify.dto.resume.ResumeData;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service to clean common AI clichés and overused phrases from text to make it sound more human.
 * Inspired by Resume-Matcher's "AI Polish" feature.
 */
@Component
public class ContentCleaner {

    // Common AI/LLM "giveaway" phrases that lower perceived quality
    private static final List<String> AI_PHRASES = List.of(
            "spearheaded",
            "orchestrated",
            "meticulously",
            "navigated",
            "fostered a culture of",
            "leveraging",
            "testament to",
            "unwavering commitment",
            "realm of",
            "tapestry of",
            "deep dive",
            "game-changer",
            "synergy",
            "paradigm shift",
            "thought leader",
            "cutting-edge",
            "seamlessly integrated",
            "pivotal role",
            "championed"
    );

    /**
     * Clean text by removing or replacing AI phrases.
     * Currently replaces them with simpler alternatives or removes them.
     */
    public String clean(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String cleaned = text;
        for (String phrase : AI_PHRASES) {
            // Simple replacement strategy: replace with empty string or simpler term depending on context.
            // For this implementation, we will try to remove the "fluff" adjectives/adverbs.
            
            // Case-insensitive replacement
            cleaned = cleaned.replaceAll("(?i)\\b" + Pattern.quote(phrase) + "\\b", "[REPLACED]");
        }
        
        // Post-processing to fix grammar if needed (basic for now)
        // Replacing placeholders with more neutral terms
        cleaned = cleaned.replace("[REPLACED]", "led")     .replace("spearheaded", "led")
                         .replace("[REPLACED]", "managed") .replace("orchestrated", "managed")
                         .replace("meticulously", "")      // Adverb removal
                         .replace("seamlessly", "");       // Adverb removal

        return cleaned;
    }

    /**
     * Check if text contains high density of AI phrases.
     */
    public boolean needsCleaning(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        return AI_PHRASES.stream().anyMatch(lower::contains);
    }
    
    /**
     * Clean "AI Clichés" from the entire ResumeData structure.
     * Focuses on: Summary, Job Titles, and Descriptions.
     */
    public ResumeData clean(ResumeData data) {
        if (data == null) return null;
        
        // 1. Clean Personal Info
        if (data.getPersonalInfo() != null) {
            String origSummary = data.getPersonalInfo().getSummary();
            if (origSummary != null) {
                data.getPersonalInfo().setSummary(clean(origSummary));
            }
        }
        
        // 2. Clean Sections (Experience, Projects, etc.)
        if (data.getSections() != null) {
             for (ResumeData.ResumeSection section : data.getSections()) {
                 if (section.getItems() != null) {
                     for (ResumeData.SectionItem item : section.getItems()) {
                         // Clean Description
                         if (item.getDescription() != null) {
                             item.setDescription(clean(item.getDescription()));
                         }
                         // Clean Title (less likely to have fluff, but possible)
                         if (item.getTitle() != null) {
                             // be conservative with titles
                             // item.setTitle(clean(item.getTitle())); 
                         }
                     }
                 }
             }
        }
        
        return data;
    }
}
