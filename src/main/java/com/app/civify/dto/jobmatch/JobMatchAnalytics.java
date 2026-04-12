package com.app.civify.dto.jobmatch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobMatchAnalytics {
    private int totalAnalyses;
    private double averageScore;
    private String strongestArea;
    private String weakestArea;
    private List<String> topMissingSkills;
    private List<ScoreTrend> recentTrends;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreTrend {
        private String targetJobTitle;
        private int atsScore;
        private String analyzedAt;
    }
}
