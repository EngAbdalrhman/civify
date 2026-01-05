package com.app.civify.service.job;

import com.app.civify.dto.job.JobApplicationRequest;
import com.app.civify.dto.job.JobApplicationResponse;
import com.app.civify.entity.resume.JobApplication;
import com.app.civify.exception.ResourceNotFoundException;
import com.app.civify.repository.resume.JobApplicationRepository;
import com.app.civify.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;

    public List<JobApplicationResponse> getAllApplications(UserPrincipal userPrincipal) {
        return jobApplicationRepository.findByUserId(userPrincipal.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public JobApplicationResponse getApplication(Long id, UserPrincipal userPrincipal) {
        JobApplication application = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job application not found"));

        if (!application.getUser().getId().equals(userPrincipal.getId())) {
            throw new ResourceNotFoundException("job.application.notfound");
        }

        return mapToResponse(application);
    }

    @Transactional
    public JobApplicationResponse createApplication(JobApplicationRequest request, UserPrincipal userPrincipal) {

        JobApplication application = new JobApplication();
        application.setUser(userPrincipal.getUser());
        application.setCvId(request.getCvId());
        application.setJobTitle(request.getJobTitle());
        application.setCompany(request.getCompany());
        application.setJobDescription(request.getJobDescription());
        application.setTailoredCV(request.getTailoredCV());
        application.setCoverLetter(request.getCoverLetter());
        application.setApplicationStatus(request.getApplicationStatus() != null ? request.getApplicationStatus() : "draft");
        application.setNotes(request.getNotes());

        JobApplication saved = jobApplicationRepository.save(application);
        return mapToResponse(saved);
    }

    @Transactional
    public JobApplicationResponse updateApplication(Long id, JobApplicationRequest request, UserPrincipal userPrincipal) {
        JobApplication application = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job application not found"));

        if (!application.getUser().getId().equals(userPrincipal.getId())) {
            throw new ResourceNotFoundException("job.application.notfound");
        }

        if (request.getCvId() != null) application.setCvId(request.getCvId());
        if (request.getJobTitle() != null) application.setJobTitle(request.getJobTitle());
        if (request.getCompany() != null) application.setCompany(request.getCompany());
        if (request.getJobDescription() != null) application.setJobDescription(request.getJobDescription());
        if (request.getTailoredCV() != null) application.setTailoredCV(request.getTailoredCV());
        if (request.getCoverLetter() != null) application.setCoverLetter(request.getCoverLetter());
        if (request.getApplicationStatus() != null) application.setApplicationStatus(request.getApplicationStatus());
        if (request.getNotes() != null) application.setNotes(request.getNotes());

        JobApplication updated = jobApplicationRepository.save(application);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteApplication(Long id, UserPrincipal userPrincipal) {
        JobApplication application = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job application not found"));

        if (!application.getUser().getId().equals(userPrincipal.getId())) {
            throw new ResourceNotFoundException("job.application.notfound");
        }

        jobApplicationRepository.delete(application);
    }

    private JobApplicationResponse mapToResponse(JobApplication app) {
        return JobApplicationResponse.builder()
                .id(app.getId())
                .userId(app.getUser().getId())
                .cvId(app.getCvId())
                .jobTitle(app.getJobTitle())
                .company(app.getCompany())
                .jobDescription(app.getJobDescription())
                .tailoredCV(app.getTailoredCV())
                .coverLetter(app.getCoverLetter())
                .applicationStatus(app.getApplicationStatus())
                .notes(app.getNotes())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }
}
