package com.app.civify.service;

import com.app.civify.dto.resume.ResumeData;
import com.app.civify.dto.resume.ResumeDto;
import com.app.civify.entity.resume.Resume;
import com.app.civify.entity.User;
import com.app.civify.exception.BadRequestException;
import com.app.civify.repository.resume.ResumeRepository;
import com.app.civify.repository.UserRepository;
import com.app.civify.service.usage.UsageTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final UsageTrackingService usageTrackingService;

    @Transactional(readOnly = true)
    public List<ResumeDto> getAllResumesForUser(Long userId) {
        return resumeRepository.findByUserId(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResumeDto getResumeById(String id, Long userId) {
        Resume resume = resumeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Resume not found or access denied"));
        return mapToDto(resume);
    }

    @Transactional
    public ResumeDto createResume(Long userId, String title, ResumeData initialData) {
        // Check if user can store more resumes based on subscription plan
        if (!usageTrackingService.canStoreResume(userId)) {
            long currentCount = resumeRepository.countByUserId(userId);
            log.warn("User {} has reached resume storage limit. Current count: {}", userId, currentCount);
            throw new BadRequestException("resume.storage.limit.reached");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Resume resume = Resume.builder()
                .user(user)
                .title(title)
                .data(initialData != null ? initialData : new ResumeData())
                .build();

        Resume savedResume = resumeRepository.save(resume);
        log.info("Created resume {} for user {}", savedResume.getId(), userId);
        return mapToDto(savedResume);
    }

    @Transactional
    public ResumeDto updateResume(String id, Long userId, String title, ResumeData data) {
        Resume resume = resumeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Resume not found or access denied"));

        if (title != null) {
            resume.setTitle(title);
        }
        if (data != null) {
            resume.setData(data);
        }

        Resume updatedResume = resumeRepository.save(resume);
        return mapToDto(updatedResume);
    }

    @Transactional
    public void deleteResume(String id, Long userId) {
        Resume resume = resumeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Resume not found or access denied"));
        resumeRepository.delete(resume);
        log.info("Deleted resume {} for user {}", id, userId);
    }

    @Transactional(readOnly = true)
    public long getResumeCountForUser(Long userId) {
        return resumeRepository.countByUserId(userId);
    }

    private ResumeDto mapToDto(Resume resume) {
        return ResumeDto.builder()
                .id(resume.getId())
                .title(resume.getTitle())
                .data(resume.getData())
                .createdAt(resume.getCreatedAt())
                .updatedAt(resume.getUpdatedAt())
                .build();
    }
}

