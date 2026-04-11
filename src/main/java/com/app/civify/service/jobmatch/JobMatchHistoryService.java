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

    private JobMatchHistoryDto toDto(JobMatchHistory entity) {
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
}
