package com.app.civify.service.jobmatch;

import com.app.civify.dto.jobmatch.JobMatchHistoryDto;
import com.app.civify.dto.jobmatch.JobMatchResult;
import com.app.civify.entity.jobmatch.JobMatchHistory;
import com.app.civify.repository.UserRepository;
import com.app.civify.repository.resume.ResumeRepository;
import com.app.civify.repository.jobmatch.JobMatchHistoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobMatchHistoryService {

    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final JobMatchHistoryRepository jobMatchHistoryRepository;
    private final ObjectMapper objectMapper;

    /**
     * Saves a job match result to history asynchronously.
     * This is in a separate service so Spring's @Async proxy actually intercepts the call.
     */
    @Async
    public void saveAsync(Long userId, String resumeId, String rawJobDescription, JobMatchResult matchResult) {
        try {
            JobMatchHistory history = JobMatchHistory.builder()
                    .user(userRepository.getReferenceById(userId))
                    .resume(resumeId != null ? resumeRepository.getReferenceById(resumeId) : null)
                    .jobTitle(matchResult.getParsedJd().getJobTitle())
                    .companyName(matchResult.getParsedJd().getCompanyName())
                    .jobDescription(rawJobDescription)
                    .overallScore(matchResult.getOverallScore())
                    .skillsScore(matchResult.getSkillsScore())
                    .experienceScore(matchResult.getExperienceScore())
                    .keywordScore(matchResult.getKeywordScore())
                    .matchResultJson(objectMapper.writeValueAsString(matchResult))
                    .build();
            jobMatchHistoryRepository.save(history);
            log.info("Saved job match history for user {}", userId);
        } catch (Exception e) {
            log.error("Failed to save job match history: {}", e.getMessage(), e);
        }
    }

    public Page<JobMatchHistoryDto> getUserHistory(Long userId, Pageable pageable) {
        return jobMatchHistoryRepository.findByUserIdOrderByAnalyzedAtDesc(userId, pageable)
                .map(this::toDto);
    }

    public JobMatchHistoryDto toDto(JobMatchHistory entity) {
        return JobMatchHistoryDto.builder()
                .id(entity.getId())
                .jobTitle(entity.getJobTitle())
                .companyName(entity.getCompanyName())
                .overallScore(entity.getOverallScore())
                .skillsScore(entity.getSkillsScore())
                .experienceScore(entity.getExperienceScore())
                .keywordScore(entity.getKeywordScore())
                .analyzedAt(entity.getAnalyzedAt())
                .resumeId(entity.getResume() != null ? entity.getResume().getId() : null)
                .build();
    }

    public com.app.civify.dto.jobmatch.JobMatchAnalytics getAnalytics(Long userId) {
        java.util.List<JobMatchHistory> history = jobMatchHistoryRepository.findTop50ByUserIdOrderByAnalyzedAtDesc(userId);
        
        if (history.isEmpty()) {
            return com.app.civify.dto.jobmatch.JobMatchAnalytics.builder()
                    .totalAnalyses(0)
                    .averageScore(0.0)
                    .strongestArea("Unknown")
                    .weakestArea("Unknown")
                    .topMissingSkills(java.util.Collections.emptyList())
                    .recentTrends(java.util.Collections.emptyList())
                    .build();
        }

        int totalScore = 0;
        int totalSkills = 0;
        int totalExp = 0;
        int totalKeywords = 0;

        java.util.Map<String, Integer> missingSkillsFreq = new java.util.HashMap<>();
        java.util.List<com.app.civify.dto.jobmatch.JobMatchAnalytics.ScoreTrend> trends = new java.util.ArrayList<>();

        // Process top 10 for trends (to show on chart)
        int trendCount = Math.min(10, history.size());
        for (int i = trendCount - 1; i >= 0; i--) {
            JobMatchHistory h = history.get(i);
            trends.add(com.app.civify.dto.jobmatch.JobMatchAnalytics.ScoreTrend.builder()
                    .targetJobTitle(h.getJobTitle())
                    .atsScore(h.getOverallScore())
                    .analyzedAt(h.getAnalyzedAt().toString())
                    .build());
        }

        for (JobMatchHistory h : history) {
            totalScore += h.getOverallScore();
            totalSkills += h.getSkillsScore();
            totalExp += h.getExperienceScore();
            totalKeywords += h.getKeywordScore();

            // Extract missing skills from JSON if available
            try {
                if (h.getMatchResultJson() != null) {
                    JobMatchResult matchResult = objectMapper.readValue(h.getMatchResultJson(), JobMatchResult.class);
                    if (matchResult.getMissingSkills() != null) {
                        for (JobMatchResult.SkillGap gap : matchResult.getMissingSkills()) {
                            String lowerSkill = gap.getSkill().toLowerCase().trim();
                            missingSkillsFreq.put(lowerSkill, missingSkillsFreq.getOrDefault(lowerSkill, 0) + 1);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Could not parse matchResultJson for history id {}: {}", h.getId(), e.getMessage());
            }
        }

        double avgScore = (double) totalScore / history.size();
        double avgSkills = (double) totalSkills / history.size();
        double avgExp = (double) totalExp / history.size();
        double avgKeywords = (double) totalKeywords / history.size();

        // Determine strongest/weakest area
        String strongestArea = "skills";
        double maxArea = avgSkills;
        if (avgExp > maxArea) { strongestArea = "experience"; maxArea = avgExp; }
        if (avgKeywords > maxArea) { strongestArea = "keywords"; maxArea = avgKeywords; }

        String weakestArea = "skills";
        double minArea = avgSkills;
        if (avgExp < minArea) { weakestArea = "experience"; minArea = avgExp; }
        if (avgKeywords < minArea) { weakestArea = "keywords"; minArea = avgKeywords; }

        // Top 5 missing skills
        java.util.List<String> topMissingSkills = missingSkillsFreq.entrySet().stream()
                .sorted(java.util.Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(java.util.Map.Entry::getKey)
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1)) // capitalize
                .toList();

        return com.app.civify.dto.jobmatch.JobMatchAnalytics.builder()
                .totalAnalyses((int) jobMatchHistoryRepository.countByUserId(userId))
                .averageScore(Math.round(avgScore * 10.0) / 10.0) // 1 decimal place
                .strongestArea(strongestArea)
                .weakestArea(weakestArea)
                .topMissingSkills(topMissingSkills)
                .recentTrends(trends)
                .build();
    }
}
