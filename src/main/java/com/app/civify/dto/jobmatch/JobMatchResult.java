package com.app.civify.dto.jobmatch;

import com.app.civify.dto.cv.CvTailorResponse.AtsScore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobMatchResult {
    // Scores (0-100)
    private int overallScore;
    private int skillsScore;
    private int experienceScore;
    private int keywordScore;
    
    // Parsed JD
    private ParsedJobDescription parsedJd;
    
    // Structured match details
    private List<SkillMatch> matchedSkills;
    private List<SkillGap> missingSkills;
    private List<String> matchedExperience;
    
    // Actionable suggestions
    private List<Suggestion> suggestions;
    
    // ATS keyword analysis
    private AtsScore atsScore;
    
    // Metadata
    private long processingTimeMs;
    private int totalTokensUsed;
    
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SkillMatch {
        private String skill;
        private String cvEvidence;
        private int confidence;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SkillGap {
        private String skill;
        private boolean required;
        private String suggestion;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Suggestion {
        private String section;
        private String action;
        private String reason;
        private String priority;
    }
}
